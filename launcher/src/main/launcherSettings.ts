import { app } from 'electron'
import { mkdir, readdir, readFile, stat, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import {
  DEFAULT_LAUNCHER_SETTINGS,
  SEED_BUNDLE_MINECRAFT_VERSION,
  type AppliedModBundleEntry,
  type Instance,
  type LauncherSettings
} from '../shared/types'

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
    // `appliedModBundleVersions` is typed `unknown` here rather than inheriting LauncherSettings's
    // current (nested, per-Minecraft-version) shape - a pre-multi-version file has it in the old
    // flat (per-mod-name only) shape under the very same key, so it can't be trusted until
    // `needsBundleVersionMigration` below says which shape is actually on disk.
    const parsed = JSON.parse(raw) as Omit<Partial<LauncherSettings>, 'appliedModBundleVersions'> & {
      selectedVersion?: string
      enabledBundledMods?: string[]
      appliedModBundleVersions?: unknown
      appliedOwnModVersion?: string | null
    }

    const needsInstanceMigration = parsed.instances === undefined
    // appliedOwnModVersion -> appliedOwnModVersions (singular -> plural rename) is the sentinel for
    // "old flat bundle-tracking shape", since appliedModBundleVersions itself kept its name across
    // the multi-version rework and so can't be used to detect the shape change on its own.
    const needsBundleVersionMigration = parsed.appliedOwnModVersions === undefined

    if (!needsInstanceMigration && !needsBundleVersionMigration) {
      return { ...DEFAULT_LAUNCHER_SETTINGS, ...parsed, appliedModBundleVersions: parsed.appliedModBundleVersions as LauncherSettings['appliedModBundleVersions'] }
    }

    // At least one legacy shape detected - migrate whichever parts are actually old in one pass,
    // instead of silently losing old settings/orphaning already-downloaded folders (would otherwise
    // mean re-downloading everything), ending in exactly one save either way.
    const instances = needsInstanceMigration ? await migrateLegacyInstances(parsed.enabledBundledMods ?? []) : (parsed.instances ?? [])

    // Pre-multi-version files only ever tracked bundle content for one implicit version (whatever
    // this launcher build targeted at the time) - nest their flat data under today's seed version,
    // the only one that could ever have been applied before this migration existed.
    const appliedModBundleVersions = needsBundleVersionMigration
      ? { [SEED_BUNDLE_MINECRAFT_VERSION]: (parsed.appliedModBundleVersions as Record<string, AppliedModBundleEntry> | undefined) ?? {} }
      : ((parsed.appliedModBundleVersions as LauncherSettings['appliedModBundleVersions'] | undefined) ?? {})
    const appliedOwnModVersions = needsBundleVersionMigration
      ? (parsed.appliedOwnModVersion ? { [SEED_BUNDLE_MINECRAFT_VERSION]: parsed.appliedOwnModVersion } : {})
      : (parsed.appliedOwnModVersions ?? {})

    const settings: LauncherSettings = {
      showSnapshots: parsed.showSnapshots ?? DEFAULT_LAUNCHER_SETTINGS.showSnapshots,
      instances,
      selectedInstanceId: needsInstanceMigration ? (instances[0]?.id ?? null) : (parsed.selectedInstanceId ?? null),
      dataRootOverride: parsed.dataRootOverride ?? DEFAULT_LAUNCHER_SETTINGS.dataRootOverride,
      appliedModBundleVersions,
      appliedOwnModVersions,
      appliedResourcepackVersions: parsed.appliedResourcepackVersions ?? DEFAULT_LAUNCHER_SETTINGS.appliedResourcepackVersions,
      maxMemoryMb: parsed.maxMemoryMb ?? DEFAULT_LAUNCHER_SETTINGS.maxMemoryMb,
      consoleInSeparateWindow: parsed.consoleInSeparateWindow ?? DEFAULT_LAUNCHER_SETTINGS.consoleInSeparateWindow,
      // A pre-multi-color-palette file's old single `accentColor` (string | null) key is simply
      // ignored here rather than migrated - it was only ever a derived-shades accent tone, not a
      // full ThemeColors object, so there's nothing sensible to map it onto.
      themeColors: parsed.themeColors ?? DEFAULT_LAUNCHER_SETTINGS.themeColors,
      language: parsed.language ?? DEFAULT_LAUNCHER_SETTINGS.language
    }
    await saveLauncherSettings(settings)
    return settings
  } catch {
    return DEFAULT_LAUNCHER_SETTINGS
  }
}

export async function saveLauncherSettings(settings: LauncherSettings): Promise<void> {
  await mkdir(app.getPath('userData'), { recursive: true })
  await writeFile(settingsPath(), JSON.stringify(settings, null, 2), 'utf-8')
}
