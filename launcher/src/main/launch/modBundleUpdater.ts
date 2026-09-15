import { mkdir, rm, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import {
  MINECRAFT_VERSION,
  type AppliedModBundleEntry,
  type BundledModPin,
  type LauncherSettings,
  type ModBundleManifest,
  type ModBundleUpdateInfo
} from '../../shared/types'
import { downloadAndVerifySha1 } from '../downloadVerify'
import { loadLauncherSettings, saveLauncherSettings } from '../launcherSettings'
import { bundledResourcesRoot } from './resourcePaths'

/** Repo root, not `launcher/` - `mod-bundle-manifest.json` is a cross-cutting artifact (references
 * both `mod/`'s own jar and the third-party jars `launcher/mods-bundle/` holds), same reasoning
 * `Projekt_Roadmap.md`/`Aktuelle_Phase.md` already live at the repo root rather than under either
 * subproject. */
const MANIFEST_URL = 'https://raw.githubusercontent.com/clenz-debug/TNTsAllIn1Client/main/mod-bundle-manifest.json'

interface ModrinthVersionFile {
  filename: string
  primary: boolean
  url: string
  hashes: { sha1: string }
}

interface ModrinthVersionResponse {
  files: ModrinthVersionFile[]
}

async function fetchManifest(): Promise<ModBundleManifest> {
  const response = await fetch(MANIFEST_URL)
  if (!response.ok) {
    throw new Error(`Mod-Bundle-Manifest konnte nicht geladen werden (${response.status}).`)
  }
  return (await response.json()) as ModBundleManifest
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
 * Phase 9's deferred "mod bundle auto-update" - lets an already-installed launcher pick up newer
 * developer-approved Sodium/Lithium/etc. builds without a full new launcher release/reinstall.
 * `mod-bundle-manifest.json` only pins Modrinth project+version ids, never URLs/hashes directly -
 * those are always resolved fresh against Modrinth's own `/v2/version/<id>` here, reusing exactly
 * the download+SHA-1 pattern `modrinthApi.ts#installModrinthMod` already established (now shared
 * via `downloadVerify.ts`), so a manifest entry can never go stale even if Modrinth's CDN URLs
 * change.
 *
 * A mismatched `minecraftVersion` (manifest bumped for a newer Minecraft version than this
 * launcher build actually targets) is treated as "nothing to do here yet", not an error - this
 * launcher only ever maintains one bundle-compatible version at a time (`MINECRAFT_VERSION`).
 */
export async function checkForModBundleUpdate(): Promise<ModBundleUpdateInfo> {
  const manifest = await fetchManifest()
  if (manifest.minecraftVersion !== MINECRAFT_VERSION) {
    return { outdatedMods: [], ownModUpdateAvailable: false }
  }

  const settings = await loadLauncherSettings()
  const outdatedMods = manifest.bundledMods
    .filter((pin) => settings.appliedModBundleVersions[pin.name]?.versionId !== pin.modrinthVersionId)
    .map((pin) => ({
      name: pin.name,
      currentVersionId: settings.appliedModBundleVersions[pin.name]?.versionId ?? null,
      pinnedVersionId: pin.modrinthVersionId
    }))
  const ownModUpdateAvailable = manifest.ownMod !== null && manifest.ownMod.version !== settings.appliedOwnModVersion
  return { outdatedMods, ownModUpdateAvailable }
}

/** Downloads+verifies one pinned mod's current file and drops it into `mods-bundle/`, deleting
 * whatever was there under its *previous* filename first - Modrinth version filenames embed the
 * version number, so a bump doesn't overwrite the old file, it'd otherwise just sit alongside the
 * new one forever. */
async function applyBundledMod(pin: BundledModPin, previous: AppliedModBundleEntry | undefined): Promise<AppliedModBundleEntry> {
  const file = await resolveModrinthFile(pin.modrinthVersionId)
  const buffer = await downloadAndVerifySha1(file.url, file.hashes.sha1, file.filename)

  const modsBundleDir = join(bundledResourcesRoot(), 'mods-bundle')
  await mkdir(modsBundleDir, { recursive: true })
  if (previous && previous.fileName !== file.filename) {
    await rm(join(modsBundleDir, previous.fileName), { force: true })
  }
  await writeFile(join(modsBundleDir, file.filename), buffer)
  return { versionId: pin.modrinthVersionId, fileName: file.filename }
}

/**
 * Applies every currently-outdated entry - re-fetches the manifest itself rather than trusting a
 * possibly-stale result from an earlier {@link checkForModBundleUpdate} call (same "always verify
 * live" habit as everywhere else this project talks to an external API). Persists progress after
 * each individual mod rather than only at the very end, so a failure partway through (one bad
 * download) keeps whatever already succeeded instead of losing it.
 */
export async function applyModBundleUpdate(): Promise<LauncherSettings> {
  const manifest = await fetchManifest()
  let settings = await loadLauncherSettings()
  if (manifest.minecraftVersion !== MINECRAFT_VERSION) {
    return settings
  }

  for (const pin of manifest.bundledMods) {
    const previous = settings.appliedModBundleVersions[pin.name]
    if (previous?.versionId === pin.modrinthVersionId) continue
    const applied = await applyBundledMod(pin, previous)
    settings = { ...settings, appliedModBundleVersions: { ...settings.appliedModBundleVersions, [pin.name]: applied } }
    await saveLauncherSettings(settings)
  }

  if (manifest.ownMod && manifest.ownMod.version !== settings.appliedOwnModVersion) {
    const jarName = `tntsallin1client-${manifest.ownMod.version}.jar`
    const buffer = await downloadAndVerifySha1(manifest.ownMod.url, manifest.ownMod.sha1, jarName)
    const ownModDir = join(bundledResourcesRoot(), 'own-mod')
    // Cleared first rather than just overwriting by name - `own-mod/` should only ever hold the one
    // current jar (same invariant `bundleSync.ts#syncOwnModJar`'s own "newest wins" logic already
    // assumes), so a stale differently-named jar from an older version never lingers.
    await rm(ownModDir, { recursive: true, force: true })
    await mkdir(ownModDir, { recursive: true })
    await writeFile(join(ownModDir, jarName), buffer)
    settings = { ...settings, appliedOwnModVersion: manifest.ownMod.version }
    await saveLauncherSettings(settings)
  }

  return settings
}
