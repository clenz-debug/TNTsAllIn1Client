import { readFile } from 'node:fs/promises'
import type { BrowserWindow } from 'electron'
import { dialog } from 'electron'
import { localizedError, localizedErrorMessage } from '../../shared/errorMessages'
import type { MinecraftCape, MinecraftSkin, SkinVariant } from '../../shared/types'
import { readPngDimensions } from '../pngUtils'

interface SkinUploadResponse {
  skins: MinecraftSkin[]
  capes: MinecraftCape[]
}

class MinecraftApiError extends Error {
  constructor(status: number, body: string) {
    super(localizedErrorMessage('auth.minecraftApiFailed', { status, detail: body }))
    this.name = 'MinecraftApiError'
  }
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
 * Uploads a new skin PNG straight from an in-memory buffer (Phase 7 step 3: the pixel editor and
 * the skin library both already have PNG bytes in hand and have no reason to round-trip them
 * through a temp file first). Validates dimensions locally first - the roadmap flags that
 * excessive failed retries against this endpoint have been reported to risk temporary account
 * restrictions, so catching an obviously-wrong file before it's ever sent matters more here than
 * for the other, lower-stakes API calls in this project.
 */
export async function uploadSkinBuffer(accessToken: string, fileBuffer: Buffer, variant: SkinVariant): Promise<SkinUploadResponse> {
  const dimensions = readPngDimensions(fileBuffer)
  if (!dimensions) {
    throw localizedError('image.invalidPng')
  }
  if (dimensions.width !== 64 || (dimensions.height !== 64 && dimensions.height !== 32)) {
    throw localizedError('skin.wrongDimensions', { width: dimensions.width, height: dimensions.height })
  }

  const form = new FormData()
  form.append('variant', variant)
  form.append('file', new Blob([new Uint8Array(fileBuffer)], { type: 'image/png' }), 'skin.png')

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

/**
 * Uploads a new skin PNG picked straight from disk (Phase 7, step 1's original direct-upload
 * button - kept working unchanged after the step-3 refactor). Thin `readFile` + delegate wrapper
 * around {@link uploadSkinBuffer}, which now holds the actual validation/upload logic.
 */
export async function uploadSkin(accessToken: string, filePath: string, variant: SkinVariant): Promise<SkinUploadResponse> {
  const fileBuffer = await readFile(filePath)
  return uploadSkinBuffer(accessToken, fileBuffer, variant)
}

const OPEN_SKIN_PNG_DIALOG: Electron.OpenDialogOptions = {
  title: 'Skin-PNG auswählen',
  properties: ['openFile'],
  filters: [{ name: 'PNG-Bild', extensions: ['png'] }]
}

/**
 * Opens the same native "Skin-PNG auswählen" dialog as the direct-upload button, but only reads
 * and validates the file - does **not** upload it to Mojang. This is the "load a PNG into the
 * pixel editor" path (Phase 7 step 3): the user may want to tweak it further before it ever
 * becomes their active skin. Returns `null` if the dialog was cancelled, same convention as the
 * existing `SkinUpload` IPC handler.
 */
export async function loadPngFileForEditor(window: BrowserWindow | null): Promise<{ buffer: Buffer; width: number; height: number } | null> {
  const result = window ? await dialog.showOpenDialog(window, OPEN_SKIN_PNG_DIALOG) : await dialog.showOpenDialog(OPEN_SKIN_PNG_DIALOG)
  if (result.canceled || result.filePaths.length === 0) return null

  const buffer = await readFile(result.filePaths[0])
  const dimensions = readPngDimensions(buffer)
  if (!dimensions) {
    throw localizedError('image.invalidPng')
  }
  if (dimensions.width !== 64 || (dimensions.height !== 64 && dimensions.height !== 32)) {
    throw localizedError('skin.wrongDimensions', { width: dimensions.width, height: dimensions.height })
  }
  return { buffer, width: dimensions.width, height: dimensions.height }
}
