import type { BrowserWindow } from 'electron'
import { dialog } from 'electron'
import { copyFile, cp, mkdir, readdir, readFile, stat } from 'node:fs/promises'
import { basename, dirname, join, sep } from 'node:path'
import { localizedError } from '../../shared/errorMessages'
import type { ClientImportResult, ExternalClientFolder } from '../../shared/types'
import { instanceDir } from './installer'
import { bundledModsDir } from './resourcePaths'
import { listBundledMods, readFabricModId } from './modsManager'
import { importWorldsFromSavesFolder } from './worldImport'
import { sharedOptionsPath } from './sharedSettings'

/**
 * "Einstellungen/Mods von einem anderen, bereits installierten Client übernehmen" (own user
 * request). Deliberately a free folder picker rather than hardcoded paths for specific known
 * clients (the official launcher, Lunar, Badlion, ...) - their exact install locations/folder
 * layouts aren't something to guess at; a folder dialog works for any client that follows the
 * normal `options.txt` + `mods/*.jar` layout, regardless of name or install location.
 */
export async function pickExternalClientFolder(parentWindow: BrowserWindow | null): Promise<ExternalClientFolder | null> {
  const dialogOptions: Electron.OpenDialogOptions = {
    title: 'Minecraft-Ordner des anderen Clients auswählen',
    properties: ['openDirectory']
  }
  const result = parentWindow
    ? await dialog.showOpenDialog(parentWindow, dialogOptions)
    : await dialog.showOpenDialog(dialogOptions)
  if (result.canceled || result.filePaths.length === 0) return null
  const folder = result.filePaths[0]
  return { folder, suggestedName: await suggestInstanceName(folder) }
}

/** Folder names that say nothing about which profile they belong to. */
const GENERIC_FOLDER_NAMES = new Set(['.minecraft', 'minecraft', 'game'])

/** The profile's display name from a client's own metadata file next to its game folder:
 * Feather's `profile.json` (`profile.name`) or Prism/MultiMC's `instance.cfg` (`name=`). */
async function readProfileName(folder: string): Promise<string | null> {
  try {
    const json = JSON.parse(await readFile(join(folder, 'profile.json'), 'utf8')) as { profile?: { name?: unknown } }
    const name = json.profile?.name
    if (typeof name === 'string' && name.trim()) return name.trim()
  } catch {
    // No Feather profile here.
  }
  try {
    const match = /^name=(.+)$/m.exec(await readFile(join(folder, 'instance.cfg'), 'utf8'))
    if (match && match[1].trim()) return match[1].trim()
  } catch {
    // No Prism/MultiMC instance here.
  }
  return null
}

/**
 * Name for the new instance (user report: the other client's profile name wasn't taken over).
 * Checks the picked folder and its parent - the metadata sits next to `.minecraft`, whichever of
 * the two was picked - then falls back to the folder's own name unless that's a generic one.
 */
async function suggestInstanceName(pickedFolder: string): Promise<string | null> {
  for (const folder of [pickedFolder, dirname(pickedFolder)]) {
    const name = await readProfileName(folder)
    if (name) return name
  }
  const folderName = basename(pickedFolder)
  if (GENERIC_FOLDER_NAMES.has(folderName.toLowerCase())) return null
  // Lunar names its profiles after the version ("26", "1.21") - say whose they are.
  return pickedFolder.toLowerCase().includes(`${sep}.lunarclient${sep}`) ? `Lunar Client ${folderName}` : folderName
}

async function fileExists(path: string): Promise<boolean> {
  return stat(path)
    .then(() => true)
    .catch(() => false)
}

/** Where other launchers keep their game files below the folder a user would naturally pick:
 * the folder itself (official launcher's `.minecraft`, Modrinth profiles), `.minecraft` (Feather's
 * `.dawn/profiles/<profile>/`, Prism/MultiMC instances), `minecraft` (older MultiMC) or `game`
 * (this launcher's own instances). User report: picking Feather's profile folder imported nothing. */
const GAME_FOLDER_CANDIDATES = ['', '.minecraft', 'minecraft', 'game']

/** Anything the import takes - a folder holding at least one of these is the game folder. */
const IMPORTED_ENTRIES = ['options.txt', 'mods', 'resourcepacks', 'saves']

async function resolveGameFolder(sourceFolder: string): Promise<string> {
  for (const sub of GAME_FOLDER_CANDIDATES) {
    const candidate = join(sourceFolder, sub)
    for (const entry of IMPORTED_ENTRIES) {
      if (await fileExists(join(candidate, entry))) return candidate
    }
  }
  return sourceFolder
}

/** Other clients' own core mods - they only run inside their own launcher. `dawn` is Feather's
 * profile core, `feather` the one Feather drops into the vanilla `.minecraft/mods`. */
const FOREIGN_CLIENT_MOD_IDS = new Set(['dawn', 'feather'])

/** Where the jars actually sit: directly in `mods`, or - Lunar Client's profiles - sorted by
 * Minecraft version into `mods/fabric-<version>/`, in which case only the new instance's own
 * version is taken. */
async function resolveModsFolder(modsDir: string, versionId: string): Promise<string> {
  const perVersion = join(modsDir, `fabric-${versionId}`)
  return (await fileExists(perVersion)) ? perVersion : modsDir
}

/** Copies every resource pack (zip or folder) that isn't already there - the only files in a
 * brand-new instance's `resourcepacks` are ones synced from our bundle, which win. */
async function importResourcepacks(sourceDir: string, targetDir: string): Promise<string[]> {
  const entries = await readdir(sourceDir, { withFileTypes: true }).catch(() => [])
  const packs = entries.filter((entry) => entry.isDirectory() || entry.name.endsWith('.zip')).map((entry) => entry.name)
  await mkdir(targetDir, { recursive: true })
  const copied: string[] = []
  for (const name of packs) {
    const target = join(targetDir, name)
    if (await fileExists(target)) continue
    await cp(join(sourceDir, name), target, { recursive: true })
    copied.push(name)
  }
  return copied
}

/** Instances an import is still copying into. Copying worlds can take a while, and the new
 * instance is already listed and selectable meanwhile - launching it would load half-copied mod
 * jars or open a half-copied world, deleting/cloning it would race the copy (user report). */
const importingInstances = new Set<string>()

export function assertInstanceNotImporting(instanceId: string): void {
  if (importingInstances.has(instanceId)) {
    throw localizedError('instance.importing')
  }
}

/** Our own mod's id (`mod/<version>/src/main/resources/fabric.mod.json`). */
const OWN_MOD_ID = 'tntsallin1client'

/** Mod ids of this client's bundled jars for `versionId` - matching by id rather than filename,
 * since another client usually ships a different version of e.g. Fabric API under another
 * filename, and two copies of one mod id make Fabric Loader refuse to start. */
async function bundledModIds(versionId: string): Promise<Set<string>> {
  const dir = bundledModsDir(versionId)
  const ids = await Promise.all((await listBundledMods(versionId)).map((name) => readFabricModId(join(dir, name))))
  return new Set(ids.filter((id): id is string => id !== null))
}

/**
 * Copies `options.txt`, non-bundled mod jars, resource packs and worlds from an external client's
 * folder into a brand-new instance (packs and worlds: own user follow-up request). Screenshots,
 * shaders and the server list stay behind. `pickedFolder` may also be one level above the actual
 * game folder (see {@link resolveGameFolder}). Two things worth knowing:
 *
 * - Instances are normally created lazily - `instances/<id>/game/` doesn't exist until the very
 *   first "Play" click (see `installer.ts#installVersion`). An import needs somewhere to put
 *   files *right now*, so this creates the `game/` folder immediately instead of waiting.
 * - `options.txt` goes into the **shared** cache (`sharedOptionsPath()`), not the new instance's
 *   own folder directly - this launcher has shared `options.txt` across every instance since
 *   Phase 6e (`applySharedOptions` unconditionally overwrites each instance's copy from that
 *   shared cache on every single launch), so writing straight into the new instance's `game/`
 *   folder would just get silently overwritten the first time it's played. Confirmed with the
 *   user: importing is meant to affect every instance's shared settings, not just the new one -
 *   the UI carries an explicit warning about this.
 *
 * Cosmetics/capes are never touched (explicit requirement) - they live only in the Mojang
 * account, not in any local folder this function ever looks at.
 *
 * `versionId` is passed in by the caller (`InstancesScreen.tsx` already has it - it's the version
 * just picked for the brand-new instance) rather than resolved here via `loadLauncherSettings()`
 * like `listCustomMods` does: the new instance's own save-effect in `PlayScreen.tsx` runs
 * asynchronously after `onInstancesChange`, so `launcher-settings.json` on disk could still be
 * stale (missing this instance entirely) at the exact moment this function runs.
 */
export async function importFromExternalClient(pickedFolder: string, instanceId: string, versionId: string): Promise<ClientImportResult> {
  importingInstances.add(instanceId)
  try {
    return await copyFromExternalClient(pickedFolder, instanceId, versionId)
  } finally {
    importingInstances.delete(instanceId)
  }
}

async function copyFromExternalClient(pickedFolder: string, instanceId: string, versionId: string): Promise<ClientImportResult> {
  const gameDir = join(instanceDir(instanceId), 'game')
  await mkdir(gameDir, { recursive: true })
  const sourceFolder = await resolveGameFolder(pickedFolder)

  let importedOptions = false
  const sourceOptionsPath = join(sourceFolder, 'options.txt')
  if (await fileExists(sourceOptionsPath)) {
    await mkdir(dirname(sharedOptionsPath()), { recursive: true })
    await copyFile(sourceOptionsPath, sharedOptionsPath())
    importedOptions = true
  }

  const copiedMods: string[] = []
  const skippedMods: string[] = []
  const sourceModsRoot = join(sourceFolder, 'mods')
  if (await fileExists(sourceModsRoot)) {
    const sourceModsDir = await resolveModsFolder(sourceModsRoot, versionId)
    const bundledIds = await bundledModIds(versionId)
    const jars = (await readdir(sourceModsDir)).filter((name) => name.endsWith('.jar'))
    for (const name of jars) {
      const id = await readFabricModId(join(sourceModsDir, name))
      const skip = id === null || id === OWN_MOD_ID || bundledIds.has(id) || FOREIGN_CLIENT_MOD_IDS.has(id)
      ;(skip ? skippedMods : copiedMods).push(name)
    }
    const targetModsDir = join(gameDir, 'mods')
    await mkdir(targetModsDir, { recursive: true })
    await Promise.all(copiedMods.map((name) => copyFile(join(sourceModsDir, name), join(targetModsDir, name))))
  }

  const resourcepacks = await importResourcepacks(join(sourceFolder, 'resourcepacks'), join(gameDir, 'resourcepacks'))
  const sourceSaves = join(sourceFolder, 'saves')
  const worlds = (await fileExists(sourceSaves)) ? await importWorldsFromSavesFolder(instanceId, sourceSaves) : []

  return { importedOptions, copiedMods, skippedMods, resourcepacks, worlds }
}
