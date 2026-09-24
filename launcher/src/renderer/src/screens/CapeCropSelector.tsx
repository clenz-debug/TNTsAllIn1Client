import { useEffect, useMemo, useRef } from 'react'
import type { PointerEvent } from 'react'
import { CAPE_FACE_ASPECT, type CropRect } from '../capeConverter'
import { useTranslations } from '../i18n/LanguageContext'

interface Props {
  image: HTMLImageElement
  /** `null` = no section frame (the converter's "whole image" mode) - the picture is still shown
   * so the eyedropper has something to pick from. */
  crop: CropRect | null
  onChange: (crop: CropRect) => void
  pixelated: boolean
  /** Set while the converter's eyedropper is armed: the next click on the picture reports that
   * pixel's color (`#rrggbb`) instead of moving the frame. */
  onPickColor: ((hex: string) => void) | null
}

/** Longest side of the on-screen picture - small pictures are scaled up to it, big ones down. */
const DISPLAY_SIZE = 320
/** Smallest crop, as a share of the largest possible one - keeps the frame grabbable. */
const MIN_ZOOM_SHARE = 0.05

function maxCropWidth(image: HTMLImageElement): number {
  return Math.min(image.width, image.height * CAPE_FACE_ASPECT)
}

/** Keeps the crop face-shaped and fully inside the picture. */
function clampCrop(image: HTMLImageElement, x: number, y: number, width: number): CropRect {
  const maxWidth = maxCropWidth(image)
  const clampedWidth = Math.min(maxWidth, Math.max(maxWidth * MIN_ZOOM_SHARE, width))
  const height = clampedWidth / CAPE_FACE_ASPECT
  return {
    x: Math.min(image.width - clampedWidth, Math.max(0, x)),
    y: Math.min(image.height - height, Math.max(0, y)),
    width: clampedWidth,
    height
  }
}

/**
 * Picture view + section picker for the picture-to-cape converter (the frame only in "choose
 * section" mode; the picture itself always, so the eyedropper can pick from it) (own user request: see the picture and choose
 * just a part of it). A face-shaped frame over the picture - drag to move, slider or mouse wheel
 * to zoom around its center; everything outside the frame is dimmed.
 */
export function CapeCropSelector({ image, crop, onChange, pixelated, onPickColor }: Props) {
  const t = useTranslations()
  const dragStart = useRef<{ pointerX: number; pointerY: number; crop: CropRect } | null>(null)
  const stageRef = useRef<HTMLDivElement>(null)
  const scale = DISPLAY_SIZE / Math.max(image.width, image.height)
  const maxWidth = maxCropWidth(image)

  // Full-resolution copy of the picture to read pixels from - sampling the on-screen <img> would
  // only give its scaled, possibly smoothed rendering.
  const pixelSource = useMemo(() => {
    const canvas = document.createElement('canvas')
    canvas.width = image.width
    canvas.height = image.height
    const ctx = canvas.getContext('2d', { willReadFrequently: true })
    ctx?.drawImage(image, 0, 0)
    return ctx
  }, [image])

  function handlePick(event: PointerEvent<HTMLDivElement>): void {
    if (!onPickColor || !pixelSource) return
    const rect = event.currentTarget.getBoundingClientRect()
    const x = Math.min(image.width - 1, Math.max(0, Math.floor((event.clientX - rect.left) / scale)))
    const y = Math.min(image.height - 1, Math.max(0, Math.floor((event.clientY - rect.top) / scale)))
    const [r, g, b] = pixelSource.getImageData(x, y, 1, 1).data
    onPickColor(`#${[r, g, b].map((channel) => channel.toString(16).padStart(2, '0')).join('')}`)
  }

  function zoomTo(width: number): void {
    if (!crop) return
    const centerX = crop.x + crop.width / 2
    const centerY = crop.y + crop.height / 2
    const height = width / CAPE_FACE_ASPECT
    onChange(clampCrop(image, centerX - width / 2, centerY - height / 2, width))
  }

  function handlePointerDown(event: PointerEvent<HTMLDivElement>): void {
    if (!crop) return
    event.currentTarget.setPointerCapture(event.pointerId)
    dragStart.current = { pointerX: event.clientX, pointerY: event.clientY, crop }
  }

  function handlePointerMove(event: PointerEvent<HTMLDivElement>): void {
    const start = dragStart.current
    if (!start) return
    const dx = (event.clientX - start.pointerX) / scale
    const dy = (event.clientY - start.pointerY) / scale
    onChange(clampCrop(image, start.crop.x + dx, start.crop.y + dy, start.crop.width))
  }

  // Native, non-passive listener: React's own onWheel is passive, so it couldn't stop the whole
  // screen from scrolling while the mouse wheel zooms the frame. The ref always holds the latest
  // zoom closure, so the listener itself only needs registering once.
  const zoomByWheel = useRef<(deltaY: number) => void>(() => undefined)
  zoomByWheel.current = (deltaY) => {
    if (crop) zoomTo(crop.width * (deltaY > 0 ? 1.1 : 1 / 1.1))
  }
  useEffect(() => {
    const stage = stageRef.current
    if (!stage) return
    const listener = (event: globalThis.WheelEvent): void => {
      if (!stage.querySelector('.cape-crop-frame')) return
      event.preventDefault()
      zoomByWheel.current(event.deltaY)
    }
    stage.addEventListener('wheel', listener, { passive: false })
    return () => stage.removeEventListener('wheel', listener)
  }, [])

  return (
    <div className="cape-crop">
      <div
        ref={stageRef}
        className={`cape-crop-stage${onPickColor ? ' cape-crop-stage--picking' : ''}`}
        style={{ width: image.width * scale, height: image.height * scale }}
        onPointerDown={onPickColor ? handlePick : undefined}
      >
        <img
          src={image.src}
          alt=""
          draggable={false}
          className="cape-crop-image"
          style={{ imageRendering: pixelated ? 'pixelated' : 'auto' }}
        />
        {crop && (
          <div
            className="cape-crop-frame"
            style={{
              left: crop.x * scale,
              top: crop.y * scale,
              width: crop.width * scale,
              height: crop.height * scale,
              // While picking a color, clicks must reach the stage (and its pixel lookup) instead.
              pointerEvents: onPickColor ? 'none' : 'auto'
            }}
            onPointerDown={handlePointerDown}
            onPointerMove={handlePointerMove}
            onPointerUp={() => (dragStart.current = null)}
            onPointerCancel={() => (dragStart.current = null)}
          />
        )}
      </div>
      {crop && (
        <>
          <label className="cape-converter-row">
            <span>{t.skin.converterZoom}</span>
            <input
              type="range"
              min={MIN_ZOOM_SHARE}
              max={1}
              step={0.01}
              // Slider right = bigger section (zoomed out), left = smaller section (zoomed in).
              value={crop.width / maxWidth}
              onChange={(event) => zoomTo(Number(event.target.value) * maxWidth)}
            />
          </label>
          <p className="version-warning">{t.skin.converterCropHint}</p>
        </>
      )}
    </div>
  )
}
