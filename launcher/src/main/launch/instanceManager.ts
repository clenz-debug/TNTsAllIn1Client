import { rm } from 'node:fs/promises'
import type { LauncherSettings } from '../../shared/types'
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
