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

/** Default pre-selection for the version picker (Phase 6a) — also the exact version the bundled
 * mods/resourcepacks in `mods-bundle`/`resourcepacks-bundle` (and our own mod jar) are built
 * against. Picking a different version in the UI still installs vanilla+Fabric fine, but
 * `bundleSync.syncBundledContent` skips the bundle entirely for any other id (see
 * `isBundleCompatibleVersion`) rather than handing Fabric Loader mods declaring a
 * `1.21.11`-only dependency range for a different game version, which Loader hard-rejects. */
export const MINECRAFT_VERSION = '1.21.11'

export function isBundleCompatibleVersion(versionId: string): boolean {
  return versionId === MINECRAFT_VERSION
}

export type GameVersionType = 'release' | 'snapshot'

export interface GameVersionSummary {
  id: string
  type: GameVersionType
  releaseTime: string
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
}

export const DEFAULT_LAUNCHER_SETTINGS: LauncherSettings = {
  showSnapshots: false,
  instances: [],
  selectedInstanceId: null
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
