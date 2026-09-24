import { CAPE_MAX_WIDTH, CAPE_MIN_WIDTH } from './config.js'

const PNG_SIGNATURE = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a])

/** Width/height from the IHDR chunk, which the PNG spec requires to be the very first chunk. */
export function readPngDimensions(buffer: Buffer): { width: number; height: number } | null {
  if (buffer.length < 24 || !buffer.subarray(0, 8).equals(PNG_SIGNATURE)) return null
  if (buffer.toString('ascii', 12, 16) !== 'IHDR') return null
  return { width: buffer.readUInt32BE(16), height: buffer.readUInt32BE(20) }
}

export type CapeCheck =
  | { ok: true; width: number; height: number }
  | { ok: false; error: 'invalid_png' | 'wrong_dimensions'; width?: number; height?: number }

/** Vanilla's cape layout scaled up: 2:1, width a multiple of 64 (64x32, 128x64, ... 2048x1024). */
export function checkCapePng(buffer: Buffer): CapeCheck {
  const dimensions = readPngDimensions(buffer)
  if (!dimensions) return { ok: false, error: 'invalid_png' }
  const { width, height } = dimensions
  const valid =
    width === height * 2 && width % CAPE_MIN_WIDTH === 0 && width >= CAPE_MIN_WIDTH && width <= CAPE_MAX_WIDTH
  return valid ? { ok: true, width, height } : { ok: false, error: 'wrong_dimensions', width, height }
}
