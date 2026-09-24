import { useEffect, useRef, useState } from 'react'
import type { PointerEvent } from 'react'
import { CAPE_REGIONS } from '../capeConverter'
import { Dropdown } from '../Dropdown'
import { formatError } from '../formatError'
import { useIsWrapped } from '../useIsWrapped'
import { useLanguage, useTranslations } from '../i18n/LanguageContext'
import { ColorPicker } from '../skinEditor/ColorPicker'
import { SkinModelPreview } from '../skinEditor/SkinModelPreview'
import type { CapeLibraryEntry, SkinVariant } from '../../../shared/types'

type Tool = 'pencil' | 'eraser' | 'fill' | 'eyedropper'

const EDITOR_WIDTHS = [64, 128, 256]
/** Screen pixels per vanilla 64x32 unit - resolutions up to 256x128 get the same on-screen panel
 * sizes; above that the panels grow instead, so one texture pixel never gets smaller than one
 * screen pixel (every cape up to 2048x1024 is editable, e.g. converter results). */
const SCREEN_PX_PER_UNIT = 16
/** Undo snapshots are full copies of the texture - about this much memory in total, so a
 * 2048x1024 cape keeps fewer steps than a 64x32 one instead of eating hundreds of MB. */
const HISTORY_BUDGET_BYTES = 64 * 1024 * 1024
const BRUSH_SIZES = [1, 2, 3, 4, 8, 16]

/**
 * Every side of the cape as its own panel (own user request: one grid per side, titled), laid out
 * like an unfolded cape - the four edges around the outside face they border. Rects are in
 * vanilla 64x32 units; `area` is the panel's CSS grid area in the unfolded layout.
 */
type PanelId = 'outside' | 'inside' | 'edgeTop' | 'edgeBottom' | 'edgeLeft' | 'edgeRight' | 'elytra'
const PANELS: Array<{ id: PanelId; rect: [x: number, y: number, w: number, h: number]; vertical?: boolean }> = [
  { id: 'edgeTop', rect: [1, 0, 10, 1] },
  { id: 'edgeLeft', rect: [0, 1, 1, 16], vertical: true },
  { id: 'outside', rect: [1, 1, 10, 16] },
  { id: 'edgeRight', rect: [11, 1, 1, 16], vertical: true },
  { id: 'edgeBottom', rect: [11, 0, 10, 1] },
  { id: 'inside', rect: [12, 1, 10, 16] },
  { id: 'elytra', rect: [22, 0, 24, 22] }
]

/** A panel's rectangle in texture pixels. */
interface Bounds {
  x: number
  y: number
  w: number
  h: number
}

interface Props {
  /** `null` = start a new cape from a blank template. */
  entry: CapeLibraryEntry | null
  skinDataUri: string | null
  skinVariant: SkinVariant
  onSaved: (entry: CapeLibraryEntry, isNew: boolean) => void
  onClose: () => void
}

function hexToRgba(hex: string): [number, number, number, number] {
  const value = parseInt(hex.slice(1), 16)
  return [(value >> 16) & 255, (value >> 8) & 255, value & 255, 255]
}

function rgbToHex(r: number, g: number, b: number): string {
  return `#${[r, g, b].map((channel) => channel.toString(16).padStart(2, '0')).join('')}`
}

function cssVar(name: string, fallback: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback
}

/**
 * Pixel editor for capes (own user request: draw capes yourself, like the skin editor). A cape
 * texture is flat, so unlike the skin editor this paints straight onto the 2D texture - shown as
 * one titled grid per side instead of the raw texture sheet - with the result live on the
 * player's own skin in 3D next to it. Painting never leaves the panel it started on.
 */
export function CapeEditorScreen({ entry, skinDataUri, skinVariant, onSaved, onClose }: Props) {
  const t = useTranslations()
  const language = useLanguage()
  const textureRef = useRef<HTMLCanvasElement | null>(null)
  const panelRefs = useRef<Partial<Record<PanelId, HTMLCanvasElement | null>>>({})
  const layoutRef = useRef<HTMLDivElement>(null)
  const layoutWrapped = useIsWrapped(layoutRef)
  const undoRef = useRef<ImageData[]>([])
  const redoRef = useRef<ImageData[]>([])
  const strokeRef = useRef<{ x: number; y: number; bounds: Bounds } | null>(null)
  const previewTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const [width, setWidth] = useState<number | null>(null)
  const [newWidth, setNewWidth] = useState(64)
  const [baseColor, setBaseColor] = useState('#3a3a3a')
  const [tool, setTool] = useState<Tool>('pencil')
  const [color, setColor] = useState('#ffffff')
  const [brushSize, setBrushSize] = useState(1)
  const [showGrid, setShowGrid] = useState(true)
  const [canUndo, setCanUndo] = useState(false)
  const [canRedo, setCanRedo] = useState(false)
  const [previewUri, setPreviewUri] = useState<string | null>(entry?.dataUri ?? null)
  const [name, setName] = useState(
    entry?.name ?? t.skin.capeDefaultName(new Date().toLocaleDateString(language === 'de' ? 'de-DE' : 'en-US'))
  )
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  /** Texture pixels per 64x32 unit, and screen pixels per texture pixel. */
  const unit = width ? width / 64 : 1
  const zoom = Math.max(1, SCREEN_PX_PER_UNIT / unit)
  const historyLimit = width ? Math.min(50, Math.max(5, Math.floor(HISTORY_BUDGET_BYTES / (width * (width / 2) * 4)))) : 50

  function boundsOf(rect: [number, number, number, number]): Bounds {
    const [x, y, w, h] = rect
    return { x: x * unit, y: y * unit, w: w * unit, h: h * unit }
  }

  /** Makes a PNG (a collection entry or a file from the PC) the working texture. */
  function loadTexture(dataUri: string): void {
    const image = new Image()
    image.onload = () => {
      const canvas = document.createElement('canvas')
      canvas.width = image.width
      canvas.height = image.height
      canvas.getContext('2d')?.drawImage(image, 0, 0)
      textureRef.current = canvas
      setWidth(image.width)
      setPreviewUri(dataUri)
    }
    image.src = dataUri
  }

  // Editing an existing collection cape: load its PNG as the working texture.
  useEffect(() => {
    if (entry) loadTexture(entry.dataUri)
  }, [entry?.id])

  /** Start from a cape PNG on the PC (own user request) - same checks as adding one to the
   * collection (2:1, 64x32 to 2048x1024, 5 MB). Saving then creates a new collection entry. */
  async function loadFromPc(): Promise<void> {
    setError(null)
    try {
      const picked = await window.api.selectCapePng()
      if (!picked) return
      loadTexture(picked.dataUri)
    } catch (err) {
      setError(formatError(err, t))
    }
  }

  function startNew(): void {
    const canvas = document.createElement('canvas')
    canvas.width = newWidth
    canvas.height = newWidth / 2
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    const s = newWidth / 64
    ctx.fillStyle = baseColor
    for (const region of CAPE_REGIONS) {
      for (const [x, y, w, h] of region.rects) ctx.fillRect(x * s, y * s, w * s, h * s)
    }
    textureRef.current = canvas
    setWidth(newWidth)
    setPreviewUri(canvas.toDataURL('image/png'))
  }

  function redraw(): void {
    const texture = textureRef.current
    if (!texture) return
    const background = cssVar('--bg', '#1b1b1b')
    const gridColor = cssVar('--text', '#ffffff')
    for (const panel of PANELS) {
      const canvas = panelRefs.current[panel.id]
      const ctx = canvas?.getContext('2d')
      if (!canvas || !ctx) continue
      const bounds = boundsOf(panel.rect)
      ctx.imageSmoothingEnabled = false
      ctx.fillStyle = background
      ctx.fillRect(0, 0, canvas.width, canvas.height)
      ctx.drawImage(texture, bounds.x, bounds.y, bounds.w, bounds.h, 0, 0, canvas.width, canvas.height)
      if (showGrid && zoom >= 4) {
        // Theme text color at low opacity - stays visible on light and dark color schemes alike.
        ctx.save()
        ctx.globalAlpha = 0.15
        ctx.strokeStyle = gridColor
        ctx.lineWidth = 1
        ctx.beginPath()
        for (let x = 0; x <= bounds.w; x++) {
          ctx.moveTo(x * zoom + 0.5, 0)
          ctx.lineTo(x * zoom + 0.5, canvas.height)
        }
        for (let y = 0; y <= bounds.h; y++) {
          ctx.moveTo(0, y * zoom + 0.5)
          ctx.lineTo(canvas.width, y * zoom + 0.5)
        }
        ctx.stroke()
        ctx.restore()
      }
    }
  }

  useEffect(redraw, [width, zoom, showGrid])

  function schedulePreview(): void {
    if (previewTimer.current) clearTimeout(previewTimer.current)
    previewTimer.current = setTimeout(() => {
      const texture = textureRef.current
      if (texture) setPreviewUri(texture.toDataURL('image/png'))
    }, 120)
  }

  function textureContext(): CanvasRenderingContext2D | null {
    return textureRef.current?.getContext('2d', { willReadFrequently: true }) ?? null
  }

  function pushHistory(): void {
    const ctx = textureContext()
    const texture = textureRef.current
    if (!ctx || !texture) return
    undoRef.current.push(ctx.getImageData(0, 0, texture.width, texture.height))
    if (undoRef.current.length > historyLimit) undoRef.current.shift()
    // A fresh edit invalidates whatever an earlier undo left on the redo stack.
    redoRef.current = []
    setCanUndo(true)
    setCanRedo(false)
  }

  function swapHistory(from: ImageData[], to: ImageData[]): void {
    const ctx = textureContext()
    const texture = textureRef.current
    const snapshot = from.pop()
    if (!ctx || !texture || !snapshot) return
    to.push(ctx.getImageData(0, 0, texture.width, texture.height))
    ctx.putImageData(snapshot, 0, 0)
    setCanUndo(undoRef.current.length > 0)
    setCanRedo(redoRef.current.length > 0)
    redraw()
    schedulePreview()
  }

  const handleUndo = (): void => swapHistory(undoRef.current, redoRef.current)
  const handleRedo = (): void => swapHistory(redoRef.current, undoRef.current)

  useEffect(() => {
    function onKeyDown(event: KeyboardEvent): void {
      if (!event.ctrlKey || event.target instanceof HTMLInputElement) return
      const key = event.key.toLowerCase()
      if (key === 'z' && !event.shiftKey) {
        event.preventDefault()
        handleUndo()
      } else if (key === 'y' || (key === 'z' && event.shiftKey)) {
        event.preventDefault()
        handleRedo()
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  })

  /** Texture coordinates under the pointer, for the panel with these bounds. */
  function texelAt(event: PointerEvent<HTMLCanvasElement>, bounds: Bounds): { x: number; y: number } {
    const rect = event.currentTarget.getBoundingClientRect()
    return {
      x: bounds.x + Math.floor((event.clientX - rect.left) / zoom),
      y: bounds.y + Math.floor((event.clientY - rect.top) / zoom)
    }
  }

  /** Paints one brush dab, clipped to the panel so a big brush never spills into another side. */
  function paintAt(ctx: CanvasRenderingContext2D, x: number, y: number, bounds: Bounds): void {
    const x0 = x - Math.floor((brushSize - 1) / 2)
    const y0 = y - Math.floor((brushSize - 1) / 2)
    ctx.save()
    ctx.beginPath()
    ctx.rect(bounds.x, bounds.y, bounds.w, bounds.h)
    ctx.clip()
    if (tool === 'eraser') {
      ctx.clearRect(x0, y0, brushSize, brushSize)
    } else {
      ctx.fillStyle = color
      ctx.fillRect(x0, y0, brushSize, brushSize)
    }
    ctx.restore()
  }

  /** Every texel on the line between two pointer samples - fast strokes would otherwise leave gaps. */
  function paintLine(ctx: CanvasRenderingContext2D, from: { x: number; y: number }, to: { x: number; y: number }, bounds: Bounds): void {
    const steps = Math.max(Math.abs(to.x - from.x), Math.abs(to.y - from.y), 1)
    for (let i = 0; i <= steps; i++) {
      paintAt(ctx, Math.round(from.x + ((to.x - from.x) * i) / steps), Math.round(from.y + ((to.y - from.y) * i) / steps), bounds)
    }
  }

  /** Flood fill that stays inside the panel's side of the cape. */
  function floodFill(ctx: CanvasRenderingContext2D, startX: number, startY: number, bounds: Bounds): void {
    const texture = textureRef.current
    if (!texture) return
    const w = texture.width
    const image = ctx.getImageData(0, 0, w, texture.height)
    const data = image.data
    const start = (startY * w + startX) * 4
    const target = [data[start], data[start + 1], data[start + 2], data[start + 3]]
    const replacement = hexToRgba(color)
    if (target.every((value, i) => value === replacement[i])) return
    const matches = (index: number): boolean => target.every((value, i) => data[index + i] === value)
    const stack = [[startX, startY]]
    while (stack.length > 0) {
      const [x, y] = stack.pop()!
      if (x < bounds.x || y < bounds.y || x >= bounds.x + bounds.w || y >= bounds.y + bounds.h) continue
      const index = (y * w + x) * 4
      if (!matches(index)) continue
      data.set(replacement, index)
      stack.push([x + 1, y], [x - 1, y], [x, y + 1], [x, y - 1])
    }
    ctx.putImageData(image, 0, 0)
  }

  function handlePointerDown(event: PointerEvent<HTMLCanvasElement>, bounds: Bounds): void {
    const ctx = textureContext()
    if (!ctx) return
    const texel = texelAt(event, bounds)

    if (tool === 'eyedropper') {
      const [r, g, b, a] = ctx.getImageData(texel.x, texel.y, 1, 1).data
      if (a > 0) setColor(rgbToHex(r, g, b))
      setTool('pencil')
      return
    }

    pushHistory()
    if (tool === 'fill') {
      floodFill(ctx, texel.x, texel.y, bounds)
      redraw()
      schedulePreview()
      return
    }
    event.currentTarget.setPointerCapture(event.pointerId)
    paintAt(ctx, texel.x, texel.y, bounds)
    strokeRef.current = { ...texel, bounds }
    redraw()
  }

  function handlePointerMove(event: PointerEvent<HTMLCanvasElement>): void {
    const ctx = textureContext()
    const last = strokeRef.current
    if (!ctx || !last) return
    const texel = texelAt(event, last.bounds)
    paintLine(ctx, last, texel, last.bounds)
    strokeRef.current = { ...texel, bounds: last.bounds }
    redraw()
    schedulePreview()
  }

  function handlePointerUp(): void {
    if (!strokeRef.current) return
    strokeRef.current = null
    schedulePreview()
  }

  /** Copies the outside onto the inside, mirrored - the usual starting point for the inner side. */
  function mirrorOutsideToInside(): void {
    const ctx = textureContext()
    const texture = textureRef.current
    if (!ctx || !texture) return
    pushHistory()
    const s = texture.width / 64
    const copy = document.createElement('canvas')
    copy.width = 10 * s
    copy.height = 16 * s
    copy.getContext('2d')?.drawImage(texture, 1 * s, 1 * s, 10 * s, 16 * s, 0, 0, 10 * s, 16 * s)
    ctx.save()
    ctx.translate(22 * s, 1 * s)
    ctx.scale(-1, 1)
    ctx.clearRect(0, 0, 10 * s, 16 * s)
    ctx.drawImage(copy, 0, 0)
    ctx.restore()
    redraw()
    schedulePreview()
  }

  async function handleSave(): Promise<void> {
    const texture = textureRef.current
    if (!texture) return
    setBusy(true)
    setError(null)
    try {
      const dataUri = texture.toDataURL('image/png')
      const finalName = name.trim() || t.skin.capeNamePlaceholder
      if (entry) {
        const updated = await window.api.updateCapeInLibrary(entry.id, dataUri, finalName)
        if (updated) onSaved(updated, false)
      } else {
        onSaved(await window.api.saveCapeToLibrary(dataUri, finalName), true)
      }
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setBusy(false)
    }
  }

  const header = (
    <header>
      <strong>{t.capeEditor.title}</strong>
      <button className="link-button" onClick={onClose}>
        {t.common.back}
      </button>
    </header>
  )

  if (!width) {
    if (entry) {
      return (
        <div className="mods-screen">
          {header}
          <p>{t.common.loading}</p>
        </div>
      )
    }
    return (
      <div className="mods-screen">
        {header}
        {error && <span className="error">{error}</span>}
        <section className="mods-section cape-converter-options">
          <h3>{t.capeEditor.loadHeading}</h3>
          <p className="version-warning">{t.capeEditor.loadHint}</p>
          <div>
            <button className="secondary-button" onClick={() => void loadFromPc()}>
              {t.capeEditor.loadFromPc}
            </button>
          </div>
        </section>
        <section className="mods-section cape-converter-options">
          <h3>{t.capeEditor.newHeading}</h3>
          <label className="cape-converter-row">
            <span>{t.capeEditor.resolution}</span>
            <Dropdown
              value={String(newWidth)}
              onChange={(value) => setNewWidth(Number(value))}
              options={EDITOR_WIDTHS.map((w) => ({ value: String(w), label: `${w}x${w / 2}` }))}
              ariaLabel={t.capeEditor.resolution}
            />
          </label>
          <span>{t.capeEditor.baseColor}</span>
          <ColorPicker color={baseColor} onChange={setBaseColor} />
          <div>
            <button className="primary-button" onClick={startNew}>
              {t.capeEditor.start}
            </button>
          </div>
        </section>
      </div>
    )
  }

  const toolButton = (id: Tool, label: string) => (
    <button className={`tool-button${tool === id ? ' active' : ''}`} onClick={() => setTool(id)}>
      {label}
    </button>
  )

  const panel = (id: PanelId) => {
    const definition = PANELS.find((candidate) => candidate.id === id)!
    const bounds = boundsOf(definition.rect)
    return (
      <div className={`cape-panel cape-panel--${id}${definition.vertical ? ' cape-panel--vertical' : ''}`}>
        <span className="cape-panel-title">{t.capeEditor.panels[id]}</span>
        <canvas
          ref={(element) => {
            panelRefs.current[id] = element
          }}
          className={`skin-editor-canvas cape-editor-canvas cape-editor-canvas--${tool}`}
          width={bounds.w * zoom}
          height={bounds.h * zoom}
          onPointerDown={(event) => handlePointerDown(event, bounds)}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerUp}
        />
      </div>
    )
  }

  return (
    <div className="mods-screen skin-editor-screen">
      {header}
      {error && <span className="error">{error}</span>}

      {layoutWrapped && <p className="version-warning editor-width-hint">{t.skinEditor.widenWindowHint}</p>}

      <div ref={layoutRef} className="skin-editor-layout">
        <div className="cape-editor-canvas-column">
          <div className="cape-panels">
            <div className="cape-net">
              {panel('edgeTop')}
              {panel('edgeLeft')}
              {panel('outside')}
              {panel('edgeRight')}
              {panel('edgeBottom')}
            </div>
            {panel('inside')}
            {panel('elytra')}
          </div>
          <p className="version-warning">{t.capeEditor.regionsHint}</p>
          {skinDataUri && (
            <SkinModelPreview
              skinDataUri={skinDataUri}
              variant={skinVariant}
              capeDataUri={previewUri}
              showCape={true}
              width={200}
              height={240}
            />
          )}
        </div>

        <div className="skin-editor-tools">
          <section className="mods-section">
            <h3>{t.skinEditor.toolHeading}</h3>
            <div className="skin-editor-tool-row cape-editor-tool-row">
              {toolButton('pencil', t.skinEditor.toolPencil)}
              {toolButton('eraser', t.skinEditor.toolEraser)}
              {toolButton('fill', t.capeEditor.toolFill)}
              {toolButton('eyedropper', t.skinEditor.toolEyedropper)}
              <button className="tool-button" disabled={!canUndo} onClick={handleUndo} title={t.skinEditor.undo} aria-label={t.skinEditor.undo}>
                ↶
              </button>
              <button className="tool-button" disabled={!canRedo} onClick={handleRedo} title={t.skinEditor.redo} aria-label={t.skinEditor.redo}>
                ↷
              </button>
            </div>
            <label className="cape-converter-row">
              <span>{t.capeEditor.brushSize}</span>
              <Dropdown
                value={String(brushSize)}
                onChange={(value) => setBrushSize(Number(value))}
                options={BRUSH_SIZES.map((size) => ({ value: String(size), label: `${size}x${size}` }))}
                ariaLabel={t.capeEditor.brushSize}
              />
            </label>
            <ColorPicker color={color} onChange={setColor} />
            <label className="checkbox-label">
              <input type="checkbox" className="toggle-switch" checked={showGrid} onChange={(event) => setShowGrid(event.target.checked)} />
              {t.skinEditor.showGrid}
            </label>
            <div>
              <button className="secondary-button" onClick={mirrorOutsideToInside}>
                {t.capeEditor.mirrorOutside}
              </button>
            </div>
          </section>

          <section className="mods-section">
            <h3>{t.skinEditor.saveHeading}</h3>
            <input
              type="text"
              className="skin-editor-name-input"
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder={t.skin.capeNamePlaceholder}
            />
            {entry && <p className="version-warning">{t.capeEditor.reactivateHint}</p>}
            <div>
              <button className="primary-button" disabled={busy} onClick={() => void handleSave()}>
                {busy ? t.skin.saving : entry ? t.capeEditor.saveUpdate : t.skin.capeSaveToCollection}
              </button>
            </div>
          </section>
        </div>
      </div>
    </div>
  )
}
