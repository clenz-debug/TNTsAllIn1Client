import { app } from 'electron'
import { mkdir, readdir, readFile, stat, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { DEFAULT_LAUNCHER_SETTINGS, type Instance, type LauncherSettings } from '../shared/types'

/** Same hand-rolled JSON-in-userData pattern as `auth/tokenCache.ts` rather than pulling in
 * electron-store for two small fields (selected version, snapshot-visibility toggle). */
function settingsPath(): string {
  return join(app.getPath('userData'), 'launcher-settings.json')
}

/** Pre-instance-system layout (Phase 6a-6e): one folder per version directly under `instances/`,
 * e.g. `instances/1.21.11/`. Turns any such folder that was actually launched at least once
 * (has a `game` subfolder) into a real {@link Instance} pointing at that exact same folder - the
 * folder's name doubles as the migrated instance's id, so nothing has to be moved or re-downloaded.
 * Folders that were only ever partially created (no `game` dir yet) are ignored rather than
 * migrated into a half-broken instance. */
async function migrateLegacyInstances(legacyEnabledBundledMods: string[]): Promise<Instance[]> {
  const instancesRoot = join(app.getPath('userData'), 'instances')
  let entries: string[]
  try {
    entries = await readdir(instancesRoot)
  } catch {
    return []
  }

  const migrated: Instance[] = []
  for (const entry of entries) {
    const hasGameDir = await stat(join(instancesRoot, entry, 'game'))
      .then((s) => s.isDirectory())
      .catch(() => false)
    if (hasGameDir) {
      migrated.push({ id: entry, name: `Migriert (${entry})`, versionId: entry, enabledBundledMods: legacyEnabledBundledMods })
    }
  }
  return migrated
}

export async function loadLauncherSettings(): Promise<LauncherSettings> {
  try {
    const raw = await readFile(settingsPath(), 'utf-8')
    const parsed = JSON.parse(raw) as Partial<LauncherSettings> & {
      selectedVersion?: string
      enabledBundledMods?: string[]
    }

    if (parsed.instances === undefined) {
      // Old-shape file (or one written by a build that predates the instance system) - migrate
      // once instead of silently losing the old enabledBundledMods setting and orphaning any
      // already-downloaded per-version folder (would otherwise mean re-downloading everything).
      const migrated = await migrateLegacyInstances(parsed.enabledBundledMods ?? [])
      const settings: LauncherSettings = {
        showSnapshots: parsed.showSnapshots ?? DEFAULT_LAUNCHER_SETTINGS.showSnapshots,
        instances: migrated,
        selectedInstanceId: migrated[0]?.id ?? null
      }
      await saveLauncherSettings(settings)
      return settings
    }

    return { ...DEFAULT_LAUNCHER_SETTINGS, ...parsed }
  } catch {
    return DEFAULT_LAUNCHER_SETTINGS
  }
}

export async function saveLauncherSettings(settings: LauncherSettings): Promise<void> {
  await mkdir(app.getPath('userData'), { recursive: true })
  await writeFile(settingsPath(), JSON.stringify(settings, null, 2), 'utf-8')
}
