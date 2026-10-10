import { crc32, inflateSync } from 'node:zlib'

const PNG_SIGNATURE = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10])
const IEND_CHUNK = Buffer.from([0, 0, 0, 0, 0x49, 0x45, 0x4e, 0x44, 0xae, 0x42, 0x60, 0x82])

/** PNG's own IHDR chunk always sits right after the 8-byte signature, width/height as big-endian
 * uint32 at byte offsets 16/20 - reading just that lets an obviously wrong file get rejected
 * before ever attempting a network call, rather than pulling in a full image-decoding dependency
 * for two numbers. Returns null for anything that isn't a PNG at all. Shared between skin
 * (`auth/skinApi.ts`) and cape (`cape/capeStorage.ts`) upload validation - same file format, same
 * check, no reason for two copies. */
export function readPngDimensions(buffer: Buffer): { width: number; height: number } | null {
  if (buffer.length < 24 || !buffer.subarray(0, 8).equals(PNG_SIGNATURE)) return null
  return { width: buffer.readUInt32BE(16), height: buffer.readUInt32BE(20) }
}

/** Samples per pixel and the allowed bit depths for each PNG color type. */
const COLOR_TYPES: Record<number, { channels: number; depths: readonly number[] }> = {
  0: { channels: 1, depths: [1, 2, 4, 8, 16] },
  2: { channels: 3, depths: [8, 16] },
  3: { channels: 1, depths: [1, 2, 4, 8] },
  4: { channels: 2, depths: [8, 16] },
  6: { channels: 4, depths: [8, 16] }
}

/** Adam7: x start, y start, x step, y step of each of the seven passes of an interlaced image. */
const ADAM7_PASSES = [
  [0, 0, 8, 8],
  [4, 0, 8, 8],
  [0, 4, 4, 8],
  [2, 0, 4, 4],
  [0, 2, 2, 4],
  [1, 0, 2, 2],
  [0, 1, 1, 2]
] as const

/** The scanline groups of the decompressed image data: `rows` lines of one filter byte plus `stride` bytes each. */
function scanlineLayout(width: number, height: number, bitsPerPixel: number, interlaced: boolean): Array<{ rows: number; stride: number }> {
  if (!interlaced) return [{ rows: height, stride: Math.ceil((width * bitsPerPixel) / 8) }]
  const layout: Array<{ rows: number; stride: number }> = []
  for (const [xStart, yStart, xStep, yStep] of ADAM7_PASSES) {
    const passWidth = Math.ceil((width - xStart) / xStep)
    const passHeight = Math.ceil((height - yStart) / yStep)
    if (passWidth > 0 && passHeight > 0) layout.push({ rows: passHeight, stride: Math.ceil((passWidth * bitsPerPixel) / 8) })
  }
  return layout
}

/**
 * Checks the whole file, not just its header, and returns a copy that holds nothing but the image:
 * header, palette, transparency, image data and the end marker. Text, metadata and unknown chunks
 * are dropped, as is anything after the end marker; the image data must decompress to exactly the
 * size the header promises, with nothing left over. The file is public under our domain, so it must
 * not be usable to park arbitrary data behind a valid-looking header.
 *
 * A file that already is in this form comes back byte for byte. Must stay the same routine as
 * `backend/src/png.ts`: the server stores a cape in exactly this form, and the cape screen
 * recognises the active cape by comparing hashes of it.
 *
 * Only call with dimensions that are already bounded: the decompressed size is derived from them.
 */
export function sanitizePng(buffer: Buffer): Buffer | null {
  if (buffer.length < 8 || !buffer.subarray(0, 8).equals(PNG_SIGNATURE)) return null

  let header: Buffer | null = null
  let palette: Buffer | null = null
  let transparency: Buffer | null = null
  const dataChunks: Buffer[] = []
  const data: Buffer[] = []
  let width = 0
  let height = 0
  let colorType = 0
  let bitsPerPixel = 0
  let interlaced = false
  let ended = false

  for (let offset = 8; offset + 12 <= buffer.length; ) {
    const length = buffer.readUInt32BE(offset)
    const end = offset + 12 + length
    if (end > buffer.length) return null
    if (crc32(buffer.subarray(offset + 4, end - 4)) !== buffer.readUInt32BE(end - 4)) return null
    const type = buffer.toString('latin1', offset + 4, offset + 8)
    const chunk = buffer.subarray(offset, end)
    const body = buffer.subarray(offset + 8, end - 4)
    offset = end

    if (!header) {
      if (type !== 'IHDR' || length !== 13) return null
      width = body.readUInt32BE(0)
      height = body.readUInt32BE(4)
      colorType = body[9]!
      const format = COLOR_TYPES[colorType]
      // Compression and filter method only have the value 0, interlace 0 (none) or 1 (Adam7)
      if (!format || !format.depths.includes(body[8]!) || body[10] !== 0 || body[11] !== 0 || body[12]! > 1) return null
      if (width === 0 || height === 0) return null
      bitsPerPixel = format.channels * body[8]!
      interlaced = body[12] === 1
      header = chunk
    } else if (type === 'IHDR') {
      return null
    } else if (type === 'PLTE') {
      if (palette || dataChunks.length > 0 || length === 0 || length % 3 !== 0 || length > 768) return null
      palette = chunk
    } else if (type === 'tRNS') {
      if (transparency || dataChunks.length > 0) return null
      transparency = chunk
    } else if (type === 'IDAT') {
      if (length > 0) {
        dataChunks.push(chunk)
        data.push(body)
      }
    } else if (type === 'IEND') {
      if (length !== 0) return null
      ended = true
      break
    }
    // Every other chunk is optional by definition and simply left out.
  }
  if (!header || !ended || dataChunks.length === 0) return null

  // A palette only means something for palette images; transparency has a fixed size per color type
  // and does not exist for the two types with an alpha channel.
  if (colorType === 3) {
    if (!palette) return null
    if (transparency && transparency.length - 12 > (palette.length - 12) / 3) return null
  } else {
    palette = null
    if (colorType === 0 || colorType === 2) {
      if (transparency && transparency.length - 12 !== (colorType === 0 ? 2 : 6)) return null
    } else {
      transparency = null
    }
  }

  const layout = scanlineLayout(width, height, bitsPerPixel, interlaced)
  const expected = layout.reduce((sum, group) => sum + group.rows * (group.stride + 1), 0)
  const compressed = Buffer.concat(data)
  let raw: Buffer
  try {
    const result = inflateSync(compressed, { maxOutputLength: expected, info: true }) as unknown as {
      buffer: Buffer
      engine: { bytesWritten: number }
    }
    // Nothing may follow the compressed image inside the data chunks
    if (result.engine.bytesWritten !== compressed.length) return null
    raw = result.buffer
  } catch {
    return null
  }
  if (raw.length !== expected) return null

  // Every line starts with its filter type, 0 to 4
  let position = 0
  for (const group of layout) {
    for (let row = 0; row < group.rows; row++) {
      if (raw[position]! > 4) return null
      position += group.stride + 1
    }
  }

  return Buffer.concat([PNG_SIGNATURE, header, ...(palette ? [palette] : []), ...(transparency ? [transparency] : []), ...dataChunks, IEND_CHUNK])
}
