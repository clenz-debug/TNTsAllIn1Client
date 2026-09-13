import { app } from 'electron'
import type { LauncherSettings } from '../shared/types'
import { loadLauncherSettings } from './launcherSettings'

/** The five top-level folders that make up the movable "game-data tree" - single source of truth
 * shared by `launch/storageConsolidation.ts` and `launch/storageManager.ts` so neither drifts out
 * of sync with what `instanceDir`/`libraryDestinationPath`/`runtimeDir`/the shared assets+versions
 * paths actually use. Deliberately NOT "everything under the root" - on the very first relocation
 * the source root is `app.getPath('userData')` itself, which also directly holds
 * `launcher-settings.json`/`auth.json`/`shared-settings/` as siblings that must never move. */
export const RELOCATABLE_SUBDIRS = ['instances', 'versions', 'libraries', 'assets', 'java-runtimes'] as const

let cachedRoot: string | null = null

/**
 * Root of the movable game-data tree - defaults to `app.getPath('userData')` itself (unchanged
 * behavior for anyone who never touches "Speicherort"), or a chosen folder once
 * `LauncherSettings.dataRootOverride` is set. Synchronous + cached: every replaced path-builder
 * (`instanceDir`, `libraryDestinationPath`, `runtimeDir`, ...) is called many times per install -
 * once per library, once per asset object - so re-reading/re-parsing `launcher-settings.json` on
 * every single call would be both slower and pointless, the value only ever changes via
 * `setDataRoot()`, an explicit, infrequent user action (see `launch/storageManager.ts`).
 */
export function dataRoot(): string {
  if (cachedRoot === null) {
    throw new Error('dataRoot() called before initDataRoot() completed.')
  }
  return cachedRoot
}

/**
 * Reads `dataRootOverride` once at startup and primes the cache - must be awaited in
 * `main/index.ts`'s `app.whenReady()` chain before `registerIpcHandlers()`/`createWindow()`, since
 * every install/launch path now resolves through `dataRoot()`. Returns the loaded settings too so
 * the startup sequence doesn't need a second `loadLauncherSettings()` call just to also get
 * `instances[]` for `storageConsolidation.ts`.
 */
export async function initDataRoot(): Promise<LauncherSettings> {
  const settings = await loadLauncherSettings()
  cachedRoot = settings.dataRootOverride ?? app.getPath('userData')
  return settings
}

/** Called by `launch/storageManager.ts#changeStorageLocation` once the physical move *and* the
 * settings persist have both fully succeeded - updates the cache so every subsequent call
 * immediately uses the new location, no app restart required. */
export function setDataRoot(newRoot: string): void {
  cachedRoot = newRoot
}
