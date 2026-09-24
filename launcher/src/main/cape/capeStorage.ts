import { createHash } from 'node:crypto'
import { readFile } from 'node:fs/promises'
import type { BrowserWindow } from 'electron'
import { dialog } from 'electron'
import { localizedError } from '../../shared/errorMessages'
import type { CapeUploadResult, CustomCapeStatus } from '../../shared/types'
import { readPngDimensions } from '../pngUtils'

/**
 * Custom capes (own cosmetic system, independent of Mojang's capes): the player's *active* cape
 * lives on our own server (`backend/`, deployed to the host behind nxlc.de) as
 * `https://nxlc.de/tntcapes/<dashed-uuid>.png` - exactly the URL template the bundled
 * "Cape Provider" mod looks up for every player it sees. Every other saved cape stays local (see
 * `capeLibrary.ts`), like the skin library. Uploads prove ownership with the player's own Minecraft
 * access token; the server asks Mojang whose token it is and only ever writes that UUID's file, so
 * no shared secret is baked into the launcher.
 */
const CAPE_API_BASE = 'https://nxlc.de/app'
const CAPE_PUBLIC_BASE = 'https://nxlc.de/tntcapes'

/** Must match `backend/src/config.ts` - checked here first so an obviously wrong file never gets sent. */
const CAPE_MAX_BYTES = 5 * 1024 * 1024
const CAPE_MIN_WIDTH = 64
const CAPE_MAX_WIDTH = 2048

/** Dashed UUID, matching the server's file names and the mods' `§id` URL template - see
 * `backend/src/capes.ts#dashedUuid` for why `§idNoHyphen` can't be used. */
function publicUrlFor(uuid: string): string {
  const hex = uuid.replace(/-/g, '').toLowerCase()
  const dashed = `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
  return `${CAPE_PUBLIC_BASE}/${dashed}.png`
}

export function capeSha1(buffer: Buffer): string {
  return createHash('sha1').update(buffer).digest('hex')
}

function toDataUri(buffer: Buffer): string {
  return `data:image/png;base64,${buffer.toString('base64')}`
}

/** 2:1 like vanilla's 64x32, scaled up in steps of 64 up to 2048x1024; at most 5 MB. */
export function validateCapePng(buffer: Buffer): { width: number; height: number } {
  if (buffer.length > CAPE_MAX_BYTES) {
    throw localizedError('cape.tooLarge', { maxMb: CAPE_MAX_BYTES / (1024 * 1024) })
  }
  const dimensions = readPngDimensions(buffer)
  if (!dimensions) {
    throw localizedError('image.invalidPng')
  }
  const { width, height } = dimensions
  if (width !== height * 2 || width % CAPE_MIN_WIDTH !== 0 || width < CAPE_MIN_WIDTH || width > CAPE_MAX_WIDTH) {
    throw localizedError('cape.wrongDimensions', { actualWidth: width, actualHeight: height })
  }
  return dimensions
}

const OPEN_CAPE_PNG_DIALOG: Electron.OpenDialogOptions = {
  title: 'Cape-PNG auswählen',
  properties: ['openFile'],
  filters: [{ name: 'PNG-Bild', extensions: ['png'] }]
}

/** Opens the native picker, reads + validates - does **not** save or upload anything; the renderer
 * shows the picked cape and asks for a name first. Returns `null` if cancelled. */
export async function loadCapePngForPreview(window: BrowserWindow | null): Promise<{ buffer: Buffer; width: number; height: number } | null> {
  const result = window ? await dialog.showOpenDialog(window, OPEN_CAPE_PNG_DIALOG) : await dialog.showOpenDialog(OPEN_CAPE_PNG_DIALOG)
  if (result.canceled || result.filePaths.length === 0) return null

  const buffer = await readFile(result.filePaths[0])
  const dimensions = validateCapePng(buffer)
  return { buffer, width: dimensions.width, height: dimensions.height }
}

/** Maps the server's `{ error: <code> }` answers onto the launcher's own de/en messages. */
async function throwForResponse(response: Response, fallbackCode: 'cape.uploadFailed' | 'cape.deleteFailed'): Promise<never> {
  const body = (await response.json().catch(() => ({}))) as { error?: string; width?: number; height?: number }
  switch (body.error) {
    case 'unauthorized':
      throw localizedError('cape.unauthorized')
    case 'rate_limited':
      throw localizedError('cape.rateLimited')
    case 'too_large':
      throw localizedError('cape.tooLarge', { maxMb: CAPE_MAX_BYTES / (1024 * 1024) })
    case 'invalid_png':
      throw localizedError('image.invalidPng')
    case 'wrong_dimensions':
      throw localizedError('cape.wrongDimensions', { actualWidth: body.width ?? '?', actualHeight: body.height ?? '?' })
    default:
      throw localizedError(fallbackCode, { status: response.status, detail: body.error ?? response.statusText })
  }
}

/** Makes this PNG the player's active cape on the server (replacing any previous one). Re-validates
 * even though the picker already did - the renderer may hand in any library entry. */
export async function uploadCustomCape(accessToken: string, uuid: string, pngBuffer: Buffer): Promise<CapeUploadResult> {
  validateCapePng(pngBuffer)
  const response = await fetch(`${CAPE_API_BASE}/capes`, {
    method: 'PUT',
    headers: { Authorization: `Bearer ${accessToken}`, 'Content-Type': 'image/png' },
    body: new Uint8Array(pngBuffer)
  })
  if (!response.ok) await throwForResponse(response, 'cape.uploadFailed')
  return { url: publicUrlFor(uuid), dataUri: toDataUri(pngBuffer), sha1: capeSha1(pngBuffer) }
}

/** Removes the active cape from the server - the local library copies stay untouched. */
export async function deleteCustomCape(accessToken: string): Promise<void> {
  const response = await fetch(`${CAPE_API_BASE}/capes`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${accessToken}` }
  })
  if (!response.ok) await throwForResponse(response, 'cape.deleteFailed')
}

/** The same public URL other players' Cape Provider fetches - a 404 just means "no active cape".
 * The query string sidesteps Apache's 5-minute cache header right after a change. */
export async function getCustomCapeStatus(uuid: string): Promise<CustomCapeStatus> {
  const response = await fetch(`${publicUrlFor(uuid)}?t=${Date.now()}`)
  if (response.status === 404) {
    return { exists: false, dataUri: null, sha1: null }
  }
  if (!response.ok) {
    throw localizedError('cape.statusLoadFailed', { status: response.status })
  }
  const buffer = Buffer.from(await response.arrayBuffer())
  return { exists: true, dataUri: toDataUri(buffer), sha1: capeSha1(buffer) }
}
