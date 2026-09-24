import { useEffect, useRef, useState } from 'react'
import { SkinViewer } from 'skinview3d'
import { CanvasTexture, NearestFilter, type Texture } from 'three'
import { formatError } from '../formatError'
import { useIsWrapped } from '../useIsWrapped'
import { useLanguage, useTranslations } from '../i18n/LanguageContext'
import type { SkinLibraryEntry, SkinVariant } from '../../../shared/types'
import { BODY_PART_TOGGLES } from '../skinEditor/bodyParts'
import { BodyPartDiagram } from '../skinEditor/BodyPartDiagram'
import { ColorPicker } from '../skinEditor/ColorPicker'
import { hitTest, uvToPixel } from '../skinEditor/raycastPaint'
import {
  canvasToPngBytes,
  createGridCanvas,
  paintPixel,
  pickColor,
  refreshGridOverlay,
  type PaintTool
} from '../skinEditor/skinBuffer'

/** How many strokes back "Rückgängig" can go - a whole-canvas `ImageData` snapshot per stroke, so
 * an unbounded stack would grow without limit over a long editing session. */
const MAX_UNDO_HISTORY = 20

interface Props {
  onClose: () => void
  /** Set when opened via an existing library entry's "Bearbeiten" button - skips the template/
   * upload chooser and loads straight into that skin, and "speichern" overwrites it in place
   * instead of creating a new entry. Does *not* touch the user's actual Mojang account skin -
   * that only ever happens via the "Verwenden" button in the Skins screen's library list. */
  editingLibraryEntry?: SkinLibraryEntry
}

interface SkinSource {
  dataUri: string
  variant: SkinVariant
}

const CANVAS_WIDTH = 400
const CANVAS_HEIGHT = 480
const SKIN_TEXTURE_SIZE = 64

export function SkinEditorScreen({ onClose, editingLibraryEntry }: Props) {
  const t = useTranslations()
  const language = useLanguage()

  const [skinSource, setSkinSource] = useState<SkinSource | null>(
    editingLibraryEntry ? { dataUri: editingLibraryEntry.dataUri, variant: editingLibraryEntry.variant } : null
  )
  const [chooserVariant, setChooserVariant] = useState<SkinVariant>('classic')
  const [tool, setTool] = useState<PaintTool>('pencil')
  const [color, setColor] = useState('#000000')
  const [visibility, setVisibility] = useState<Record<string, boolean>>(() =>
    Object.fromEntries(BODY_PART_TOGGLES.map((toggle) => [toggle.id, true]))
  )
  const [name, setName] = useState(
    editingLibraryEntry?.name ?? t.skinEditor.defaultName(new Date().toLocaleDateString(language === 'de' ? 'de-DE' : 'en-US'))
  )
  const [existingId, setExistingId] = useState(editingLibraryEntry?.id)
  const [gridEnabled, setGridEnabled] = useState(true)
  const [canUndo, setCanUndo] = useState(false)
  const [canRedo, setCanRedo] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const canvasRef = useRef<HTMLCanvasElement>(null)
  const layoutRef = useRef<HTMLDivElement>(null)
  const layoutWrapped = useIsWrapped(layoutRef)
  const viewerRef = useRef<SkinViewer | null>(null)
  const toolRef = useRef(tool)
  const colorRef = useRef(color)
  const visibilityRef = useRef(visibility)
  const gridEnabledRef = useRef(gridEnabled)
  // The plain skin texture skinview3d itself creates vs. the grid-overlay one we swap in on top
  // of it - both point at the same underlying pixel data, only the *displayed* one differs.
  const originalTextureRef = useRef<Texture | null>(null)
  const gridCanvasRef = useRef<HTMLCanvasElement | null>(null)
  const gridTextureRef = useRef<CanvasTexture | null>(null)
  // Whole-canvas snapshots, one pushed per stroke (not per pixel) - see MAX_UNDO_HISTORY.
  const historyRef = useRef<ImageData[]>([])
  const redoRef = useRef<ImageData[]>([])

  toolRef.current = tool
  colorRef.current = color
  visibilityRef.current = visibility
  gridEnabledRef.current = gridEnabled

  useEffect(() => {
    if (!skinSource || !canvasRef.current) return

    // Deliberately no `skin`/`model` in the constructor options: with a `data:` URI source that
    // loads asynchronously (skinview3d's `loadSkin` returns a Promise for anything but an
    // already-decoded `TextureSource`), `playerObject.skin.map` is still `null` right after the
    // constructor returns - capturing it synchronously here previously grabbed that `null` as
    // "the original texture", which then made the model render fully white the first time the
    // grid toggle was switched back off (own bug, caught via live testing). Loading explicitly
    // and waiting for it to resolve avoids the race entirely.
    const viewer = new SkinViewer({
      canvas: canvasRef.current,
      width: CANVAS_WIDTH,
      height: CANVAS_HEIGHT
    })
    viewerRef.current = viewer

    const gridCanvas = createGridCanvas(SKIN_TEXTURE_SIZE)
    gridCanvasRef.current = gridCanvas
    const gridTexture = new CanvasTexture(gridCanvas)
    gridTexture.magFilter = NearestFilter
    gridTexture.minFilter = NearestFilter
    gridTextureRef.current = gridTexture

    function refreshDisplayedTexture(): void {
      if (gridEnabledRef.current) {
        refreshGridOverlay(viewer.skinCanvas, gridCanvas)
        gridTexture.needsUpdate = true
        viewer.playerObject.skin.map = gridTexture
      } else if (originalTextureRef.current) {
        viewer.playerObject.skin.map = originalTextureRef.current
      }
    }

    let cancelled = false
    void viewer.loadSkin(skinSource.dataUri, { model: skinSource.variant === 'slim' ? 'slim' : 'default' }).then(() => {
      if (cancelled) return
      originalTextureRef.current = viewer.playerObject.skin.map
      // Grid defaults to on - show it immediately once the real skin texture exists, not just
      // after the first stroke.
      refreshDisplayedTexture()
    })

    function paintableTargets() {
      // Both base and overlay parts are valid raycast targets whenever visible - see the doc
      // comment on BODY_PART_TOGGLES for why restricting this to base-only made painting a
      // visible overlay part (e.g. a hat) paint through to the base part underneath instead.
      return BODY_PART_TOGGLES.filter((toggle) => visibilityRef.current[toggle.id]).map((toggle) =>
        toggle.getObject(viewer.playerObject.skin)
      )
    }

    function applyAtPointer(clientX: number, clientY: number): void {
      // Never reached with 'view' in practice (onPointerDown returns before calling this, and
      // onPointerMove only calls it while `painting` is true, which 'view' never sets) - guarded
      // explicitly anyway so paintPixel's narrower parameter type stays honest rather than casting.
      if (toolRef.current === 'view') return
      const hit = hitTest(clientX, clientY, viewer.canvas, viewer.camera, paintableTargets())
      if (!hit) return
      const { x, y } = uvToPixel(hit.uv, SKIN_TEXTURE_SIZE, SKIN_TEXTURE_SIZE)
      const ctx = viewer.skinCanvas.getContext('2d')
      if (!ctx) return

      if (toolRef.current === 'eyedropper') {
        setColor(pickColor(ctx, x, y))
        setTool('pencil')
        return
      }
      paintPixel(ctx, x, y, toolRef.current, colorRef.current)
      // Both textures read from the same underlying `skinCanvas`, but only whichever one is
      // *currently displayed* was getting marked dirty here before - switching display modes
      // later (grid on/off) would then show stale, pre-edit content on the one that had been
      // sitting unmarked in the background. Marking the original unconditionally, and letting
      // refreshDisplayedTexture() handle the grid one, keeps both always in sync regardless of
      // which is on-screen at any given moment.
      if (originalTextureRef.current) originalTextureRef.current.needsUpdate = true
      refreshDisplayedTexture()
    }

    let painting = false

    function onPointerDown(event: PointerEvent): void {
      // "Ansicht"/view tool: never paints, so OrbitControls handles the drag exactly like it
      // would if this listener didn't exist at all - no hit-test, no disabling controls.
      if (toolRef.current === 'view') return

      const hit = hitTest(event.clientX, event.clientY, viewer.canvas, viewer.camera, paintableTargets())
      if (!hit) return
      // Capture phase, ahead of OrbitControls' own bubble-phase mousedown listener on this same
      // canvas - disabling controls here (before OrbitControls ever sees the event) is what stops
      // a paint stroke from also rotating the camera.
      viewer.controls.enabled = false
      painting = true

      // One snapshot per stroke (not per pixel) for "Rückgängig" - taken before this stroke's
      // first pixel change, and only for tools that actually mutate pixels (not the eyedropper).
      if (toolRef.current === 'pencil' || toolRef.current === 'eraser') {
        const ctx = viewer.skinCanvas.getContext('2d')
        if (ctx) {
          historyRef.current.push(ctx.getImageData(0, 0, SKIN_TEXTURE_SIZE, SKIN_TEXTURE_SIZE))
          if (historyRef.current.length > MAX_UNDO_HISTORY) historyRef.current.shift()
          setCanUndo(true)
          // A fresh stroke invalidates any redo history from a previous undo - standard
          // undo/redo semantics, same as any text editor.
          redoRef.current = []
          setCanRedo(false)
        }
      }

      applyAtPointer(event.clientX, event.clientY)
    }

    function onPointerMove(event: PointerEvent): void {
      if (!painting) return
      applyAtPointer(event.clientX, event.clientY)
    }

    function onPointerUp(): void {
      painting = false
      viewer.controls.enabled = true
    }

    viewer.canvas.addEventListener('pointerdown', onPointerDown, { capture: true })
    window.addEventListener('pointermove', onPointerMove)
    window.addEventListener('pointerup', onPointerUp)

    return () => {
      cancelled = true
      viewer.canvas.removeEventListener('pointerdown', onPointerDown, { capture: true })
      window.removeEventListener('pointermove', onPointerMove)
      window.removeEventListener('pointerup', onPointerUp)
      gridTexture.dispose()
      viewer.dispose()
      viewerRef.current = null
      originalTextureRef.current = null
      gridCanvasRef.current = null
      gridTextureRef.current = null
      historyRef.current = []
      redoRef.current = []
      setCanUndo(false)
      setCanRedo(false)
    }
    // Deliberately only `skinSource` - the viewer is created once per chosen starting skin, tool/
    // color/visibility/grid changes flow in through the refs above instead of re-creating it.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [skinSource])

  function toggleGrid(value: boolean): void {
    setGridEnabled(value)
    gridEnabledRef.current = value
    const viewer = viewerRef.current
    const gridCanvas = gridCanvasRef.current
    const gridTexture = gridTextureRef.current
    if (!viewer || !gridCanvas || !gridTexture) return
    if (value) {
      refreshGridOverlay(viewer.skinCanvas, gridCanvas)
      gridTexture.needsUpdate = true
      viewer.playerObject.skin.map = gridTexture
    } else {
      viewer.playerObject.skin.map = originalTextureRef.current
    }
  }

  /** Refreshes whichever texture is currently on-screen (grid overlay or the plain skin) after an
   * undo/redo swap - same texture-selection logic as the effect's own `refreshDisplayedTexture`,
   * duplicated here in miniature since undo/redo run from outside that effect's closure. */
  function refreshTextureAfterHistoryChange(): void {
    const viewer = viewerRef.current
    if (!viewer) return
    // Keep both textures in sync unconditionally - see the matching comment in applyAtPointer for
    // why only updating whichever one is currently displayed left the other one stale.
    if (originalTextureRef.current) originalTextureRef.current.needsUpdate = true
    if (gridEnabledRef.current && gridCanvasRef.current && gridTextureRef.current) {
      refreshGridOverlay(viewer.skinCanvas, gridCanvasRef.current)
      gridTextureRef.current.needsUpdate = true
    }
  }

  function handleUndo(): void {
    const viewer = viewerRef.current
    const snapshot = historyRef.current.pop()
    if (!viewer || !snapshot) return
    const ctx = viewer.skinCanvas.getContext('2d')
    if (!ctx) return
    // Current state goes onto the redo stack before being overwritten, so "Vorwärts" can bring it
    // back - standard undo/redo semantics.
    redoRef.current.push(ctx.getImageData(0, 0, SKIN_TEXTURE_SIZE, SKIN_TEXTURE_SIZE))
    ctx.putImageData(snapshot, 0, 0)
    refreshTextureAfterHistoryChange()
    setCanUndo(historyRef.current.length > 0)
    setCanRedo(true)
  }

  function handleRedo(): void {
    const viewer = viewerRef.current
    const snapshot = redoRef.current.pop()
    if (!viewer || !snapshot) return
    const ctx = viewer.skinCanvas.getContext('2d')
    if (!ctx) return
    historyRef.current.push(ctx.getImageData(0, 0, SKIN_TEXTURE_SIZE, SKIN_TEXTURE_SIZE))
    ctx.putImageData(snapshot, 0, 0)
    refreshTextureAfterHistoryChange()
    setCanUndo(true)
    setCanRedo(redoRef.current.length > 0)
  }

  function toggleVisibility(id: string, value: boolean): void {
    setVisibility((current) => ({ ...current, [id]: value }))
    const viewer = viewerRef.current
    if (!viewer) return
    const object = BODY_PART_TOGGLES.find((toggle) => toggle.id === id)?.getObject(viewer.playerObject.skin)
    if (object) object.visible = value
  }

  async function loadTemplate(variant: SkinVariant): Promise<void> {
    setBusy(true)
    setError(null)
    try {
      const dataUri = await window.api.loadSkinTemplate(variant)
      setSkinSource({ dataUri, variant })
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setBusy(false)
    }
  }

  async function loadOwnPng(): Promise<void> {
    setBusy(true)
    setError(null)
    try {
      const result = await window.api.loadSkinPngForEditor()
      if (result) setSkinSource({ dataUri: result.dataUri, variant: chooserVariant })
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setBusy(false)
    }
  }

  async function handleSaveToLibrary(): Promise<void> {
    if (!viewerRef.current) return
    setBusy(true)
    setError(null)
    try {
      const bytes = await canvasToPngBytes(viewerRef.current.skinCanvas)
      const saved = await window.api.saveSkinToLibrary(bytes, skinSource!.variant, name, existingId)
      setExistingId(saved.id)
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setBusy(false)
    }
  }

  async function handleExport(): Promise<void> {
    if (!viewerRef.current) return
    setBusy(true)
    setError(null)
    try {
      const bytes = await canvasToPngBytes(viewerRef.current.skinCanvas)
      await window.api.exportSkinPng(bytes, `${name || 'skin'}.png`)
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setBusy(false)
    }
  }

  if (!skinSource) {
    return (
      <div className="mods-screen skin-editor-screen">
        <header>
          <strong>{t.skinEditor.chooserTitle}</strong>
          <button className="link-button" onClick={onClose}>
            {t.common.back}
          </button>
        </header>

        {error && <span className="error">{error}</span>}

        <section className="mods-section">
          <h3>{t.skinEditor.chooseSourceHeading}</h3>
          <label className="checkbox-label">
            <input type="radio" name="chooser-variant" checked={chooserVariant === 'classic'} onChange={() => setChooserVariant('classic')} />
            {t.skin.variantClassic}
          </label>
          <label className="checkbox-label">
            <input type="radio" name="chooser-variant" checked={chooserVariant === 'slim'} onChange={() => setChooserVariant('slim')} />
            {t.skin.variantSlim}
          </label>
          <div>
            <button className="primary-button" disabled={busy} onClick={() => void loadTemplate(chooserVariant)}>
              {chooserVariant === 'classic' ? t.skinEditor.loadSteveTemplate : t.skinEditor.loadAlexTemplate}
            </button>
            <button className="secondary-button" disabled={busy} onClick={() => void loadOwnPng()}>
              {t.skinEditor.loadOwnPng}
            </button>
          </div>
        </section>
      </div>
    )
  }

  return (
    <div className="mods-screen skin-editor-screen">
      <header>
        <strong>{t.skinEditor.title}</strong>
        <button className="link-button" onClick={onClose}>
          {t.common.back}
        </button>
      </header>

      {error && <span className="error">{error}</span>}

      {layoutWrapped && <p className="version-warning editor-width-hint">{t.skinEditor.widenWindowHint}</p>}

      <div ref={layoutRef} className="skin-editor-layout">
        <canvas ref={canvasRef} className="skin-editor-canvas" width={CANVAS_WIDTH} height={CANVAS_HEIGHT} />

        <div className="skin-editor-tools">
          <section className="mods-section">
            <h3>{t.skinEditor.toolHeading}</h3>
            <div className="skin-editor-tool-row">
              <button className={`tool-button${tool === 'pencil' ? ' active' : ''}`} onClick={() => setTool('pencil')}>
                {t.skinEditor.toolPencil}
              </button>
              <button className={`tool-button${tool === 'eraser' ? ' active' : ''}`} onClick={() => setTool('eraser')}>
                {t.skinEditor.toolEraser}
              </button>
              <button className={`tool-button${tool === 'eyedropper' ? ' active' : ''}`} onClick={() => setTool('eyedropper')}>
                {t.skinEditor.toolEyedropper}
              </button>
              <button className={`tool-button${tool === 'view' ? ' active' : ''}`} onClick={() => setTool('view')}>
                {t.skinEditor.toolView}
              </button>
              <button className="tool-button" disabled={!canUndo} onClick={handleUndo} title={t.skinEditor.undo} aria-label={t.skinEditor.undo}>
                ↶
              </button>
              <button className="tool-button" disabled={!canRedo} onClick={handleRedo} title={t.skinEditor.redo} aria-label={t.skinEditor.redo}>
                ↷
              </button>
            </div>
            <ColorPicker color={color} onChange={setColor} />
            <label className="checkbox-label">
              <input type="checkbox" className="toggle-switch" checked={gridEnabled} onChange={(event) => toggleGrid(event.target.checked)} />
              {t.skinEditor.showGrid}
            </label>
          </section>

          <section className="mods-section">
            <h3>{t.skinEditor.visibilityHeading}</h3>
            <p className="body-part-diagram-hint">{t.skinEditor.visibilityHint}</p>
            <div className="body-part-diagram-row">
              <BodyPartDiagram layerLabel={t.skinEditor.layerBase} layerPrefix="base" visibility={visibility} onToggle={toggleVisibility} />
              <BodyPartDiagram layerLabel={t.skinEditor.layerOverlay} layerPrefix="overlay" visibility={visibility} onToggle={toggleVisibility} />
            </div>
          </section>

          <section className="mods-section">
            <h3>{t.skinEditor.saveHeading}</h3>
            <input
              type="text"
              className="skin-editor-name-input"
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder={t.skin.namePlaceholder}
            />
            <div>
              <button className="primary-button" disabled={busy} onClick={() => void handleSaveToLibrary()}>
                {busy ? t.skin.saving : existingId ? t.skinEditor.updateInLibrary : t.skinEditor.saveToLibrary}
              </button>
              <button className="secondary-button" disabled={busy} onClick={() => void handleExport()}>
                {t.skinEditor.exportPng}
              </button>
            </div>
          </section>
        </div>
      </div>
    </div>
  )
}
