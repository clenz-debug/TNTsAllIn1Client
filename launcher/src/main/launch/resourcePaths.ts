import { app } from 'electron'
import { existsSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Where `mods-bundle/`, `resourcepacks-bundle/`, and our own mod jar (`own-mod/`) live, per
 * Minecraft version.
 *
 * Two places in a packaged (installed) launcher:
 *  - **baked**: what the installer shipped, `process.resourcesPath` (electron-builder's
 *    `extraResources`, see `electron-builder.yml`). Read-only in practice: inside the install folder,
 *    which may be under "Program Files", and replaced wholesale by every launcher update.
 *  - **downloaded**: `userData/bundles/`, where `modBundleUpdater.ts` writes. Always writable and
 *    untouched by launcher updates.
 * Per version exactly one of the two is active, never a mix: the downloaded copy as soon as one
 * exists (it is always complete, see `modBundleUpdater.ts#applyModBundleUpdate`), otherwise the
 * baked one.
 *
 * Dev mode keeps a single place, the `launcher/` folder itself (where the dev-populated bundle
 * folders sit next to `src/`) - downloads there too, so a clicked "Aktualisieren" never hides the
 * folders you maintain by hand.
 */

/** The installer's (or, in dev, the `launcher/` folder's) bundle root. */
export function bundledResourcesRoot(): string {
  return app.isPackaged ? process.resourcesPath : app.getAppPath()
}

/** Only an installed launcher keeps downloads apart from what it shipped - see the file comment. */
export function usesDownloadedBundles(): boolean {
  return app.isPackaged
}

export function downloadedBundlesRoot(): string {
  return usesDownloadedBundles() ? join(app.getPath('userData'), 'bundles') : bundledResourcesRoot()
}

/** A complete downloaded copy exists for this version (`mods-bundle/<versionId>/` is written last,
 * so its presence marks the copy as complete). */
export function hasDownloadedBundle(versionId: string): boolean {
  return usesDownloadedBundles() && existsSync(join(downloadedBundlesRoot(), 'mods-bundle', versionId))
}

function activeRoot(versionId: string): string {
  return hasDownloadedBundle(versionId) ? downloadedBundlesRoot() : bundledResourcesRoot()
}

/**
 * Per-Minecraft-version bundle subfolders (multi-version support follow-up) - two different
 * versions' Sodium/etc. builds need to coexist on disk without overwriting each other, since a
 * user can have instances on several bundle-compatible versions at once. `versionId` is trusted
 * unsanitized here, same convention `installer.ts#sharedVersionDir` already uses for the parallel
 * `versions/<versionId>/` layout - it only ever comes from Mojang's own version manifest ids.
 */
export function bundledModsDir(versionId: string): string {
  return join(activeRoot(versionId), 'mods-bundle', versionId)
}

export function bundledResourcepacksDir(versionId: string): string {
  return join(activeRoot(versionId), 'resourcepacks-bundle', versionId)
}

export function ownModDir(versionId: string): string {
  return join(activeRoot(versionId), 'own-mod', versionId)
}

/** The baked own-mod folder itself - dev mode's optional pinned snapshot, see `bundleSync.ts`. */
export function bakedOwnModDir(versionId: string): string {
  return join(bundledResourcesRoot(), 'own-mod', versionId)
}

/** The mod bundle manifest as it was when this launcher was built - what the baked bundles
 * correspond to. Packaged: shipped next to them (`electron-builder.yml`); dev: the repo's file. */
export function seedManifestPath(): string {
  return app.isPackaged ? join(process.resourcesPath, 'seed-manifest.json') : join(app.getAppPath(), '..', 'mod-bundle-manifest.json')
}
