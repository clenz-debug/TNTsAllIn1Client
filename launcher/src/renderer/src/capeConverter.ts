/**
 * Turns any picture into a cape texture (own user request) - entirely in the renderer via a canvas,
 * no upload involved; the result goes through the normal "name it, save to collection, activate"
 * flow like a hand-made cape PNG.
 *
 * Cape texture layout in vanilla's 64x32 units (every resolution is a multiple, `scale = width / 64`):
 *   top (1,0) 10x1, bottom (11,0) 10x1, edges (0,1) and (11,1) 1x16,
 *   outside (1,1) 10x16 - the side other players see from behind,
 *   inside (12,1) 10x16 - facing the player's back,
 *   elytra (22,0) 24x22 - used when the player wears an elytra with a cape.
 */
/** The layout above as data (64x32 units) - shared with the cape editor's region overlay,
 * its "new cape" template fill and its mirror action. */
export type CapeRegionId = 'outside' | 'inside' | 'edges' | 'elytra'
export const CAPE_REGIONS: Array<{ id: CapeRegionId; rects: Array<[x: number, y: number, w: number, h: number]> }> = [
  { id: 'outside', rects: [[1, 1, 10, 16]] },
  { id: 'inside', rects: [[12, 1, 10, 16]] },
  {
    id: 'edges',
    rects: [
      [1, 0, 10, 1],
      [11, 0, 10, 1],
      [0, 1, 1, 16],
      [11, 1, 1, 16]
    ]
  },
  { id: 'elytra', rects: [[22, 0, 24, 22]] }
]

export type CapeFitMode = 'cover' | 'contain'
export type CapeInsideMode = 'mirror' | 'color'

/** Width : height of the cape's outside face (10x16 texture units). */
export const CAPE_FACE_ASPECT = 10 / 16

/** Part of the source picture (in its own pixels) shown on the cape in "cover" mode. */
export interface CropRect {
  x: number
  y: number
  width: number
  height: number
}

/** The largest centered face-shaped rectangle that fits inside the picture - the starting crop. */
export function defaultCrop(imageWidth: number, imageHeight: number): CropRect {
  const width = Math.min(imageWidth, imageHeight * CAPE_FACE_ASPECT)
  const height = width / CAPE_FACE_ASPECT
  return { x: (imageWidth - width) / 2, y: (imageHeight - height) / 2, width, height }
}

export interface CapeConverterOptions {
  width: number
  fit: CapeFitMode
  inside: CapeInsideMode
  /** `#rrggbb` - edges, inside (when not mirrored), elytra and the letterbox bars of "contain". */
  background: string
  /** Nearest-neighbour scaling for pixel art instead of smooth filtering for photos. */
  pixelated: boolean
  /** "cover" only: the chosen section of the picture; `null` = centered default. */
  crop: CropRect | null
}

export const CAPE_CONVERTER_WIDTHS = [64, 128, 256, 512, 1024, 2048]

function drawFitted(
  ctx: CanvasRenderingContext2D,
  image: CanvasImageSource & { width: number; height: number },
  x: number,
  y: number,
  w: number,
  h: number,
  fit: CapeFitMode,
  mirror: boolean,
  crop: CropRect | null
): void {
  if (fit === 'cover') {
    const source = crop ?? defaultCrop(image.width, image.height)
    ctx.save()
    if (mirror) {
      ctx.translate(x * 2 + w, 0)
      ctx.scale(-1, 1)
    }
    ctx.drawImage(image, source.x, source.y, source.width, source.height, x, y, w, h)
    ctx.restore()
    return
  }

  const imageRatio = image.width / image.height
  const targetRatio = w / h
  // "contain": shrink the whole picture until it fits; the background shows around it.
  const scale = imageRatio > targetRatio ? w / image.width : h / image.height
  const drawW = image.width * scale
  const drawH = image.height * scale
  const offsetX = x + (w - drawW) / 2
  const offsetY = y + (h - drawH) / 2

  ctx.save()
  ctx.beginPath()
  ctx.rect(x, y, w, h)
  ctx.clip()
  if (mirror) {
    ctx.translate(x * 2 + w, 0)
    ctx.scale(-1, 1)
  }
  ctx.drawImage(image, offsetX, offsetY, drawW, drawH)
  ctx.restore()
}

/** Returns the finished cape as a `data:image/png;base64,...` URI. */
export function renderCapeFromImage(image: HTMLImageElement, options: CapeConverterOptions): string {
  const s = options.width / 64
  const canvas = document.createElement('canvas')
  canvas.width = options.width
  canvas.height = options.width / 2
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('Canvas 2D context unavailable')
  ctx.imageSmoothingEnabled = !options.pixelated
  ctx.imageSmoothingQuality = 'high'

  const fillRect = (x: number, y: number, w: number, h: number): void => {
    ctx.fillStyle = options.background
    ctx.fillRect(x * s, y * s, w * s, h * s)
  }

  fillRect(1, 0, 10, 1)
  fillRect(11, 0, 10, 1)
  fillRect(0, 1, 1, 16)
  fillRect(11, 1, 1, 16)
  fillRect(22, 0, 24, 22)

  fillRect(1, 1, 10, 16)
  drawFitted(ctx, image, 1 * s, 1 * s, 10 * s, 16 * s, options.fit, false, options.crop)

  fillRect(12, 1, 10, 16)
  if (options.inside === 'mirror') {
    drawFitted(ctx, image, 12 * s, 1 * s, 10 * s, 16 * s, options.fit, true, options.crop)
  }

  return canvas.toDataURL('image/png')
}

/** Decoded size of a base64 `data:` URI, for the 5 MB server limit check before saving. */
export function dataUriByteLength(dataUri: string): number {
  const base64 = dataUri.slice(dataUri.indexOf(',') + 1)
  const padding = base64.endsWith('==') ? 2 : base64.endsWith('=') ? 1 : 0
  return (base64.length * 3) / 4 - padding
}

export function loadImageFile(file: File): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onerror = () => reject(reader.error ?? new Error('read failed'))
    reader.onload = () => {
      const image = new Image()
      image.onload = () => resolve(image)
      image.onerror = () => reject(new Error('decode failed'))
      image.src = String(reader.result)
    }
    reader.readAsDataURL(file)
  })
}
