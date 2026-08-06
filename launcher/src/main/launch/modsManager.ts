import type { BrowserWindow } from 'electron'
import { app, dialog } from 'electron'
import { basename, join } from 'node:path'
import { copyFile, mkdir, readdir, rm } from 'node:fs/promises'
import { instanceDir } from './installer'

/** Matches `rootProject.name` in `mod/settings.gradle.kts` - every jar `bundleSync.syncOwnModJar`
 * produces starts with this, regardless of version (`tntsallin1client-0.1.0.jar` etc.). Used to
 * exclude our own mod from the "custom mods" list below - it's not something the user adds or
 * removes here, it's what makes this "our" client to begin with. */
const OWN_MOD_PREFIX = 'tntsallin1client-'

/**
 * Bundled jar filename prefixes that are never optional via the Mods screen's toggle - always
 * synced regardless of `enabledBundledMods`, for two different reasons:
 *  - `fabric-api-`: a hard dependency every other bundled mod declares in its own
 *    `fabric.mod.json`. If it were off while e.g. Sodium was on, Fabric Loader would reject the
 *    whole launch on Sodium's unmet dependency instead of degrading gracefully.
 *  - `sodium-fabric-`/`lithium-fabric-`: explicit user request. `enabledBundledMods` itself
 *    defaults to nothing enabled specifically so a first launch doesn't silently carry visible
 *    behavior changes the user never asked for - but Sodium/Lithium are pure performance, no
 *    visible behavior change, and losing "good performance even on weak hardware" by default
 *    would work against this project's whole point (see Projekt_Roadmap.md's stated goal).
 */
const ALWAYS_ENABLED_PREFIXES = ['fabric-api-', 'sodium-fabric-', 'lithium-fabric-']

export function isAlwaysEnabledBundledMod(fileName: string): boolean {
  return ALWAYS_ENABLED_PREFIXES.some((prefix) => fileName.startsWith(prefix))
}

async function listJarsIn(dir: string): Promise<string[]> {
  try {
    return (await readdir(dir)).filter((name) => name.endsWith('.jar'))
  } catch {
    return []
  }
}

/** Every third-party jar in `launcher/mods-bundle/`, Fabric API included - the full-fidelity list
 * used to recognize "this filename is a bundled mod" (see `listCustomMods` below and
 * `bundleSync.ts`), read fresh every time rather than cached, since it only changes when someone
 * edits the (dev-populated, git-ignored) folder itself. Not what the Mods screen shows as
 * toggleable - see {@link listToggleableBundledMods} for that. */
export async function listBundledMods(): Promise<string[]> {
  return listJarsIn(join(app.getAppPath(), 'mods-bundle'))
}

/** The subset of {@link listBundledMods} the Mods screen actually offers a checkbox for - the
 * always-enabled ones are deliberately left out, see {@link isAlwaysEnabledBundledMod}. */
export async function listToggleableBundledMods(): Promise<string[]> {
  const all = await listBundledMods()
  return all.filter((name) => !isAlwaysEnabledBundledMod(name))
}

/** Whatever's sitting in a version's `game/mods` folder that isn't a bundled mod and isn't our
 * own jar - i.e. mods the user added themselves via `addCustomMods`. Tied to a specific version
 * because a Fabric mod jar only ever targets one Minecraft version, same reasoning `instanceDir`
 * itself is per-version since Phase 6a. */
export async function listCustomMods(versionId: string): Promise<string[]> {
  const modsDir = join(instanceDir(versionId), 'game', 'mods')
  const bundled = new Set(await listBundledMods())
  return (await listJarsIn(modsDir)).filter((name) => !bundled.has(name) && !name.startsWith(OWN_MOD_PREFIX))
}

/**
 * Opens a native "choose file(s)" dialog (not literal drag & drop - see Aktuelle_Phase.md for why)
 * scoped to `.jar` files, copies whatever was picked into the given version's `game/mods`, and
 * returns the refreshed custom-mods list. A cancelled dialog is not an error, just returns the
 * unchanged list.
 */
export async function addCustomMods(versionId: string, parentWindow: BrowserWindow | null): Promise<string[]> {
  const dialogOptions: Electron.OpenDialogOptions = {
    title: 'Fabric-Mod-Jar(s) auswählen',
    properties: ['openFile', 'multiSelections'],
    filters: [{ name: 'Fabric Mod Jar', extensions: ['jar'] }]
  }
  const result = parentWindow
    ? await dialog.showOpenDialog(parentWindow, dialogOptions)
    : await dialog.showOpenDialog(dialogOptions)
  if (!result.canceled && result.filePaths.length > 0) {
    const modsDir = join(instanceDir(versionId), 'game', 'mods')
    await mkdir(modsDir, { recursive: true })
    await Promise.all(result.filePaths.map((filePath) => copyFile(filePath, join(modsDir, basename(filePath)))))
  }
  return listCustomMods(versionId)
}

export async function removeCustomMod(versionId: string, fileName: string): Promise<string[]> {
  await rm(join(instanceDir(versionId), 'game', 'mods', fileName), { force: true })
  return listCustomMods(versionId)
}
