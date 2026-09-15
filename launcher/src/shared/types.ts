/** Mirrors `minecraft/profile`'s own `skins[]`/`capes[]` entries (Phase 7) - `url` points at
 * Mojang's texture CDN (`textures.minecraft.net`), fetched through the main process and handed to
 * the renderer as a data: URI rather than loaded directly (see `skinApi.ts` - the renderer's CSP
 * only allows `img-src 'self' data:'`). */
export interface MinecraftSkin {
  id: string
  state: 'ACTIVE' | 'INACTIVE'
  url: string
  variant: 'CLASSIC' | 'SLIM'
}

export interface MinecraftCape {
  id: string
  state: 'ACTIVE' | 'INACTIVE'
  url: string
  alias: string
}

export interface MinecraftProfile {
  id: string
  name: string
  accessToken: string
  /** true when steps 4/5 of the auth chain (login_with_xbox, profile) were mocked because the
   * Mojang API allowlist request (aka.ms/mce-reviewappid) has not been approved yet. */
  isMock: boolean
  skins: MinecraftSkin[]
  capes: MinecraftCape[]
}

/** Model/arm-width choice for a skin upload - "classic" (Steve, 4px arms) or "slim" (Alex, 3px
 * arms). Was local to `main/auth/skinApi.ts` until the Phase 7 pixel editor + skin library needed
 * it in main, preload, and renderer alike. */
export type SkinVariant = 'classic' | 'slim'

/** One entry in the local skin library (`main/skin/skinLibrary.ts`) - the pixel editor's "save"
 * action writes/updates one of these; the Skins screen lists them for the user to pick which one
 * to actually wear (`SkinLibraryUse` re-uploads it via the normal Mojang skin endpoint), edit
 * again, or delete. Purely local bookkeeping - Mojang's account API itself has no concept of
 * "my saved skins", only ever one active skin. */
export interface SkinLibraryEntry {
  id: string
  name: string
  variant: SkinVariant
  createdAt: string
  /** The skin PNG itself, already as a `data:image/png;base64,...` URI - small enough (a few KB)
   * that shipping it inline with the list avoids a second IPC round-trip per thumbnail. */
  dataUri: string
}

/** Result of the direct-upload flow (`SkinUpload` IPC) - carries the new library entry's id
 * alongside the updated profile so the renderer can immediately offer to rename it (own user
 * request: name the skin *after* picking/uploading the file, not before). */
export interface SkinUploadResult {
  profile: MinecraftProfile
  libraryEntryId: string
}

export interface AuthProgressEvent {
  step: 'ms-oauth' | 'xbox-live' | 'xsts' | 'minecraft-login' | 'profile' | 'done' | 'error'
  message: string
}

export type LaunchStage =
  | 'manifest'
  | 'java-runtime'
  | 'client-jar'
  | 'libraries'
  | 'assets'
  | 'fabric-meta'
  | 'fabric-libraries'
  | 'bundles'
  | 'launching'
  | 'running'
  | 'done'
  | 'error'

export interface LaunchProgressEvent {
  stage: LaunchStage
  completed: number
  total: number
  label?: string
}

export interface GameLogEvent {
  source: 'launcher' | 'game'
  level: 'info' | 'error'
  message: string
}

/** The one Minecraft version this specific launcher build bakes directly into `extraResources`
 * (see `electron-builder.yml`) - default pre-selection for the version picker (Phase 6a) when
 * nothing else is bundle-compatible yet. This is no longer "the only bundle-compatible version":
 * any other version can become bundle-compatible purely via a `mod-bundle-manifest.json` entry
 * (see `main/launch/bundleCompat.ts#getBundleCompatibleVersions`), downloaded on demand on first
 * "Play" - no new launcher build/release needed for that. Only bumping *this* constant (and the
 * matching `extraResources` paths) changes what a fresh install bakes in up front. */
export const SEED_BUNDLE_MINECRAFT_VERSION = '1.21.11'

export function isBundleCompatibleVersion(versionId: string, bundleCompatibleVersions: readonly string[]): boolean {
  return bundleCompatibleVersions.includes(versionId)
}

export type GameVersionType = 'release' | 'snapshot'

export interface GameVersionSummary {
  id: string
  type: GameVersionType
  releaseTime: string
}

/** One Modrinth search hit, trimmed to what the "Mods durchsuchen" section actually shows (own
 * user request, modeled on how Dawn Client lets you search/install mods without leaving the
 * launcher). `iconDataUri` is already fetched and re-encoded by the main process - the renderer's
 * CSP only allows `img-src 'self' data:'`, same reasoning as `fetchSkinTexture`. */
export interface ModrinthSearchResult {
  projectId: string
  title: string
  description: string
  downloads: number
  iconDataUri: string | null
}

/** Sort orders Modrinth's `/search` endpoint accepts via `index=` - same options as the sort
 * dropdown on modrinth.com/app's own browse view. */
export type ModrinthSortIndex = 'relevance' | 'downloads' | 'follows' | 'newest' | 'updated'

/** Results per page for the Modrinth browse/search view - shared so the renderer's page-number
 * math (`totalHits / MODRINTH_SEARCH_PAGE_SIZE`) always matches what the main process actually
 * requests from Modrinth's `limit=` param. */
export const MODRINTH_SEARCH_PAGE_SIZE = 20

/** One page of a Modrinth browse/search request - `totalHits` lets the renderer know whether a
 * "load more" button still has anything to fetch. */
export interface ModrinthSearchPage {
  results: ModrinthSearchResult[]
  totalHits: number
}

/** Result of a successful custom-cape upload (`main/cape/capeStorage.ts`) - `url` is the public
 * Backblaze B2 URL baked into the bundled "Cape Provider" mod's lookup template, `dataUri` is the
 * same PNG re-encoded for an immediate `SkinModelPreview` refresh without a second network round
 * trip. */
export interface CapeUploadResult {
  url: string
  dataUri: string
}

/** Whether the current account has a custom cape stored in our B2 bucket - `dataUri` is set
 * whenever `exists` is true, so the renderer never needs a separate fetch just to preview it. */
export interface CustomCapeStatus {
  exists: boolean
  dataUri: string | null
}

/** Result of importing settings/mods from another client's folder (`main/launch/clientImport.ts`)
 * - reported back to the renderer so it can show a short summary ("3 Mods übernommen", ...). */
export interface ClientImportResult {
  importedOptions: boolean
  copiedMods: string[]
}

/** A single named instance (own user request: "statt einem Wechsel der Version ein System... das
 * man einzelne Instanzen erstellen kann", so e.g. the same Minecraft version can exist twice - once
 * with a mod, once without - without constantly toggling mods back and forth on one shared folder).
 * Each instance owns its own on-disk game directory (`instances/<id>/`, see `main/launch/installer.ts`'s
 * `instanceDir`), so its saves/options/mods/resourcepacks never mix with another instance's. */
export interface Instance {
  /** Stable, filesystem-safe id - also the literal folder name under `instances/`. Never shown to
   * the user directly (that's `name`), generated once at creation time and never reused. */
  id: string
  name: string
  versionId: string
  /** Filenames (from `mods-bundle/`) turned ON for this specific instance - see the equivalent
   * field's own doc comment history on the old, pre-instance `LauncherSettings.enabledBundledMods`
   * this replaces. Opt-in per instance now instead of one shared global list, for the same reason
   * instances exist at all: two instances of the same version can have different mods enabled. */
  enabledBundledMods: string[]
}

/** Persisted across app restarts (Phase 6b) - see `main/launcherSettings.ts` for the on-disk
 * JSON file, same hand-rolled pattern as `main/auth/tokenCache.ts` rather than pulling in a new
 * dependency for two small fields. Was a single `selectedVersion`/`enabledBundledMods` pair
 * (Phase 6a/6c) until the instance system replaced "one shared install per version" with real,
 * independent instances - `main/launcherSettings.ts#loadLauncherSettings` migrates any old-shape
 * file (and any already-downloaded legacy `instances/<versionId>/` folder) into this shape once. */
export interface LauncherSettings {
  showSnapshots: boolean
  instances: Instance[]
  selectedInstanceId: string | null
  /** Custom root for the movable game-data tree (`instances/`, `versions/`, `libraries/`,
   * `assets/`, `java-runtimes/` - see `main/dataRoot.ts#RELOCATABLE_SUBDIRS`), chosen via the
   * Instances screen's "Speicherort" section. `null` means the default, `app.getPath('userData')`.
   * Never covers `launcher-settings.json`/`auth.json`/`shared-settings/` themselves, which always
   * stay at the fixed OS profile folder. */
  dataRootOverride: string | null
  /** What `main/launch/modBundleUpdater.ts` last successfully wrote into `mods-bundle/<mcVersion>/`,
   * keyed first by Minecraft version then by the manifest's mod `name` - two different versions'
   * bundles can be applied/tracked at once now (see `resourcePaths.ts`'s per-version layout), so
   * "already applied" needs the version dimension too, not just the mod name. */
  appliedModBundleVersions: Record<string, Record<string, AppliedModBundleEntry>>
  /** Mirrors `appliedModBundleVersions`'s version-keying for the one non-Modrinth entry
   * (`ModBundleVersionEntry.ownMod`, our own mod jar) - one applied-version string per Minecraft
   * version that has ever had the own mod jar downloaded for it. */
  appliedOwnModVersions: Record<string, string>
  /** Mirrors `appliedModBundleVersions`'s shape for `ModBundleVersionEntry.bundledResourcepacks` -
   * just a version label per pack per Minecraft version (no separate filename to track: unlike
   * Modrinth-sourced mods, we control the resourcepack filenames ourselves and always write
   * `<name>.zip`, so there's never a stale differently-named file to clean up). */
  appliedResourcepackVersions: Record<string, Record<string, string>>
}

export const DEFAULT_LAUNCHER_SETTINGS: LauncherSettings = {
  showSnapshots: false,
  instances: [],
  selectedInstanceId: null,
  dataRootOverride: null,
  appliedModBundleVersions: {},
  appliedOwnModVersions: {},
  appliedResourcepackVersions: {}
}

/** One pinned third-party mod entry in `mod-bundle-manifest.json` (repo root) - only references a
 * Modrinth project+version id, never a URL/hash directly; those are always resolved fresh against
 * Modrinth's own `/v2/version/<id>` at check/apply time (`main/launch/modBundleUpdater.ts`), so the
 * manifest itself can never go stale if Modrinth ever changes its CDN URLs. */
export interface BundledModPin {
  name: string
  modrinthProjectId: string
  modrinthVersionId: string
}

/** One pinned bundled resourcepack entry in `mod-bundle-manifest.json` - unlike `BundledModPin`,
 * resourcepacks aren't all sourced from Modrinth (some are GitHub repos, some hand-curated Vanilla
 * Tweaks selections - see `Projekt_Roadmap.md`'s license section), so there's no single API to
 * live-resolve a version id against. Instead this pins a direct download URL + SHA-1 straight to a
 * GitHub Release asset the maintainer uploaded themselves - same pattern `ownMod` already uses. */
export interface BundledResourcepackPin {
  name: string
  version: string
  url: string
  sha1: string
}

/** One Minecraft version's full bundle content - own mod jar, third-party mods, resourcepacks.
 * The maintainer only ever pushes a new/updated entry once everything in it has been built and
 * tested together (see `mod-bundle-release-runbook.md`) - there's no code-level guarantee that a
 * given entry is "complete", that's a process rule, not a type constraint. */
export interface ModBundleVersionEntry {
  ownMod: { version: string; url: string; sha1: string } | null
  bundledMods: BundledModPin[]
  bundledResourcepacks: BundledResourcepackPin[]
}

/** The mod-bundle auto-update manifest (`mod-bundle-manifest.json`, fetched via
 * raw.githubusercontent.com) - hand-maintained by the developer, pins exactly which build of each
 * bundled mod/resourcepack/own-mod-jar is currently approved per Minecraft version, not just
 * "whatever's newest" (Phase 9's roadmap text calls this a "self-controlled manifest" on purpose).
 * A Minecraft version becomes bundle-compatible for every already-installed launcher purely by
 * gaining a key here - see `main/launch/bundleCompat.ts`. */
export interface ModBundleManifest {
  versions: Record<string, ModBundleVersionEntry>
}

/** What `modBundleUpdater.ts` actually wrote for one bundled mod - the filename too, not just the
 * version id, so a version bump (which changes the Modrinth filename) can delete the old file
 * instead of leaving it sitting alongside the new one. */
export interface AppliedModBundleEntry {
  versionId: string
  fileName: string
}

/** One manifest entry whose pinned version doesn't match what's currently applied. */
export interface ModBundleUpdateEntry {
  name: string
  currentVersionId: string | null
  pinnedVersionId: string
}

/** Same shape as {@link ModBundleUpdateEntry} but for a resourcepack, whose "pinned version" is
 * just the manifest's own free-form `version` label rather than a Modrinth version id. */
export interface ModBundleResourcepackUpdateEntry {
  name: string
  currentVersion: string | null
  pinnedVersion: string
}

export interface ModBundleUpdateInfo {
  outdatedMods: ModBundleUpdateEntry[]
  outdatedResourcepacks: ModBundleResourcepackUpdateEntry[]
  ownModUpdateAvailable: boolean
}

/** Current storage location + free space, shown in the Instances screen's "Speicherort" section
 * (`main/launch/storageManager.ts#getStorageInfo`). `freeBytes` is `null` when `statfs` isn't
 * available or the folder doesn't exist yet - the renderer just hides that line then. */
export interface StorageInfo {
  path: string
  freeBytes: number | null
}

/** Progress for `main/launch/storageManager.ts#changeStorageLocation`'s file move - deliberately a
 * separate shape from `LaunchProgressEvent`/`LaunchStage`: a storage move is an independent,
 * Instances-screen-scoped operation (not part of a "Play click"'s install+launch pipeline), and
 * there's only one kind of "stage" here (moving files), so `subfolder` + `label` already carry
 * enough info without a `stage` field. */
export interface StorageMoveProgressEvent {
  subfolder: string
  completed: number
  total: number
  label?: string
}

/** Phase 9 - pushed from `main/autoUpdate.ts` (wraps `electron-updater`'s own event stream, see
 * that file) over the `update:status` channel whenever it changes; the renderer just mirrors
 * whatever it's given rather than polling. Replaces the old Phase 6d `update-manifest.json`-based
 * check, which only ever told you "something's newer" with a link, never downloaded or installed
 * anything - `electron-updater` does both, driven straight off this repo's GitHub Releases instead
 * of a hand-maintained JSON file that had to be bumped in lockstep with `package.json`'s version. */
export interface UpdateStatus {
  state: 'checking' | 'available' | 'not-available' | 'downloading' | 'downloaded' | 'error'
  version?: string
  /** 0-100, only meaningful while `state === 'downloading'`. */
  percent?: number
  /** Only meaningful while `state === 'downloaded'`. */
  releaseNotes?: string
  /** Only meaningful while `state === 'error'`. */
  message?: string
}
