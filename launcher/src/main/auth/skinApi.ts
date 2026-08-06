import { readFile } from 'node:fs/promises'
import type { MinecraftCape, MinecraftSkin } from '../../shared/types'

export type SkinVariant = 'classic' | 'slim'

interface SkinUploadResponse {
  skins: MinecraftSkin[]
  capes: MinecraftCape[]
}

class MinecraftApiError extends Error {
  constructor(status: number, body: string) {
    super(`Minecraft API call failed (${status}): ${body}`)
    this.name = 'MinecraftApiError'
  }
}

/** PNG's own IHDR chunk always sits right after the 8-byte signature, width/height as big-endian
 * uint32 at byte offsets 16/20 - reading just that lets an obviously wrong file get rejected
 * before ever attempting a network call, rather than pulling in a full image-decoding dependency
 * for two numbers. Returns null for anything that isn't a PNG at all. */
function readPngDimensions(buffer: Buffer): { width: number; height: number } | null {
  const PNG_SIGNATURE = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10])
  if (buffer.length < 24 || !buffer.subarray(0, 8).equals(PNG_SIGNATURE)) return null
  return { width: buffer.readUInt32BE(16), height: buffer.readUInt32BE(20) }
}

/**
 * Fetches a skin/cape texture from Mojang's texture CDN and returns it as a data: URI. The
 * renderer's CSP deliberately only allows `img-src 'self' data:` (not `textures.minecraft.net`
 * directly) - same "renderer never talks to the network itself" split every other API call in
 * this app already follows (auth, version manifest, update check all live in main/).
 */
export async function fetchTextureDataUri(url: string): Promise<string> {
  const response = await fetch(url)
  if (!response.ok) {
    throw new MinecraftApiError(response.status, await response.text())
  }
  const buffer = Buffer.from(await response.arrayBuffer())
  const contentType = response.headers.get('content-type') ?? 'image/png'
  return `data:${contentType};base64,${buffer.toString('base64')}`
}

/**
 * Uploads a new skin PNG (Phase 7, step 1 of the roadmap's three-part skin/cape editor - just
 * upload for now, no curated gallery or pixel editor yet). Validates dimensions locally first -
 * the roadmap flags that excessive failed retries against this endpoint have been reported to
 * risk temporary account restrictions, so catching an obviously-wrong file before it's ever sent
 * matters more here than for the other, lower-stakes API calls in this project.
 */
export async function uploadSkin(accessToken: string, filePath: string, variant: SkinVariant): Promise<SkinUploadResponse> {
  const fileBuffer = await readFile(filePath)
  const dimensions = readPngDimensions(fileBuffer)
  if (!dimensions) {
    throw new Error('Datei ist kein gültiges PNG.')
  }
  if (dimensions.width !== 64 || (dimensions.height !== 64 && dimensions.height !== 32)) {
    throw new Error(
      `Minecraft-Skins müssen 64x64 (oder das alte 64x32-Format) sein, diese Datei ist ${dimensions.width}x${dimensions.height}.`
    )
  }

  const form = new FormData()
  form.append('variant', variant)
  form.append('file', new Blob([fileBuffer], { type: 'image/png' }), 'skin.png')

  const response = await fetch('https://api.minecraftservices.com/minecraft/profile/skins', {
    method: 'POST',
    headers: { Authorization: `Bearer ${accessToken}` },
    body: form
  })
  if (!response.ok) {
    throw new MinecraftApiError(response.status, await response.text())
  }
  return (await response.json()) as SkinUploadResponse
}
