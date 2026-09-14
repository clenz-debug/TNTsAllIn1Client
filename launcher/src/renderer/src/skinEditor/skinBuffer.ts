/** `'view'` disables painting entirely - a dedicated "just rotate the model" mode, requested
 * because otherwise the only way to orbit the camera was to drag on a spot that misses the model
 * (awkward once it fills most of the canvas). */
export type PaintTool = 'pencil' | 'eraser' | 'eyedropper' | 'view'

/** Pencil fills the pixel with the current color; eraser clears it to true alpha-0 (`clearRect`,
 * not a white/transparent-looking fill) so the skin's actual transparency is preserved. */
export function paintPixel(ctx: CanvasRenderingContext2D, x: number, y: number, tool: 'pencil' | 'eraser', color: string): void {
  if (tool === 'eraser') {
    ctx.clearRect(x, y, 1, 1)
  } else {
    ctx.fillStyle = color
    ctx.fillRect(x, y, 1, 1)
  }
}

/** Reads back the color of an already-painted pixel, as a `#rrggbb` hex string ready to feed
 * straight into an `<input type="color">`. */
export function pickColor(ctx: CanvasRenderingContext2D, x: number, y: number): string {
  const [r, g, b] = ctx.getImageData(x, y, 1, 1).data
  return `#${[r, g, b].map((channel) => channel.toString(16).padStart(2, '0')).join('')}`
}

/** Encodes the current canvas contents as PNG bytes, ready for the `ArrayBuffer`-based IPC calls
 * (`saveSkinToLibrary`/`uploadEditedSkin`/`exportSkinPng`) - structured-clone carries `ArrayBuffer`
 * across the context bridge natively, no base64 round-trip needed. */
export async function canvasToPngBytes(canvas: HTMLCanvasElement): Promise<ArrayBuffer> {
  const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/png'))
  if (!blob) {
    throw new Error('Konnte den Skin nicht als PNG kodieren.')
  }
  return blob.arrayBuffer()
}

/** How many on-screen pixels each texture texel occupies in the grid-overlay canvas - large
 * enough that a 1px grid line stays a thin border around each texel instead of eating a visible
 * chunk of it. */
export const GRID_CELL_SIZE = 8

/** A blank canvas sized to hold `textureSize`x`textureSize` texels at `GRID_CELL_SIZE` each -
 * created once per editor session and only ever refreshed in place afterward, not resized on
 * every stroke (resizing a canvas clears it and is unnecessary work repeated many times per
 * second while dragging). */
export function createGridCanvas(textureSize: number, cellSize = GRID_CELL_SIZE): HTMLCanvasElement {
  const canvas = document.createElement('canvas')
  canvas.width = textureSize * cellSize
  canvas.height = textureSize * cellSize
  return canvas
}

/**
 * Redraws `target` as an upscaled, nearest-neighbor copy of `source` (the real skin pixels) with
 * thin gridlines at every texel boundary - "otherwise painting is hard" (own user feedback), since
 * a 64x64 texture mapped onto a 3D model gives no visual cue where one pixel ends and the next
 * begins. `target` is purely a display aid, swapped in as the model's texture only while the grid
 * toggle is on (see `SkinEditorScreen.tsx`) - `source` (skinview3d's own `skinCanvas`) stays the
 * only thing ever read back for saving/exporting, so grid lines never leak into the saved file.
 */
export function refreshGridOverlay(source: HTMLCanvasElement, target: HTMLCanvasElement, cellSize = GRID_CELL_SIZE): void {
  const ctx = target.getContext('2d')
  if (!ctx) return
  ctx.imageSmoothingEnabled = false
  ctx.clearRect(0, 0, target.width, target.height)
  ctx.drawImage(source, 0, 0, target.width, target.height)

  ctx.strokeStyle = 'rgba(0, 0, 0, 0.35)'
  ctx.lineWidth = 1
  for (let x = 0; x <= source.width; x++) {
    const px = x * cellSize + 0.5
    ctx.beginPath()
    ctx.moveTo(px, 0)
    ctx.lineTo(px, target.height)
    ctx.stroke()
  }
  for (let y = 0; y <= source.height; y++) {
    const py = y * cellSize + 0.5
    ctx.beginPath()
    ctx.moveTo(0, py)
    ctx.lineTo(target.width, py)
    ctx.stroke()
  }
}
