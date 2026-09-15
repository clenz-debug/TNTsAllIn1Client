/** PNG's own IHDR chunk always sits right after the 8-byte signature, width/height as big-endian
 * uint32 at byte offsets 16/20 - reading just that lets an obviously wrong file get rejected
 * before ever attempting a network call, rather than pulling in a full image-decoding dependency
 * for two numbers. Returns null for anything that isn't a PNG at all. Shared between skin
 * (`auth/skinApi.ts`) and cape (`cape/capeStorage.ts`) upload validation - same file format, same
 * check, no reason for two copies. */
export function readPngDimensions(buffer: Buffer): { width: number; height: number } | null {
  const PNG_SIGNATURE = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10])
  if (buffer.length < 24 || !buffer.subarray(0, 8).equals(PNG_SIGNATURE)) return null
  return { width: buffer.readUInt32BE(16), height: buffer.readUInt32BE(20) }
}
