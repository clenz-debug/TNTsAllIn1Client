import { randomUUID } from 'node:crypto'
import { cp, mkdir, readdir, readFile, rename, rm } from 'node:fs/promises'
import { join } from 'node:path'
import { localizedError } from '../../shared/errorMessages'
import type { Instance, LauncherSettings } from '../../shared/types'
import { loadLauncherSettings, saveLauncherSettings } from '../launcherSettings'
import { instanceDir } from './installer'

function savesDir(instanceId: string): string {
  return join(instanceDir(instanceId), 'game', 'saves')
}

/**
 * Deletes an instance's settings entry *and* its on-disk game directory (saves, options, mods,
 * everything) - unlike every other mutation on `LauncherSettings` (create/rename/toggle mods),
 * which the renderer does itself by editing its own copy and calling the existing generic
 * `settings:save` IPC channel, deleting needs a dedicated main-process round trip because only the
 * main process can touch the filesystem (renderer runs with `contextIsolation`/`sandbox: true`).
 * Bundling the settings-array update and the directory removal into one function here keeps both
 * always in sync - there's no window where a renderer-side settings save and a separate delete
 * call could race or disagree about whether the instance still exists.
 *
 * The caller (the renderer's Instances screen) is expected to have already confirmed this with the
 * user - this function itself does not ask again, same as `WaypointListScreen#confirmDeleteAll`
 * always asking before calling into its own equivalent bulk-delete on the mod side.
 */
export async function deleteInstance(instanceId: string): Promise<LauncherSettings> {
  const settings = await loadLauncherSettings()
  const remaining = settings.instances.filter((instance) => instance.id !== instanceId)
  const updated: LauncherSettings = {
    ...settings,
    instances: remaining,
    selectedInstanceId: settings.selectedInstanceId === instanceId ? (remaining[0]?.id ?? null) : settings.selectedInstanceId
  }
  await saveLauncherSettings(updated)
  await rm(instanceDir(instanceId), { recursive: true, force: true })
  return updated
}

/**
 * Duplicates an instance's settings entry *and* its complete on-disk game directory (saves,
 * options, mods, resourcepacks, everything) - own user request, so trying something risky on a
 * copy (a new mod, a config change) never has to touch the original. Same "settings + filesystem
 * in one main-process round trip" reasoning as {@link deleteInstance}.
 *
 * The source instance's `game/` folder may not exist yet (an instance only gets one lazily, on its
 * first "Play" click via `installVersion()`) - `cp`'s `force: true` alone doesn't cover a missing
 * *source*, so a missing source directory is treated as "nothing to copy yet", not an error.
 */
export async function cloneInstance(instanceId: string, newName: string): Promise<LauncherSettings> {
  const settings = await loadLauncherSettings()
  const source = settings.instances.find((instance) => instance.id === instanceId)
  if (!source) {
    throw localizedError('instance.notFound', { instanceId })
  }

  const clone: Instance = { ...source, id: randomUUID(), name: newName }
  try {
    await cp(instanceDir(instanceId), instanceDir(clone.id), { recursive: true })
  } catch (err) {
    if ((err as NodeJS.ErrnoException).code !== 'ENOENT') throw err
  }

  const updated: LauncherSettings = {
    ...settings,
    instances: [...settings.instances, clone],
    selectedInstanceId: clone.id
  }
  await saveLauncherSettings(updated)
  return updated
}

/** World folder names in an instance's `game/saves/` right now (standard vanilla layout: one
 * subdirectory per world) - a missing `saves/` (instance never played, or never created a world)
 * is not an error, just an empty list, same convention as `modsManager.ts#listJarsIn`. */
export async function listInstanceWorlds(instanceId: string): Promise<string[]> {
  try {
    const entries = await readdir(savesDir(instanceId), { withFileTypes: true })
    return entries.filter((entry) => entry.isDirectory()).map((entry) => entry.name)
  } catch {
    return []
  }
}

/** A world's `icon.png` (the thumbnail vanilla's own "Select World" screen shows for it) as a data
 * URI - same base64-encoding approach as `skinApi.ts#fetchTextureDataUri`, needed for the same CSP
 * reason (`img-src 'self' data:'` in the renderer - a plain `file://` src wouldn't be allowed even
 * if Electron's sandboxed renderer could resolve one). Not every world has one (very old saves, or
 * one that hasn't been opened in-game yet since creation) - missing is `null`, not an error, same
 * "best effort, never fail the whole screen over one missing picture" convention already used for
 * Modrinth search result icons in `modrinthApi.ts#searchModrinthMods`. */
export async function getWorldIcon(instanceId: string, worldName: string): Promise<string | null> {
  try {
    const buffer = await readFile(join(savesDir(instanceId), worldName, 'icon.png'))
    return `data:image/png;base64,${buffer.toString('base64')}`
  } catch {
    return null
  }
}

/** A `worldName` guaranteed free in `targetInstanceId`'s `saves/` - a numbered suffix rather than
 * overwriting or failing on a collision, same "just make it work, resolve the name after"
 * convention as {@link cloneInstance}'s "(Kopie)" suffix. Shared by
 * {@link moveWorldBetweenInstances} and {@link copyWorldBetweenInstances} so a name collision is
 * resolved identically either way. */
async function resolveFreeWorldName(targetInstanceId: string, worldName: string): Promise<string> {
  const existing = new Set(await listInstanceWorlds(targetInstanceId))
  let candidate = worldName
  for (let suffix = 2; existing.has(candidate); suffix++) {
    candidate = `${worldName} (${suffix})`
  }
  return candidate
}

/**
 * Moves (not copies) one world folder from one instance's `saves/` into another's - own user
 * request. Deliberately a real move, not a copy: the whole point of the warning the renderer shows
 * before calling this is "this can't be undone", which wouldn't be true if the original stuck
 * around. No version/mod-compatibility check happens here on purpose - detecting whether a world
 * will actually still work (different Minecraft version's chunk format, mod blocks/items the
 * target instance doesn't have) is out of scope, same "own risk" framing as the confirmation
 * dialog itself; this only ever moves files.
 *
 * `rename` is used over `cp`+`rm` for the common case (both instances live under the same data
 * root, see `installer.ts#instanceDir`), with a copy+delete fallback only for the unlikely case a
 * symlinked/relocated instance folder puts them on different filesystems (`rename` fails with
 * `EXDEV` there).
 */
export async function moveWorldBetweenInstances(
  sourceInstanceId: string,
  worldName: string,
  targetInstanceId: string
): Promise<{ movedTo: string }> {
  const sourcePath = join(savesDir(sourceInstanceId), worldName)
  const targetDir = savesDir(targetInstanceId)
  await mkdir(targetDir, { recursive: true })

  const destinationName = await resolveFreeWorldName(targetInstanceId, worldName)
  const destinationPath = join(targetDir, destinationName)

  try {
    await rename(sourcePath, destinationPath)
  } catch (err) {
    if ((err as NodeJS.ErrnoException).code !== 'EXDEV') throw err
    await cp(sourcePath, destinationPath, { recursive: true })
    await rm(sourcePath, { recursive: true, force: true })
  }

  return { movedTo: destinationName }
}

/**
 * Copies (leaves the original untouched) one world folder from one instance's `saves/` into
 * another's - own user request, follow-up to {@link moveWorldBetweenInstances}: sometimes you want
 * to try a world under a different version/mod set without giving up the original if it breaks.
 * Same no-compatibility-check reasoning as the move above - this only ever copies files - but
 * unlike the move, nothing is lost if the copy doesn't work out (just delete it again), which is
 * exactly why the renderer doesn't show the destructive "can't be undone" warning for this one.
 */
export async function copyWorldBetweenInstances(
  sourceInstanceId: string,
  worldName: string,
  targetInstanceId: string
): Promise<{ copiedTo: string }> {
  const sourcePath = join(savesDir(sourceInstanceId), worldName)
  const targetDir = savesDir(targetInstanceId)
  await mkdir(targetDir, { recursive: true })

  const destinationName = await resolveFreeWorldName(targetInstanceId, worldName)
  await cp(sourcePath, join(targetDir, destinationName), { recursive: true })

  return { copiedTo: destinationName }
}
