import { randomUUID } from 'node:crypto'
import { mkdir, readFile, rm, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { app } from 'electron'
import type { CapeLibraryEntry } from '../../shared/types'
import { capeSha1, validateCapePng } from './capeStorage'

/**
 * The local cape collection (own user decision): only the *active* cape is uploaded to our server,
 * every other saved cape stays on this PC - same model as the skin library (`skin/skinLibrary.ts`),
 * in its own folder next to it under `userData`. Also the source the future offline mode will
 * read the player's own cape from.
 */
function libraryDir(): string {
  return join(app.getPath('userData'), 'capes')
}

function indexPath(): string {
  return join(libraryDir(), 'capes.json')
}

function pngPath(id: string): string {
  return join(libraryDir(), `${id}.png`)
}

interface StoredIndexEntry {
  id: string
  name: string
  createdAt: string
}

async function readIndex(): Promise<StoredIndexEntry[]> {
  try {
    return JSON.parse(await readFile(indexPath(), 'utf-8')) as StoredIndexEntry[]
  } catch {
    return []
  }
}

async function writeIndex(entries: StoredIndexEntry[]): Promise<void> {
  await mkdir(libraryDir(), { recursive: true })
  await writeFile(indexPath(), JSON.stringify(entries, null, 2), 'utf-8')
}

function toEntry(stored: StoredIndexEntry, buffer: Buffer): CapeLibraryEntry {
  const { width, height } = validateCapePng(buffer)
  return { ...stored, width, height, sha1: capeSha1(buffer), dataUri: `data:image/png;base64,${buffer.toString('base64')}` }
}

/** Every saved cape with its PNG inlined - an index row whose PNG is missing or no longer valid is
 * skipped (and dropped from the index by the next save/delete), same as the skin library. */
export async function listCapeLibrary(): Promise<CapeLibraryEntry[]> {
  const entries: CapeLibraryEntry[] = []
  for (const stored of await readIndex()) {
    try {
      entries.push(toEntry(stored, await readFile(pngPath(stored.id))))
    } catch {
      continue
    }
  }
  return entries
}

export async function saveCapeToLibrary(pngBuffer: Buffer, name: string): Promise<CapeLibraryEntry> {
  validateCapePng(pngBuffer)
  const stored: StoredIndexEntry = { id: randomUUID(), name, createdAt: new Date().toISOString() }
  await mkdir(libraryDir(), { recursive: true })
  await writeFile(pngPath(stored.id), pngBuffer)
  await writeIndex([...(await readIndex()), stored])
  return toEntry(stored, pngBuffer)
}

/** The cape editor's "save" for an entry it was opened on - new PNG and name, same id/createdAt.
 * Doesn't touch the server: if this was the active cape, the old version stays active there until
 * the user activates the edited one. Returns `null` if the id doesn't exist (any more). */
export async function updateCapeInLibrary(id: string, pngBuffer: Buffer, name: string): Promise<CapeLibraryEntry | null> {
  validateCapePng(pngBuffer)
  const index = await readIndex()
  const existing = index.find((entry) => entry.id === id)
  if (!existing) return null
  const updated: StoredIndexEntry = { ...existing, name }
  await writeFile(pngPath(id), pngBuffer)
  await writeIndex(index.map((entry) => (entry.id === id ? updated : entry)))
  return toEntry(updated, pngBuffer)
}

/** Local only - an active cape on the server stays until it's removed or replaced there. */
export async function deleteCapeFromLibrary(id: string): Promise<void> {
  await writeIndex((await readIndex()).filter((entry) => entry.id !== id))
  await rm(pngPath(id), { force: true })
}

export async function readCapeLibraryPng(id: string): Promise<Buffer | null> {
  if (!(await readIndex()).some((entry) => entry.id === id)) return null
  return readFile(pngPath(id))
}
