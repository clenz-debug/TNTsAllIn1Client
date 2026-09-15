import type { BrowserWindow } from 'electron'
import { dialog } from 'electron'
import { copyFile, mkdir, readdir, stat } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import type { ClientImportResult } from '../../shared/types'
import { instanceDir } from './installer'
import { listBundledMods } from './modsManager'
import { sharedOptionsPath } from './sharedSettings'

/**
 * "Einstellungen/Mods von einem anderen, bereits installierten Client übernehmen" (own user
 * request). Deliberately a free folder picker rather than hardcoded paths for specific known
 * clients (the official launcher, Lunar, Badlion, ...) - their exact install locations/folder
 * layouts aren't something to guess at; a folder dialog works for any client that follows the
 * normal `options.txt` + `mods/*.jar` layout, regardless of name or install location.
 */
export async function pickExternalClientFolder(parentWindow: BrowserWindow | null): Promise<string | null> {
  const dialogOptions: Electron.OpenDialogOptions = {
    title: 'Minecraft-Ordner des anderen Clients auswählen',
    properties: ['openDirectory']
  }
  const result = parentWindow
    ? await dialog.showOpenDialog(parentWindow, dialogOptions)
    : await dialog.showOpenDialog(dialogOptions)
  if (result.canceled || result.filePaths.length === 0) return null
  return result.filePaths[0]
}

async function fileExists(path: string): Promise<boolean> {
  return stat(path)
    .then(() => true)
    .catch(() => false)
}

/**
 * Copies `options.txt` and non-bundled mod jars from an external client's folder into a brand-new
 * instance. Two things worth knowing:
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
export async function importFromExternalClient(sourceFolder: string, instanceId: string, versionId: string): Promise<ClientImportResult> {
  const gameDir = join(instanceDir(instanceId), 'game')
  await mkdir(gameDir, { recursive: true })

  let importedOptions = false
  const sourceOptionsPath = join(sourceFolder, 'options.txt')
  if (await fileExists(sourceOptionsPath)) {
    await mkdir(dirname(sharedOptionsPath()), { recursive: true })
    await copyFile(sourceOptionsPath, sharedOptionsPath())
    importedOptions = true
  }

  const copiedMods: string[] = []
  const sourceModsDir = join(sourceFolder, 'mods')
  if (await fileExists(sourceModsDir)) {
    const bundled = new Set(await listBundledMods(versionId))
    const entries = (await readdir(sourceModsDir)).filter((name) => name.endsWith('.jar') && !bundled.has(name))
    const targetModsDir = join(gameDir, 'mods')
    await mkdir(targetModsDir, { recursive: true })
    await Promise.all(entries.map((name) => copyFile(join(sourceModsDir, name), join(targetModsDir, name))))
    copiedMods.push(...entries)
  }

  return { importedOptions, copiedMods }
}
