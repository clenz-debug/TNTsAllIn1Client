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

async function listJarsIn(dir: string): Promise<string[]> {
  try {
    return (await readdir(dir)).filter((name) => name.endsWith('.jar'))
  } catch {
    return []
  }
}

/** The third-party jars in `launcher/mods-bundle/` (Sodium, Lithium, ...) - the Mods screen's
 * per-mod enable/disable toggles (Phase 6c) operate on this list, read fresh every time rather
 * than cached, since it only changes when someone edits the (dev-populated, git-ignored) folder
 * itself. */
export async function listBundledMods(): Promise<string[]> {
  return listJarsIn(join(app.getAppPath(), 'mods-bundle'))
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
