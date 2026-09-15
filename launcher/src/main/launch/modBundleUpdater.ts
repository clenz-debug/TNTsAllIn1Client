import { mkdir, rm, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import {
  type AppliedModBundleEntry,
  type BundledModPin,
  type BundledResourcepackPin,
  type LauncherSettings,
  type ModBundleUpdateInfo
} from '../../shared/types'
import { downloadAndVerifySha1 } from '../downloadVerify'
import { loadLauncherSettings, saveLauncherSettings } from '../launcherSettings'
import { fetchManifest } from './bundleCompat'
import { bundledModsDir, bundledResourcepacksDir, ownModDir } from './resourcePaths'

interface ModrinthVersionFile {
  filename: string
  primary: boolean
  url: string
  hashes: { sha1: string }
}

interface ModrinthVersionResponse {
  files: ModrinthVersionFile[]
}

async function resolveModrinthFile(versionId: string): Promise<ModrinthVersionFile> {
  const response = await fetch(`https://api.modrinth.com/v2/version/${versionId}`)
  if (!response.ok) {
    throw new Error(`Modrinth-Version ${versionId} konnte nicht geladen werden (${response.status}).`)
  }
  const data = (await response.json()) as ModrinthVersionResponse
  const file = data.files.find((f) => f.primary) ?? data.files[0]
  if (!file) {
    throw new Error(`Modrinth-Version ${versionId} hat keine herunterladbare Datei.`)
  }
  return file
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

  const settings = await loadLauncherSettings()
  const appliedMods = settings.appliedModBundleVersions[versionId] ?? {}
  const appliedResourcepacks = settings.appliedResourcepackVersions[versionId] ?? {}
  const appliedOwnMod = settings.appliedOwnModVersions[versionId] ?? null

  const outdatedMods = entry.bundledMods
    .filter((pin) => appliedMods[pin.name]?.versionId !== pin.modrinthVersionId)
    .map((pin) => ({
      name: pin.name,
      currentVersionId: appliedMods[pin.name]?.versionId ?? null,
      pinnedVersionId: pin.modrinthVersionId
    }))
  const outdatedResourcepacks = entry.bundledResourcepacks
    .filter((pin) => appliedResourcepacks[pin.name] !== pin.version)
    .map((pin) => ({
      name: pin.name,
      currentVersion: appliedResourcepacks[pin.name] ?? null,
      pinnedVersion: pin.version
    }))
  const ownModUpdateAvailable = entry.ownMod !== null && entry.ownMod.version !== appliedOwnMod
  return { outdatedMods, outdatedResourcepacks, ownModUpdateAvailable }
}

/** Downloads+verifies one pinned mod's current file and drops it into `mods-bundle/<versionId>/`,
 * deleting whatever was there under its *previous* filename first - Modrinth version filenames
 * embed the version number, so a bump doesn't overwrite the old file, it'd otherwise just sit
 * alongside the new one forever. */
async function applyBundledMod(
  versionId: string,
  pin: BundledModPin,
  previous: AppliedModBundleEntry | undefined
): Promise<AppliedModBundleEntry> {
  const file = await resolveModrinthFile(pin.modrinthVersionId)
  const buffer = await downloadAndVerifySha1(file.url, file.hashes.sha1, file.filename)

  const modsBundleDir = bundledModsDir(versionId)
  await mkdir(modsBundleDir, { recursive: true })
  if (previous && previous.fileName !== file.filename) {
    await rm(join(modsBundleDir, previous.fileName), { force: true })
  }
  await writeFile(join(modsBundleDir, file.filename), buffer)
  return { versionId: pin.modrinthVersionId, fileName: file.filename }
}

/** Downloads+verifies one pinned resourcepack and writes it as `<name>.zip` - simpler than
 * `applyBundledMod`: no Modrinth resolve step (direct URL+SHA-1 pin), and since we control the
 * filename ourselves (unlike Modrinth's version-numbered filenames) it's always the same name, so
 * a version bump just overwrites in place - no stale differently-named file to clean up. */
async function applyBundledResourcepack(versionId: string, pin: BundledResourcepackPin): Promise<void> {
  const buffer = await downloadAndVerifySha1(pin.url, pin.sha1, `${pin.name}.zip`)
  const resourcepacksDir = bundledResourcepacksDir(versionId)
  await mkdir(resourcepacksDir, { recursive: true })
  await writeFile(join(resourcepacksDir, `${pin.name}.zip`), buffer)
}

/**
 * Applies every currently-outdated entry for one Minecraft version - re-fetches the manifest itself
 * rather than trusting a possibly-stale result from an earlier {@link checkForModBundleUpdate} call
 * (same "always verify live" habit as everywhere else this project talks to an external API).
 * Persists progress after each individual mod/resourcepack/own-mod rather than only at the very end,
 * so a failure partway through (one bad download) keeps whatever already succeeded instead of
 * losing it. Also doubles as the first-use bootstrap for a version that was never downloaded at all
 * (see `ipc/handlers.ts`'s `LaunchPlay` handler) - "nothing applied yet" and "everything outdated"
 * are the same code path here, nothing extra needed for that case.
 */
export async function applyModBundleUpdate(versionId: string): Promise<LauncherSettings> {
  const manifest = await fetchManifest()
  let settings = await loadLauncherSettings()
  const entry = manifest.versions[versionId]
  if (!entry) {
    return settings
  }

  for (const pin of entry.bundledMods) {
    const versionMods = settings.appliedModBundleVersions[versionId] ?? {}
    const previous = versionMods[pin.name]
    if (previous?.versionId === pin.modrinthVersionId) continue
    const applied = await applyBundledMod(versionId, pin, previous)
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
    await applyBundledResourcepack(versionId, pin)
    settings = {
      ...settings,
      appliedResourcepackVersions: {
        ...settings.appliedResourcepackVersions,
        [versionId]: { ...versionPacks, [pin.name]: pin.version }
      }
    }
    await saveLauncherSettings(settings)
  }

  if (entry.ownMod && entry.ownMod.version !== (settings.appliedOwnModVersions[versionId] ?? null)) {
    const jarName = `tntsallin1client-${entry.ownMod.version}.jar`
    const buffer = await downloadAndVerifySha1(entry.ownMod.url, entry.ownMod.sha1, jarName)
    const modDir = ownModDir(versionId)
    // Cleared first rather than just overwriting by name - own-mod/<versionId>/ should only ever
    // hold the one current jar for that version (same invariant `bundleSync.ts#syncOwnModJar`'s
    // own "newest wins" logic already assumes), so a stale differently-named jar never lingers.
    await rm(modDir, { recursive: true, force: true })
    await mkdir(modDir, { recursive: true })
    await writeFile(join(modDir, jarName), buffer)
    settings = {
      ...settings,
      appliedOwnModVersions: { ...settings.appliedOwnModVersions, [versionId]: entry.ownMod.version }
    }
    await saveLauncherSettings(settings)
  }

  return settings
}
