export interface MinecraftProfile {
  id: string
  name: string
  accessToken: string
  /** true when steps 4/5 of the auth chain (login_with_xbox, profile) were mocked because the
   * Mojang API allowlist request (aka.ms/mce-reviewappid) has not been approved yet. */
  isMock: boolean
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

/** Persisted across app restarts (Phase 6b) - see `main/launcherSettings.ts` for the on-disk
 * JSON file, same hand-rolled pattern as `main/auth/tokenCache.ts` rather than pulling in a new
 * dependency for two small fields. */
export interface LauncherSettings {
  selectedVersion: string
  showSnapshots: boolean
}

export const DEFAULT_LAUNCHER_SETTINGS: LauncherSettings = {
  selectedVersion: MINECRAFT_VERSION,
  showSnapshots: false
}
