/**
 * The "legacy" Minecraft versions - everything before 1.14, where Fabric doesn't exist. They start
 * as plain Minecraft without a mod loader (own user decision: players who want the old versions'
 * modpacks use other launchers for that anyway, so no Forge here), which also means without our own
 * mod until it has an own way in on these versions.
 *
 * Per the project's version scheme only the last, most stable sub-version of each old Minecraft
 * version is offered (see `mod-bundle-release-runbook.md`). 1.6.4 and older are not in the list yet:
 * they need more than the old launch format (assets in the pre-1.7.3 layout, session argument).
 */
export const LEGACY_VERSIONS: readonly string[] = ['1.13.2', '1.12.2', '1.11.2', '1.10.2', '1.9.4', '1.8.9', '1.7.10']

export function isLegacyVersion(versionId: string): boolean {
  return LEGACY_VERSIONS.includes(versionId)
}

/**
 * Releases before 1.13 write `options.txt` in the LWJGL 2 format (key bindings as numeric key
 * codes, no `version:` line) and drop every line they don't know when saving. Sharing one
 * `options.txt` between such a version and a modern one would reset the modern settings, so the two
 * generations each get their own shared file (see `main/launch/sharedSettings.ts`).
 */
export function usesLegacyOptionsFormat(versionId: string): boolean {
  const match = /^1\.(\d+)(?:\.\d+)?$/.exec(versionId)
  return match !== null && Number(match[1]) < 13
}
