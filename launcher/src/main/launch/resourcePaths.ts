import { app } from 'electron'
import { join } from 'node:path'

/**
 * Where `mods-bundle/`, `resourcepacks-bundle/`, and the packaged snapshot of our own mod jar
 * (`own-mod/`) live - dev-mode this is `app.getAppPath()` (the `launcher/` folder itself, where
 * those two bundle folders already sit as siblings of `src/`), packaged this is
 * `process.resourcesPath` (electron-builder's `extraResources` copy everything listed under
 * `electron-builder.yml`'s `extraResources` there - see that file for exactly what and why).
 *
 * Centralized here because `bundleSync.ts`/`modsManager.ts` both used to call `app.getAppPath()`
 * directly, which only ever pointed at the right place in the previous unpackaged dev-only setup
 * (flagged as a known gap back when that code was first written, see `bundleSync.ts`'s own older
 * comment) - a packaged app has no sibling `mod/` project and no `mods-bundle/`/`resourcepacks-bundle/`
 * folders at `app.getAppPath()` at all unless electron-builder is told to put them somewhere.
 */
export function bundledResourcesRoot(): string {
  return app.isPackaged ? process.resourcesPath : app.getAppPath()
}

/**
 * Per-Minecraft-version bundle subfolders (multi-version support follow-up) - two different
 * versions' Sodium/etc. builds need to coexist on disk without overwriting each other, since a
 * user can have instances on several bundle-compatible versions at once. `versionId` is trusted
 * unsanitized here, same convention `installer.ts#sharedVersionDir` already uses for the parallel
 * `versions/<versionId>/` layout - it only ever comes from Mojang's own version manifest ids.
 */
export function bundledModsDir(versionId: string): string {
  return join(bundledResourcesRoot(), 'mods-bundle', versionId)
}

export function bundledResourcepacksDir(versionId: string): string {
  return join(bundledResourcesRoot(), 'resourcepacks-bundle', versionId)
}

export function ownModDir(versionId: string): string {
  return join(bundledResourcesRoot(), 'own-mod', versionId)
}
