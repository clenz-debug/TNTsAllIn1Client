import type { BrowserWindow } from 'electron'
import { app, dialog } from 'electron'
import { createWriteStream, existsSync } from 'node:fs'
import { cp, mkdir, readdir, rm, stat } from 'node:fs/promises'
import { basename, dirname, join, sep } from 'node:path'
import { Transform } from 'node:stream'
import { pipeline } from 'node:stream/promises'
import { crc32 } from 'node:zlib'
import { openPromise } from 'yauzl'
import { localizedError } from '../../shared/errorMessages'
import { resolveFreeWorldName, savesDir } from './instanceManager'

/** Held open (and locked on Windows) by Minecraft while a world is loaded - never worth copying,
 * vanilla recreates it on the next load anyway. */
const SESSION_LOCK = 'session.lock'

/** What makes a folder a world: vanilla's own "is this a save" check is the same file. */
const LEVEL_DAT = 'level.dat'

/** A name that's safe as a Windows folder name - worlds from a zip made on another OS can carry
 * characters Windows doesn't allow. Falls back to "World" if nothing usable is left. */
function sanitizeWorldName(name: string): string {
  const cleaned = name.replace(/[<>:"/\\|?*\u0000-\u001f]/g, '_').replace(/[. ]+$/, '').trim()
  return cleaned || 'World'
}

/** The dialog opens in the vanilla launcher's `saves` folder when there is one - the most likely
 * place someone uploads a world from. */
function defaultSavesPath(): string | undefined {
  const vanillaSaves = join(app.getPath('appData'), '.minecraft', 'saves')
  return existsSync(vanillaSaves) ? vanillaSaves : undefined
}

/** The world folders inside one picked folder: the folder itself if it's a world, otherwise its
 * direct subfolders that are - so picking a whole `saves` folder imports every world in it. */
async function findWorldFolders(folder: string): Promise<string[]> {
  if (existsSync(join(folder, LEVEL_DAT))) return [folder]
  const entries = await readdir(folder, { withFileTypes: true }).catch(() => [])
  return entries
    .filter((entry) => entry.isDirectory() && existsSync(join(folder, entry.name, LEVEL_DAT)))
    .map((entry) => join(folder, entry.name))
}

/** Runs one world's copy/unpack and removes whatever it already wrote if it fails halfway (broken
 * file, disk full) - a half world would otherwise show up in the list and in-game. */
async function withCleanup<T>(destinationPaths: string[], work: () => Promise<T>): Promise<T> {
  try {
    return await work()
  } catch (err) {
    await Promise.all(destinationPaths.map((path) => rm(path, { recursive: true, force: true })))
    throw err
  }
}

async function importWorldFolder(instanceId: string, worldFolder: string): Promise<string> {
  const destinationName = await resolveFreeWorldName(instanceId, sanitizeWorldName(basename(worldFolder)))
  const destinationPath = join(savesDir(instanceId), destinationName)
  await withCleanup([destinationPath], () =>
    cp(worldFolder, destinationPath, { recursive: true, filter: (source) => basename(source) !== SESSION_LOCK })
  )
  return destinationName
}

/** World roots inside a zip (zip-internal paths, `''` for a world directly at the zip's root):
 * every folder holding a `level.dat`, minus any nested inside another world. */
async function findZipWorldRoots(zipPath: string): Promise<string[]> {
  const roots: string[] = []
  const zipfile = await openPromise(zipPath, { lazyEntries: true, autoClose: true })
  try {
    for await (const entry of zipfile.eachEntry()) {
      const name = entry.fileName
      if (name.startsWith('__MACOSX/')) continue
      if (name === LEVEL_DAT || name.endsWith(`/${LEVEL_DAT}`)) {
        roots.push(name.slice(0, name.length - LEVEL_DAT.length))
      }
    }
  } finally {
    zipfile.close()
  }
  return roots.filter((root) => !roots.some((other) => other !== root && root.startsWith(other)))
}

/** Unpacks each world root of a zip into its own free `saves/` folder. Entries resolving outside
 * their destination (`..` in a path) are skipped instead of written. */
async function importZipWorlds(instanceId: string, zipPath: string, roots: string[]): Promise<string[]> {
  const zipName = basename(zipPath).replace(/\.zip$/i, '')
  const destinations = new Map<string, string>()
  for (const root of roots) {
    const rootName = root === '' ? zipName : root.slice(0, -1).split('/').pop() ?? zipName
    const destinationName = await resolveFreeWorldName(instanceId, sanitizeWorldName(rootName))
    const destinationPath = join(savesDir(instanceId), destinationName)
    await mkdir(destinationPath, { recursive: true })
    destinations.set(root, destinationPath)
  }

  await withCleanup([...destinations.values()], () => unpackZipWorlds(zipPath, destinations))
  return [...destinations.values()].map((path) => basename(path))
}

/** Passes data through while checksumming it - yauzl doesn't verify an entry's CRC-32 itself, so
 * a damaged zip would otherwise land as a silently corrupted world. */
function crcCheck(expected: number, fileName: string): Transform {
  let crc = 0
  return new Transform({
    transform(chunk: Buffer, _encoding, callback) {
      crc = crc32(chunk, crc)
      callback(null, chunk)
    },
    flush(callback) {
      callback(crc === expected ? null : new Error(`CRC mismatch in ${fileName}`))
    }
  })
}

async function unpackZipWorlds(zipPath: string, destinations: Map<string, string>): Promise<void> {
  const roots = [...destinations.keys()]
  const zipfile = await openPromise(zipPath, { lazyEntries: true, autoClose: true })
  try {
    for await (const entry of zipfile.eachEntry()) {
      if (entry.fileName.startsWith('__MACOSX/') || entry.fileName.endsWith('/')) continue
      const root = roots.find((candidate) => entry.fileName.startsWith(candidate))
      if (root === undefined) continue
      const relative = entry.fileName.slice(root.length)
      if (basename(relative) === SESSION_LOCK) continue
      const destinationRoot = destinations.get(root)!
      const target = join(destinationRoot, ...relative.split('/'))
      if (!target.startsWith(destinationRoot + sep)) continue
      await mkdir(dirname(target), { recursive: true })
      await pipeline(await zipfile.openReadStreamPromise(entry), crcCheck(entry.crc32, entry.fileName), createWriteStream(target))
    }
  } finally {
    zipfile.close()
  }
}

/**
 * "Upload world" in the Worlds screen (own user request): copies worlds from anywhere on the PC
 * into an instance's `saves/`, either as folders or as zips (Windows' dialog can't offer both at
 * once, hence `kind`). Name collisions get a numbered suffix like copying between instances does.
 * Returns the new world names - empty if the dialog was cancelled; throws `worlds.noWorldFound`
 * if nothing picked contained a world at all.
 */
export async function importWorlds(
  instanceId: string,
  kind: 'folder' | 'zip',
  dialogTitle: string,
  parentWindow: BrowserWindow | null
): Promise<string[]> {
  const dialogOptions: Electron.OpenDialogOptions = {
    title: dialogTitle,
    defaultPath: defaultSavesPath(),
    properties: kind === 'folder' ? ['openDirectory', 'multiSelections'] : ['openFile', 'multiSelections'],
    filters: kind === 'zip' ? [{ name: 'ZIP', extensions: ['zip'] }] : undefined
  }
  const result = parentWindow
    ? await dialog.showOpenDialog(parentWindow, dialogOptions)
    : await dialog.showOpenDialog(dialogOptions)
  if (result.canceled || result.filePaths.length === 0) return []

  await mkdir(savesDir(instanceId), { recursive: true })
  const imported: string[] = []
  for (const picked of result.filePaths) {
    if (kind === 'folder') {
      if (!(await stat(picked)).isDirectory()) continue
      for (const worldFolder of await findWorldFolders(picked)) {
        imported.push(await importWorldFolder(instanceId, worldFolder))
      }
    } else {
      const roots = await findZipWorldRoots(picked).catch(() => {
        throw localizedError('worlds.zipUnreadable', { file: basename(picked) })
      })
      if (roots.length > 0) {
        const worlds = await importZipWorlds(instanceId, picked, roots).catch((err: unknown) => {
          throw (err as NodeJS.ErrnoException).code ? err : localizedError('worlds.zipUnreadable', { file: basename(picked) })
        })
        imported.push(...worlds)
      }
    }
  }
  if (imported.length === 0) throw localizedError('worlds.noWorldFound')
  return imported
}
