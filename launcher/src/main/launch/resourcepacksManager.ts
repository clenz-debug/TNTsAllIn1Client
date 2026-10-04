import type { BrowserWindow } from 'electron'
import { dialog } from 'electron'
import { basename, join } from 'node:path'
import { copyFile, mkdir, readdir, readFile, rm, stat } from 'node:fs/promises'
import type { Readable } from 'node:stream'
import { openPromise } from 'yauzl'
import type { ResourcepackEntry } from '../../shared/types'
import { loadLauncherSettings } from '../launcherSettings'
import { instanceDir } from './installer'
import { LEGACY_DARK_MODE_PACK_NAME } from './legacyDarkModePack'
import { NEW_TEXTURES_PACK_NAME } from './newTexturesPack'
import { bundledResourcepacksDir } from './resourcePaths'

function resourcepacksDir(instanceId: string): string {
  return join(instanceDir(instanceId), 'game', 'resourcepacks')
}

/**
 * The client's own packs in a legacy instance (before 1.14), which has no version bundle: the two the
 * launcher builds on launch (`newTexturesPack.ts`, `legacyDarkModePack.ts`) and the two our mod puts
 * there from its own jar when the game starts (`Blocks3d.java` in `mod/1.8.9` - keep the names in
 * step). The mod's switches for new textures, dark mode and 3D block models need them.
 */
const LEGACY_CLIENT_PACK_NAMES = [
  NEW_TEXTURES_PACK_NAME,
  LEGACY_DARK_MODE_PACK_NAME,
  'TNT 3D Blocks.zip',
  'TNT 3D Blocks - New Textures.zip'
]

/** The packs that belong to the client itself: the filenames in this instance's version bundle
 * (`resourcepacks-bundle/<versionId>/`), synced in on every launch (`bundleSync.ts`) and pinned by
 * our mod, and the legacy instances' own ({@link LEGACY_CLIENT_PACK_NAMES}). They're never listed
 * or removable here (own user request for the legacy ones: removing a pack the client's features
 * need would be counterproductive) - a removal would only last until the next launch anyway. */
async function bundledNames(instanceId: string): Promise<Set<string>> {
  const settings = await loadLauncherSettings()
  const instance = settings.instances.find((candidate) => candidate.id === instanceId)
  if (!instance) return new Set(LEGACY_CLIENT_PACK_NAMES)
  const bundled = await readdir(bundledResourcepacksDir(instance.versionId)).catch(() => [] as string[])
  return new Set([...bundled, ...LEGACY_CLIENT_PACK_NAMES])
}

async function streamToBuffer(stream: Readable): Promise<Buffer> {
  const chunks: Buffer[] = []
  for await (const chunk of stream) {
    chunks.push(chunk as Buffer)
  }
  return Buffer.concat(chunks)
}

/** A pack's own `pack.png` (root of the zip or folder) as a data: URI, or `null` if it has none or
 * can't be read - the list shows a placeholder then, same as `WorldsScreen` for worlds without an icon. */
async function readPackIcon(packPath: string, isFolder: boolean): Promise<string | null> {
  try {
    if (isFolder) {
      return `data:image/png;base64,${(await readFile(join(packPath, 'pack.png'))).toString('base64')}`
    }
    const zipfile = await openPromise(packPath, { lazyEntries: true, autoClose: true })
    try {
      for await (const entry of zipfile.eachEntry()) {
        if (entry.fileName === 'pack.png') {
          const buffer = await streamToBuffer(await zipfile.openReadStreamPromise(entry))
          return `data:image/png;base64,${buffer.toString('base64')}`
        }
      }
    } finally {
      zipfile.close()
    }
  } catch {
    // Missing/unreadable icon or a broken zip - not worth an error for a list entry.
  }
  return null
}

/**
 * Every resource pack in an instance's `game/resourcepacks` the user added themselves (via
 * {@link addResourcepacks}, by hand, or copied along with a cloned instance) - zips and unpacked
 * folder packs alike, bundled packs excluded (see {@link bundledNames}).
 */
export async function listResourcepacks(instanceId: string): Promise<ResourcepackEntry[]> {
  const dir = resourcepacksDir(instanceId)
  const bundled = await bundledNames(instanceId)
  const names = (await readdir(dir).catch(() => [] as string[])).filter((name) => !bundled.has(name)).sort((a, b) => a.localeCompare(b))

  const entries = await Promise.all(
    names.map(async (name): Promise<ResourcepackEntry | null> => {
      const packPath = join(dir, name)
      const isFolder = (await stat(packPath)).isDirectory()
      if (!isFolder && !name.toLowerCase().endsWith('.zip')) return null
      return { name, iconDataUri: await readPackIcon(packPath, isFolder) }
    })
  )
  return entries.filter((entry): entry is ResourcepackEntry => entry !== null)
}

/** Native "choose file(s)" dialog for `.zip` packs (same approach as `modsManager.ts#addCustomMods`),
 * copies the picked files into the instance's `game/resourcepacks`. A cancelled dialog just returns
 * the unchanged list. `dialogTitle` comes from the renderer so it follows the language setting. */
export async function addResourcepacks(
  instanceId: string,
  dialogTitle: string,
  parentWindow: BrowserWindow | null
): Promise<ResourcepackEntry[]> {
  const dialogOptions: Electron.OpenDialogOptions = {
    title: dialogTitle,
    properties: ['openFile', 'multiSelections'],
    filters: [{ name: 'Resource Pack', extensions: ['zip'] }]
  }
  const result = parentWindow
    ? await dialog.showOpenDialog(parentWindow, dialogOptions)
    : await dialog.showOpenDialog(dialogOptions)
  if (!result.canceled && result.filePaths.length > 0) {
    const dir = resourcepacksDir(instanceId)
    await mkdir(dir, { recursive: true })
    await Promise.all(result.filePaths.map((filePath) => copyFile(filePath, join(dir, basename(filePath)))))
  }
  return listResourcepacks(instanceId)
}

/** Deletes one user pack (zip or folder). Only names {@link listResourcepacks} actually returns are
 * accepted - keeps bundled packs and anything outside the folder (`..` in a name) out of reach. */
export async function removeResourcepack(instanceId: string, name: string): Promise<ResourcepackEntry[]> {
  const current = await listResourcepacks(instanceId)
  if (current.some((entry) => entry.name === name)) {
    await rm(join(resourcepacksDir(instanceId), name), { recursive: true, force: true })
  }
  return listResourcepacks(instanceId)
}

/** Deletes every user pack in the instance, bundled packs stay. */
export async function removeAllResourcepacks(instanceId: string): Promise<ResourcepackEntry[]> {
  const dir = resourcepacksDir(instanceId)
  const current = await listResourcepacks(instanceId)
  await Promise.all(current.map((entry) => rm(join(dir, entry.name), { recursive: true, force: true })))
  return listResourcepacks(instanceId)
}
