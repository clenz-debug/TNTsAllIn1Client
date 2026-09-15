import { readdir } from 'node:fs/promises'
import { join } from 'node:path'
import { SEED_BUNDLE_MINECRAFT_VERSION, type ModBundleManifest } from '../../shared/types'
import { bundledModsDir, bundledResourcesRoot } from './resourcePaths'

/** Repo root, not `launcher/` - `mod-bundle-manifest.json` is a cross-cutting artifact (references
 * both `mod/`'s own jar and the third-party jars `launcher/mods-bundle/` holds), same reasoning
 * `Projekt_Roadmap.md`/`Aktuelle_Phase.md` already live at the repo root rather than under either
 * subproject. */
const MANIFEST_URL = 'https://raw.githubusercontent.com/clenz-debug/TNTsAllIn1Client/main/mod-bundle-manifest.json'

export async function fetchManifest(): Promise<ModBundleManifest> {
  const response = await fetch(MANIFEST_URL)
  if (!response.ok) {
    throw new Error(`Mod-Bundle-Manifest konnte nicht geladen werden (${response.status}).`)
  }
  return (await response.json()) as ModBundleManifest
}

/** Memoized for the process's lifetime only - re-fetched fresh on every app start, never mid-session
 * (a hobby-project-proportionate simplicity trade-off: a version added to the manifest while the
 * launcher is already running only shows up after the next restart). `checkForModBundleUpdate`/
 * `applyModBundleUpdate` intentionally bypass this cache and call `fetchManifest()` directly instead,
 * since those two are meant to always act on the live file. */
let cachedManifest: Promise<ModBundleManifest | null> | null = null

function getCachedManifest(): Promise<ModBundleManifest | null> {
  if (!cachedManifest) {
    cachedManifest = fetchManifest().catch(() => null)
  }
  return cachedManifest
}

async function listLocalBundleVersions(): Promise<string[]> {
  try {
    const entries = await readdir(join(bundledResourcesRoot(), 'mods-bundle'), { withFileTypes: true })
    return entries.filter((entry) => entry.isDirectory()).map((entry) => entry.name)
  } catch {
    return []
  }
}

/**
 * Union of: the baked-in seed version (always available, even fully offline), every version id
 * that already has a `mods-bundle/<id>/` folder on disk (a previous download - or the seed itself -
 * stays usable offline even if a later manifest edit ever drops that entry), and every key of the
 * last successfully fetched manifest's `versions` map. A fetch failure only ever shrinks this back
 * to the first two - same "a failed background check is never a hard error" convention the existing
 * mod-bundle-update banner already follows.
 */
export async function getBundleCompatibleVersions(): Promise<Set<string>> {
  const manifest = await getCachedManifest()
  const localVersions = await listLocalBundleVersions()
  return new Set([SEED_BUNDLE_MINECRAFT_VERSION, ...localVersions, ...Object.keys(manifest?.versions ?? {})])
}

export async function isVersionBundleCompatible(versionId: string): Promise<boolean> {
  return (await getBundleCompatibleVersions()).has(versionId)
}

/** True once `mods-bundle/<versionId>/` actually has *something* in it - i.e. no first-use download
 * is needed before `bundleSync.syncBundledContent` can just copy what's already there. Scoped to
 * mods-bundle only, not own-mod/resourcepacks: those are always applied together with the mods in
 * one `applyModBundleUpdate` call, so the mods folder's presence is a reliable single proxy for
 * "this version's bundle has already been bootstrapped at least once". */
export async function hasLocalBundleContent(versionId: string): Promise<boolean> {
  try {
    return (await readdir(bundledModsDir(versionId))).length > 0
  } catch {
    return false
  }
}
