import { app } from 'electron'
import { mkdir, readdir, readFile, stat, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { DEFAULT_LAUNCHER_SETTINGS, type Instance, type LauncherSettings } from '../shared/types'

/** Same hand-rolled JSON-in-userData pattern as `auth/tokenCache.ts` rather than pulling in
 * electron-store for two small fields (selected version, snapshot-visibility toggle). */
function settingsPath(): string {
  return join(app.getPath('userData'), 'launcher-settings.json')
}

/** The real, installed Minecraft version inside a legacy instance folder - found by looking for
 * `versions/<x>/<x>.jar` (exactly the path `installer.ts#installVersion` itself downloads the
 * client jar to), not by assuming the folder's own name is the version id. That assumption held
 * for every folder created from Phase 6a onward (named directly after the version, e.g.
 * `instances/1.21.11/`), but not for the one folder that predates even that - `instances/default/`,
 * the single shared instance dir from Phase 3/4 before per-version folders existed at all, whose
 * name is literally the word "default", not a version id. Migrating it with `versionId: 'default'`
 * (the first version of this migration did exactly that) makes every launch fail immediately -
 * `default` isn't a real entry in Mojang's version manifest. */
async function findInstalledVersionId(instanceFolder: string): Promise<string | null> {
  const versionsDir = join(instanceFolder, 'versions')
  let entries: string[]
  try {
    entries = await readdir(versionsDir)
  } catch {
    return null
  }

  for (const entry of entries) {
    const hasJar = await stat(join(versionsDir, entry, `${entry}.jar`))
      .then((s) => s.isFile())
      .catch(() => false)
    if (hasJar) return entry
  }
  return null
}

/** Pre-instance-system layout (Phase 3-6e): a folder per version (or, before that, a single
 * `default` folder) directly under `instances/`. Turns any such folder that actually has an
 * installed game version in it into a real {@link Instance} pointing at that exact same folder -
 * the folder's own name becomes the migrated instance's id (whatever it happened to be called),
 * its *real* version comes from {@link findInstalledVersionId} instead of being assumed from the
 * folder name - nothing has to be moved or re-downloaded either way. Folders with nothing
 * installed in them (no version jar found) are ignored rather than migrated into a broken instance. */
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
    const versionId = await findInstalledVersionId(join(instancesRoot, entry))
    if (versionId) {
      migrated.push({ id: entry, name: `Migriert (${entry})`, versionId, enabledBundledMods: legacyEnabledBundledMods })
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
