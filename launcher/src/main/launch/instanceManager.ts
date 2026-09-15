import { randomUUID } from 'node:crypto'
import { cp, rm } from 'node:fs/promises'
import type { Instance, LauncherSettings } from '../../shared/types'
import { loadLauncherSettings, saveLauncherSettings } from '../launcherSettings'
import { instanceDir } from './installer'

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
    throw new Error(`Instanz ${instanceId} nicht gefunden.`)
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
