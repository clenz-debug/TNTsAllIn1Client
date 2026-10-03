import { deflateSync, inflateSync } from 'node:zlib'

/** A decoded picture: four bytes per pixel (red, green, blue, alpha), row by row from the top. */
export interface RgbaImage {
  width: number
  height: number
  data: Uint8Array
}

const PNG_SIGNATURE = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10])
/** Samples per pixel for each PNG color type (0 gray, 2 RGB, 3 palette, 4 gray+alpha, 6 RGBA). */
const CHANNELS: Record<number, number> = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }

/**
 * Reads a PNG into plain RGBA - every color type and bit depth the format has, as long as the file
 * is not interlaced (Minecraft's textures never are). Written out here instead of pulling in an
 * image library: `newTexturesPack.ts` only needs to recolor one texture and to put a few next to
 * each other. Throws for anything it can't read.
 */
export function decodePng(buffer: Buffer): RgbaImage {
  if (buffer.length < 8 || !buffer.subarray(0, 8).equals(PNG_SIGNATURE)) {
    throw new Error('not a PNG file')
  }
  let width = 0
  let height = 0
  let bitDepth = 0
  let colorType = 0
  let palette: Buffer | null = null
  let transparency: Buffer | null = null
  const dataChunks: Buffer[] = []

  for (let offset = 8; offset + 8 <= buffer.length; ) {
    const length = buffer.readUInt32BE(offset)
    const type = buffer.toString('latin1', offset + 4, offset + 8)
    const body = buffer.subarray(offset + 8, offset + 8 + length)
    if (type === 'IHDR') {
      width = body.readUInt32BE(0)
      height = body.readUInt32BE(4)
      bitDepth = body[8]
      colorType = body[9]
      if (body[12] !== 0) throw new Error('interlaced PNG files are not supported')
    } else if (type === 'PLTE') {
      palette = body
    } else if (type === 'tRNS') {
      transparency = body
    } else if (type === 'IDAT') {
      dataChunks.push(body)
    } else if (type === 'IEND') {
      break
    }
    offset += 12 + length
  }

  const channels = CHANNELS[colorType]
  if (!channels || width === 0 || height === 0) throw new Error('unsupported PNG file')
  const bitsPerPixel = channels * bitDepth
  const bytesPerPixel = Math.max(1, bitsPerPixel >> 3)
  const stride = Math.ceil((width * bitsPerPixel) / 8)
  const raw = inflateSync(Buffer.concat(dataChunks))
  if (raw.length < (stride + 1) * height) throw new Error('truncated PNG file')

  // Every row starts with the filter it was written with; taking the filters back gives the samples.
  const rows = Buffer.alloc(stride * height)
  for (let y = 0; y < height; y++) {
    const filter = raw[y * (stride + 1)]
    const source = y * (stride + 1) + 1
    const row = y * stride
    for (let x = 0; x < stride; x++) {
      const left = x >= bytesPerPixel ? rows[row + x - bytesPerPixel] : 0
      const up = y > 0 ? rows[row - stride + x] : 0
      const upLeft = y > 0 && x >= bytesPerPixel ? rows[row - stride + x - bytesPerPixel] : 0
      let predicted = 0
      if (filter === 1) predicted = left
      else if (filter === 2) predicted = up
      else if (filter === 3) predicted = (left + up) >> 1
      else if (filter === 4) predicted = paeth(left, up, upLeft)
      rows[row + x] = (raw[source + x] + predicted) & 0xff
    }
  }

  /** One sample as written in the file - 0 to 2^bitDepth - 1. */
  const sampleAt = (y: number, index: number): number => {
    const row = y * stride
    if (bitDepth === 8) return rows[row + index]
    if (bitDepth === 16) return rows.readUInt16BE(row + index * 2)
    const bit = index * bitDepth
    return (rows[row + (bit >> 3)] >> (8 - bitDepth - (bit & 7))) & ((1 << bitDepth) - 1)
  }
  const maxSample = (1 << bitDepth) - 1
  const toByte = (sample: number): number => (bitDepth === 8 ? sample : Math.round((sample * 255) / maxSample))

  const data = new Uint8Array(width * height * 4)
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const out = (y * width + x) * 4
      const first = sampleAt(y, x * channels)
      if (colorType === 3) {
        if (!palette || first * 3 + 2 >= palette.length) throw new Error('PNG palette entry missing')
        data[out] = palette[first * 3]
        data[out + 1] = palette[first * 3 + 1]
        data[out + 2] = palette[first * 3 + 2]
        data[out + 3] = transparency && first < transparency.length ? transparency[first] : 255
      } else if (colorType === 0 || colorType === 4) {
        const gray = toByte(first)
        data[out] = gray
        data[out + 1] = gray
        data[out + 2] = gray
        if (colorType === 4) data[out + 3] = toByte(sampleAt(y, x * 2 + 1))
        else data[out + 3] = transparency && transparency.length >= 2 && transparency.readUInt16BE(0) === first ? 0 : 255
      } else {
        const green = sampleAt(y, x * channels + 1)
        const blue = sampleAt(y, x * channels + 2)
        data[out] = toByte(first)
        data[out + 1] = toByte(green)
        data[out + 2] = toByte(blue)
        if (colorType === 6) {
          data[out + 3] = toByte(sampleAt(y, x * 4 + 3))
        } else {
          const keyed =
            transparency !== null &&
            transparency.length >= 6 &&
            transparency.readUInt16BE(0) === first &&
            transparency.readUInt16BE(2) === green &&
            transparency.readUInt16BE(4) === blue
          data[out + 3] = keyed ? 0 : 255
        }
      }
    }
  }
  return { width, height, data }
}

function paeth(left: number, up: number, upLeft: number): number {
  const estimate = left + up - upLeft
  const toLeft = Math.abs(estimate - left)
  const toUp = Math.abs(estimate - up)
  const toUpLeft = Math.abs(estimate - upLeft)
  if (toLeft <= toUp && toLeft <= toUpLeft) return left
  return toUp <= toUpLeft ? up : upLeft
}

/** Writes RGBA as an 8-bit PNG with alpha - not the smallest file possible, but one every reader takes. */
export function encodePng(image: RgbaImage): Buffer {
  const { width, height, data } = image
  const stride = width * 4
  const raw = Buffer.alloc((stride + 1) * height)
  for (let y = 0; y < height; y++) {
    // Filter 0: the row as it is.
    raw.set(data.subarray(y * stride, (y + 1) * stride), y * (stride + 1) + 1)
  }
  const header = Buffer.alloc(13)
  header.writeUInt32BE(width, 0)
  header.writeUInt32BE(height, 4)
  header[8] = 8
  header[9] = 6
  return Buffer.concat([PNG_SIGNATURE, chunk('IHDR', header), chunk('IDAT', deflateSync(raw)), chunk('IEND', Buffer.alloc(0))])
}

function chunk(type: string, body: Buffer): Buffer {
  const typeAndBody = Buffer.concat([Buffer.from(type, 'latin1'), body])
  const result = Buffer.alloc(body.length + 12)
  result.writeUInt32BE(body.length, 0)
  typeAndBody.copy(result, 4)
  result.writeUInt32BE(crc32(typeAndBody), body.length + 8)
  return result
}

const CRC_TABLE = new Uint32Array(256).map((_, index) => {
  let value = index
  for (let bit = 0; bit < 8; bit++) {
    value = value & 1 ? 0xedb88320 ^ (value >>> 1) : value >>> 1
  }
  return value >>> 0
})

function crc32(buffer: Buffer): number {
  let crc = 0xffffffff
  for (const byte of buffer) {
    crc = CRC_TABLE[(crc ^ byte) & 0xff] ^ (crc >>> 8)
  }
  return (crc ^ 0xffffffff) >>> 0
}
