import { copyFile, mkdir, readdir, readFile, rename, rm, rmdir, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { localizedError } from '../../shared/errorMessages'
import {
  type AppliedModBundleEntry,
  type BundledModPin,
  type BundledResourcepackPin,
  type LauncherSettings,
  type ModBundleManifest,
  type ModBundleUpdateInfo,
  type ModBundleVersionEntry
} from '../../shared/types'
import { downloadAndVerifySha1 } from '../downloadVerify'
import { loadLauncherSettings, saveLauncherSettings } from '../launcherSettings'
import { fetchManifest } from './bundleCompat'
import { bakedOwnModDir, downloadedBundlesRoot, hasDownloadedBundle, seedManifestPath, usesDownloadedBundles } from './resourcePaths'

interface ModrinthVersionFile {
  filename: string
  primary: boolean
  url: string
  hashes: { sha1: string }
}

interface ModrinthVersionResponse {
  files: ModrinthVersionFile[]
}

async function resolveModrinthFile(versionId: string, signal?: AbortSignal): Promise<ModrinthVersionFile> {
  const response = await fetch(`https://api.modrinth.com/v2/version/${versionId}`, { signal })
  if (!response.ok) {
    throw localizedError('mods.modrinthVersionLoadFailed', { versionId, status: response.status })
  }
  const data = (await response.json()) as ModrinthVersionResponse
  const file = data.files.find((f) => f.primary) ?? data.files[0]
  if (!file) {
    throw localizedError('mods.modrinthVersionNoFile', { versionId })
  }
  return file
}

/** The three folders one version's bundle consists of. */
interface BundleDirs {
  mods: string
  resourcepacks: string
  ownMod: string
}

function bundleDirsUnder(root: string, versionId: string): BundleDirs {
  return {
    mods: join(root, 'mods-bundle', versionId),
    resourcepacks: join(root, 'resourcepacks-bundle', versionId),
    ownMod: join(root, 'own-mod', versionId)
  }
}

/** What one version currently has, in the manifest's terms: mod name -> Modrinth version id, pack
 * name -> version label, own mod version. */
interface AppliedState {
  mods: Record<string, string>
  resourcepacks: Record<string, string>
  ownMod: string | null
}

async function readSeedManifest(): Promise<ModBundleManifest | null> {
  try {
    return JSON.parse(await readFile(seedManifestPath(), 'utf8')) as ModBundleManifest
  } catch {
    return null
  }
}

function stateOfEntry(entry: ModBundleVersionEntry | undefined): AppliedState {
  if (!entry) return { mods: {}, resourcepacks: {}, ownMod: null }
  return {
    mods: Object.fromEntries(entry.bundledMods.map((pin) => [pin.name, pin.modrinthVersionId])),
    resourcepacks: Object.fromEntries(entry.bundledResourcepacks.map((pin) => [pin.name, pin.version])),
    ownMod: entry.ownMod?.version ?? null
  }
}

/**
 * What's applied for a version right now. An installed launcher still running a version from the
 * installer's baked copy has nothing in its settings for it - that copy is whatever the manifest
 * said when the launcher was built (`seed-manifest.json`), so compare against that instead of
 * calling every baked mod outdated on a fresh install.
 */
async function appliedState(versionId: string, settings: LauncherSettings): Promise<AppliedState> {
  if (usesDownloadedBundles() && !hasDownloadedBundle(versionId)) {
    return stateOfEntry((await readSeedManifest())?.versions[versionId])
  }
  return {
    mods: Object.fromEntries(Object.entries(settings.appliedModBundleVersions[versionId] ?? {}).map(([name, applied]) => [name, applied.versionId])),
    resourcepacks: settings.appliedResourcepackVersions[versionId] ?? {},
    ownMod: settings.appliedOwnModVersions[versionId] ?? null
  }
}

/**
 * Phase 9's deferred "mod bundle auto-update", now per-Minecraft-version (multi-version support
 * follow-up) - lets an already-installed launcher pick up newer developer-approved Sodium/Lithium/
 * etc. builds, resourcepacks, or a new own-mod-jar build for any bundle-compatible version, without
 * a full new launcher release/reinstall. `mod-bundle-manifest.json` only pins Modrinth project+
 * version ids for third-party mods (never URLs/hashes directly - those are always resolved fresh
 * against Modrinth's own `/v2/version/<id>` here, so a manifest entry can never go stale even if
 * Modrinth's CDN URLs change) plus direct URL+SHA-1 pins for resourcepacks/the own mod jar (things
 * *we* host ourselves, not Modrinth - see `mod-bundle-release-runbook.md`).
 *
 * A version with no manifest entry at all is treated as "nothing to do here yet", not an error -
 * this project only ever maintains bundle content for versions the maintainer has actually finished
 * building and testing (see `mod-bundle-release-runbook.md`).
 */
export async function checkForModBundleUpdate(versionId: string): Promise<ModBundleUpdateInfo> {
  const manifest = await fetchManifest()
  const entry = manifest.versions[versionId]
  if (!entry) {
    return { outdatedMods: [], outdatedResourcepacks: [], ownModUpdateAvailable: false }
  }

  const applied = await appliedState(versionId, await loadLauncherSettings())
  const outdatedMods = entry.bundledMods
    .filter((pin) => applied.mods[pin.name] !== pin.modrinthVersionId)
    .map((pin) => ({
      name: pin.name,
      currentVersionId: applied.mods[pin.name] ?? null,
      pinnedVersionId: pin.modrinthVersionId
    }))
  const outdatedResourcepacks = entry.bundledResourcepacks
    .filter((pin) => applied.resourcepacks[pin.name] !== pin.version)
    .map((pin) => ({
      name: pin.name,
      currentVersion: applied.resourcepacks[pin.name] ?? null,
      pinnedVersion: pin.version
    }))
  // Dev mode always runs the live build of our mod (bundleSync.ts) - a release jar is never offered there.
  const ownModUpdateAvailable = usesDownloadedBundles() && entry.ownMod !== null && entry.ownMod.version !== applied.ownMod
  return { outdatedMods, outdatedResourcepacks, ownModUpdateAvailable }
}

/** Downloads+verifies one pinned mod's current file and drops it into `modsDir`, deleting whatever
 * was there under its *previous* filename first - Modrinth version filenames embed the version
 * number, so a bump doesn't overwrite the old file, it'd otherwise just sit alongside the new one
 * forever. */
async function applyBundledMod(
  modsDir: string,
  pin: BundledModPin,
  previous: AppliedModBundleEntry | undefined,
  signal?: AbortSignal
): Promise<AppliedModBundleEntry> {
  const file = await resolveModrinthFile(pin.modrinthVersionId, signal)
  const buffer = await downloadAndVerifySha1(file.url, file.hashes.sha1, file.filename, signal)

  await mkdir(modsDir, { recursive: true })
  if (previous && previous.fileName !== file.filename) {
    await rm(join(modsDir, previous.fileName), { force: true })
  }
  await writeFile(join(modsDir, file.filename), buffer)
  return { versionId: pin.modrinthVersionId, fileName: file.filename }
}

/** Downloads+verifies one pinned resourcepack and writes it as `<name>.zip` - simpler than
 * `applyBundledMod`: no Modrinth resolve step (direct URL+SHA-1 pin), and since we control the
 * filename ourselves (unlike Modrinth's version-numbered filenames) it's always the same name, so
 * a version bump just overwrites in place - no stale differently-named file to clean up. */
async function applyBundledResourcepack(resourcepacksDir: string, pin: BundledResourcepackPin, signal?: AbortSignal): Promise<void> {
  const buffer = await downloadAndVerifySha1(pin.url, pin.sha1, `${pin.name}.zip`, signal)
  await mkdir(resourcepacksDir, { recursive: true })
  await writeFile(join(resourcepacksDir, `${pin.name}.zip`), buffer)
}

/** Writes our own mod jar - `ownModDir` should only ever hold the one current jar for that version
 * (same invariant `bundleSync.ts#syncOwnModJar`'s own "newest wins" logic already assumes), so it's
 * cleared first and a stale differently-named jar never lingers. */
async function applyOwnMod(ownModDir: string, ownMod: NonNullable<ModBundleVersionEntry['ownMod']>, signal?: AbortSignal): Promise<void> {
  const jarName = `tntsallin1client-${ownMod.version}.jar`
  const buffer = await downloadAndVerifySha1(ownMod.url, ownMod.sha1, jarName, signal)
  await rm(ownModDir, { recursive: true, force: true })
  await mkdir(ownModDir, { recursive: true })
  await writeFile(join(ownModDir, jarName), buffer)
}

/**
 * An installed launcher's first update of a version it has only the installer's baked copy of (or
 * nothing at all): the whole version is downloaded into a staging folder under `userData/bundles/`
 * and moved into place only once complete - `mods-bundle/<versionId>/` last, since its presence is
 * what marks the downloaded copy as the active one (`resourcePaths.ts`). A full download rather
 * than patching a copy of the baked files: the manifest names no files, so which baked jar belongs
 * to which pin can't be told reliably. If the manifest has no own mod for the version, the baked
 * one is carried over - the downloaded copy must never end up without our mod.
 */
async function downloadCompleteBundle(
  versionId: string,
  entry: ModBundleVersionEntry,
  settings: LauncherSettings,
  signal?: AbortSignal
): Promise<LauncherSettings> {
  const staging = bundleDirsUnder(join(downloadedBundlesRoot(), '.staging'), versionId)
  await Promise.all(Object.values(staging).map((dir) => rm(dir, { recursive: true, force: true })))

  const appliedMods: Record<string, AppliedModBundleEntry> = {}
  for (const pin of entry.bundledMods) {
    appliedMods[pin.name] = await applyBundledMod(staging.mods, pin, undefined, signal)
  }
  const appliedPacks: Record<string, string> = {}
  for (const pin of entry.bundledResourcepacks) {
    await applyBundledResourcepack(staging.resourcepacks, pin, signal)
    appliedPacks[pin.name] = pin.version
  }
  await mkdir(staging.resourcepacks, { recursive: true })
  if (entry.ownMod) {
    await applyOwnMod(staging.ownMod, entry.ownMod, signal)
  } else {
    await mkdir(staging.ownMod, { recursive: true })
    const baked = await readdir(bakedOwnModDir(versionId)).catch(() => [] as string[])
    for (const jar of baked.filter((name) => name.endsWith('.jar'))) {
      await copyFile(join(bakedOwnModDir(versionId), jar), join(staging.ownMod, jar))
    }
  }

  await mkdir(staging.mods, { recursive: true })
  const final = bundleDirsUnder(downloadedBundlesRoot(), versionId)
  for (const kind of ['resourcepacks', 'ownMod', 'mods'] as const) {
    await rm(final[kind], { recursive: true, force: true })
    await mkdir(join(final[kind], '..'), { recursive: true })
    await rename(staging[kind], final[kind])
    // The now-empty staging parent - left alone if another version is being staged in it right now.
    await rmdir(join(staging[kind], '..')).catch(() => undefined)
  }
  await rmdir(join(downloadedBundlesRoot(), '.staging')).catch(() => undefined)

  const { [versionId]: _previousOwnMod, ...otherOwnMods } = settings.appliedOwnModVersions
  const updated: LauncherSettings = {
    ...settings,
    appliedModBundleVersions: { ...settings.appliedModBundleVersions, [versionId]: appliedMods },
    appliedResourcepackVersions: { ...settings.appliedResourcepackVersions, [versionId]: appliedPacks },
    appliedOwnModVersions: entry.ownMod ? { ...settings.appliedOwnModVersions, [versionId]: entry.ownMod.version } : otherOwnMods
  }
  await saveLauncherSettings(updated)
  return updated
}

/**
 * Applies every currently-outdated entry for one Minecraft version - re-fetches the manifest itself
 * rather than trusting a possibly-stale result from an earlier {@link checkForModBundleUpdate} call
 * (same "always verify live" habit as everywhere else this project talks to an external API).
 * Persists progress after each individual mod/resourcepack/own-mod rather than only at the very end,
 * so a failure partway through (one bad download) keeps whatever already succeeded instead of
 * losing it. Also doubles as the first-use bootstrap for a version that was never downloaded at all
 * (see `ipc/handlers.ts`'s `LaunchPlay` handler). An installed launcher's first update of a version
 * goes through {@link downloadCompleteBundle} instead.
 */
export async function applyModBundleUpdate(versionId: string, signal?: AbortSignal): Promise<LauncherSettings> {
  const manifest = await fetchManifest()
  let settings = await loadLauncherSettings()
  const entry = manifest.versions[versionId]
  if (!entry) {
    return settings
  }
  if (usesDownloadedBundles() && !hasDownloadedBundle(versionId)) {
    return downloadCompleteBundle(versionId, entry, settings, signal)
  }

  const dirs = bundleDirsUnder(downloadedBundlesRoot(), versionId)

  for (const pin of entry.bundledMods) {
    const versionMods = settings.appliedModBundleVersions[versionId] ?? {}
    const previous = versionMods[pin.name]
    if (previous?.versionId === pin.modrinthVersionId) continue
    const applied = await applyBundledMod(dirs.mods, pin, previous, signal)
    settings = {
      ...settings,
      appliedModBundleVersions: {
        ...settings.appliedModBundleVersions,
        [versionId]: { ...versionMods, [pin.name]: applied }
      }
    }
    await saveLauncherSettings(settings)
  }

  for (const pin of entry.bundledResourcepacks) {
    const versionPacks = settings.appliedResourcepackVersions[versionId] ?? {}
    if (versionPacks[pin.name] === pin.version) continue
    await applyBundledResourcepack(dirs.resourcepacks, pin, signal)
    settings = {
      ...settings,
      appliedResourcepackVersions: {
        ...settings.appliedResourcepackVersions,
        [versionId]: { ...versionPacks, [pin.name]: pin.version }
      }
    }
    await saveLauncherSettings(settings)
  }

  // Never in dev mode: a downloaded release jar in launcher/own-mod/ would hide the live build of our
  // mod (bundleSync.ts prefers that folder as a pinned snapshot).
  if (usesDownloadedBundles() && entry.ownMod && entry.ownMod.version !== (settings.appliedOwnModVersions[versionId] ?? null)) {
    await applyOwnMod(dirs.ownMod, entry.ownMod, signal)
    settings = {
      ...settings,
      appliedOwnModVersions: { ...settings.appliedOwnModVersions, [versionId]: entry.ownMod.version }
    }
    await saveLauncherSettings(settings)
  }

  return settings
}
