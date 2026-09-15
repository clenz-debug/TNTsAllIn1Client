import { readFile } from 'node:fs/promises'
import type { BrowserWindow } from 'electron'
import { dialog } from 'electron'
import { readPngDimensions } from '../pngUtils'
import { getB2Config } from './b2Config'
import { deleteCapeObject, putCapeObject } from './b2Client'

/** Vanilla's own native cape format (2:1 aspect ratio) - the conservative v1 choice, since unlike
 * skins there's no authority (Mojang's own upload endpoint) to validate against ahead of time.
 * Cape Provider's own docs only mention a 10MB *file size* ceiling, nothing about pixel
 * dimensions - **needs live verification** (upload a real test cape, check it renders correctly,
 * not stretched/misaligned) before trusting this is actually what it expects; a one-line change
 * here if it turns out to support other sizes too, same as the equivalent skin check would be. */
const CAPE_WIDTH = 64
const CAPE_HEIGHT = 32

function objectKeyFor(uuid: string): string {
  return `${uuid.replace(/-/g, '')}.png`
}

function publicUrlFor(uuid: string): string {
  return `${getB2Config().publicBaseUrl}/${objectKeyFor(uuid)}`
}

/** Throws a German error, never touches the network - same "reject an obviously wrong file before
 * any request" pattern `skinApi.ts#uploadSkinBuffer` already uses for skins. */
export function validateCapePng(buffer: Buffer): { width: number; height: number } {
  const dimensions = readPngDimensions(buffer)
  if (!dimensions) {
    throw new Error('Datei ist kein gültiges PNG.')
  }
  if (dimensions.width !== CAPE_WIDTH || dimensions.height !== CAPE_HEIGHT) {
    throw new Error(`Capes müssen exakt ${CAPE_WIDTH}x${CAPE_HEIGHT} sein, diese Datei ist ${dimensions.width}x${dimensions.height}.`)
  }
  return dimensions
}

const OPEN_CAPE_PNG_DIALOG: Electron.OpenDialogOptions = {
  title: 'Cape-PNG auswählen',
  properties: ['openFile'],
  filters: [{ name: 'PNG-Bild', extensions: ['png'] }]
}

/** Mirrors `skinApi.ts#loadPngFileForEditor` exactly - opens the native picker, reads + validates,
 * but does **not** upload. The renderer needs this separate "pick and preview" step before
 * "confirm and upload" so `SkinModelPreview` can show the chosen cape first (same two-step flow
 * skin library entries already get via `pendingRename`). Returns `null` if cancelled. */
export async function loadCapePngForPreview(window: BrowserWindow | null): Promise<{ buffer: Buffer; width: number; height: number } | null> {
  const result = window ? await dialog.showOpenDialog(window, OPEN_CAPE_PNG_DIALOG) : await dialog.showOpenDialog(OPEN_CAPE_PNG_DIALOG)
  if (result.canceled || result.filePaths.length === 0) return null

  const buffer = await readFile(result.filePaths[0])
  const dimensions = validateCapePng(buffer)
  return { buffer, width: dimensions.width, height: dimensions.height }
}

function toDataUri(buffer: Buffer): string {
  return `data:image/png;base64,${buffer.toString('base64')}`
}

/** Re-validates server-side even though the renderer already validated once for the preview -
 * defense in depth, same reasoning `uploadSkinBuffer` already applies to a caller's claimed PNG. */
export async function uploadCustomCape(uuid: string, pngBuffer: Buffer): Promise<{ url: string; dataUri: string }> {
  validateCapePng(pngBuffer)
  const config = getB2Config()
  await putCapeObject(config, objectKeyFor(uuid), pngBuffer)
  return { url: publicUrlFor(uuid), dataUri: toDataUri(pngBuffer) }
}

export async function deleteCustomCape(uuid: string): Promise<void> {
  const config = getB2Config()
  await deleteCapeObject(config, objectKeyFor(uuid))
}

/** Plain unauthenticated GET - the B2 bucket is public-read, no signing needed. A 404 means
 * "no custom cape set", not an error - same convention `capeStorage`'s own delete uses. */
export async function getCustomCapeStatus(uuid: string): Promise<{ exists: boolean; dataUri: string | null }> {
  const response = await fetch(publicUrlFor(uuid))
  if (response.status === 404) {
    return { exists: false, dataUri: null }
  }
  if (!response.ok) {
    throw new Error(`Cape-Status konnte nicht geladen werden (${response.status}).`)
  }
  const buffer = Buffer.from(await response.arrayBuffer())
  return { exists: true, dataUri: toDataUri(buffer) }
}
