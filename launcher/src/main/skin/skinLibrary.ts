import { randomUUID } from 'node:crypto'
import { mkdir, readFile, rm, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { app } from 'electron'
import type { SkinLibraryEntry, SkinVariant } from '../../shared/types'

/**
 * The local skin library (Phase 7 step 3) - Mojang's own account API has no concept of "my saved
 * skins", only ever one active skin, so keeping several around to switch between (the user's own
 * request, modeled on how skinmc.net/Skindex let you manage a personal skin list) has to live
 * entirely on this side. Its own folder, sibling to `auth.json`/`launcher-settings.json` -
 * deliberately **not** part of `dataRoot()`'s movable game-data tree (`RELOCATABLE_SUBDIRS`),
 * which is for the large, instance-/version-bound install data; skins are tiny, account-scoped
 * personal data, closer in kind to the cached auth profile than to a Minecraft install.
 */
function libraryDir(): string {
  return join(app.getPath('userData'), 'skins')
}

function indexPath(): string {
  return join(libraryDir(), 'skins.json')
}

function pngPath(id: string): string {
  return join(libraryDir(), `${id}.png`)
}

interface StoredIndexEntry {
  id: string
  name: string
  variant: SkinVariant
  createdAt: string
}

async function readIndex(): Promise<StoredIndexEntry[]> {
  try {
    const raw = await readFile(indexPath(), 'utf-8')
    return JSON.parse(raw) as StoredIndexEntry[]
  } catch {
    return []
  }
}

async function writeIndex(entries: StoredIndexEntry[]): Promise<void> {
  await mkdir(libraryDir(), { recursive: true })
  await writeFile(indexPath(), JSON.stringify(entries, null, 2), 'utf-8')
}

function toDataUri(buffer: Buffer): string {
  return `data:image/png;base64,${buffer.toString('base64')}`
}

/** Lists every library entry with its PNG inlined as a `data:` URI - cheap enough (a few KB each)
 * to avoid a second IPC round-trip per thumbnail in the Skins screen. An index row whose PNG file
 * is missing (manually deleted, or a prior write got interrupted) is silently dropped rather than
 * surfaced as a broken thumbnail - it never gets written back out by the next save/delete either,
 * so the stale row cleans itself up. */
export async function listSkinLibrary(): Promise<SkinLibraryEntry[]> {
  const index = await readIndex()
  const entries: SkinLibraryEntry[] = []
  for (const stored of index) {
    try {
      const buffer = await readFile(pngPath(stored.id))
      entries.push({ ...stored, dataUri: toDataUri(buffer) })
    } catch {
      continue
    }
  }
  return entries
}

/** The pixel editor's "in Bibliothek speichern" action - a new entry when `existingId` is
 * omitted, or an in-place overwrite (same id, same `createdAt`, new PNG/name/variant) when the
 * editor was opened via an existing entry's "Bearbeiten" button. Does **not** touch the user's
 * actual Mojang account skin - that only happens via {@link readSkinLibraryEntryForUpload} +
 * `uploadSkinBuffer`, triggered by the separate "Verwenden" action. */
export async function saveSkinToLibrary(
  pngBuffer: Buffer,
  variant: SkinVariant,
  name: string,
  existingId?: string
): Promise<SkinLibraryEntry> {
  const index = await readIndex()
  const id = existingId ?? randomUUID()
  const createdAt = existingId ? (index.find((entry) => entry.id === existingId)?.createdAt ?? new Date().toISOString()) : new Date().toISOString()

  await mkdir(libraryDir(), { recursive: true })
  await writeFile(pngPath(id), pngBuffer)

  const stored: StoredIndexEntry = { id, name, variant, createdAt }
  await writeIndex([...index.filter((entry) => entry.id !== id), stored])
  return { ...stored, dataUri: toDataUri(pngBuffer) }
}

/** Removes a library entry - purely local bookkeeping, there is no Mojang-side "delete this skin"
 * call to make (nor a need for one: deleting a locally saved copy doesn't change whatever is
 * currently the account's active skin). */
export async function deleteSkinFromLibrary(id: string): Promise<void> {
  const index = await readIndex()
  await writeIndex(index.filter((entry) => entry.id !== id))
  await rm(pngPath(id), { force: true })
}

/** Single entry lookup (with its PNG as a `data:` URI) for re-opening a library skin in the
 * editor's "Bearbeiten" flow. */
export async function getSkinLibraryEntry(id: string): Promise<SkinLibraryEntry | null> {
  const stored = (await readIndex()).find((entry) => entry.id === id)
  if (!stored) return null
  try {
    const buffer = await readFile(pngPath(id))
    return { ...stored, dataUri: toDataUri(buffer) }
  } catch {
    return null
  }
}

/** Renames a library entry in place, without touching its PNG - used right after a direct upload
 * (own user request: name it *after* picking/uploading the file, not before) and available for
 * any other future "just rename it" need. Returns `null` if the id doesn't exist. */
export async function renameSkinInLibrary(id: string, name: string): Promise<SkinLibraryEntry | null> {
  const index = await readIndex()
  const existing = index.find((entry) => entry.id === id)
  if (!existing) return null
  const updated: StoredIndexEntry = { ...existing, name }
  await writeIndex([...index.filter((entry) => entry.id !== id), updated])
  const buffer = await readFile(pngPath(id))
  return { ...updated, dataUri: toDataUri(buffer) }
}

/** Raw bytes + variant for the "Verwenden" action - reads the PNG directly rather than going
 * through a `data:` URI round-trip, since this feeds straight into `uploadSkinBuffer`. */
export async function readSkinLibraryEntryForUpload(id: string): Promise<{ buffer: Buffer; variant: SkinVariant } | null> {
  const stored = (await readIndex()).find((entry) => entry.id === id)
  if (!stored) return null
  const buffer = await readFile(pngPath(id))
  return { buffer, variant: stored.variant }
}
