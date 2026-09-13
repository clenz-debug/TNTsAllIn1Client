import { join } from 'node:path'
import { dataRoot } from '../dataRoot'
import type { LibraryEntry, Rule } from './versionManifest'

const OS_NAME = process.platform === 'win32' ? 'windows' : process.platform === 'darwin' ? 'osx' : 'linux'

/** Evaluates a Mojang version-manifest rule list (used both for library selection and for
 * conditional JVM/game arguments) against the current OS and the given feature flags. */
export function matchesRules(rules: Rule[] | undefined, features: Record<string, boolean> = {}): boolean {
  if (!rules || rules.length === 0) return true

  let allowed = false
  for (const rule of rules) {
    const osMatches = !rule.os || !rule.os.name || rule.os.name === OS_NAME
    const featuresMatch =
      !rule.features || Object.entries(rule.features).every(([key, value]) => features[key] === value)
    if (osMatches && featuresMatch) {
      allowed = rule.action === 'allow'
    }
  }
  return allowed
}

/** Modern (LWJGL3-based) version manifests list platform-specific natives as regular library
 * entries filtered by `rules` — LWJGL extracts its own natives from whatever's on the classpath
 * at runtime, so no separate "natives" extraction step is needed for 1.21.11. */
export function librariesForCurrentOs(libraries: LibraryEntry[]): LibraryEntry[] {
  return libraries.filter((lib) => matchesRules(lib.rules))
}

/** Shared across every instance and every version (own user request: instances of the same version
 * were each downloading a full separate copy) - a Maven coordinate already encodes its own
 * group/artifact/version/classifier into `libraryRelativePath`, so different libraries (Vanilla's
 * own, and Fabric Loader's, see `fabricInstaller.ts`) never collide on the same path even across
 * differently-versioned installs. */
export function libraryDestinationPath(libraryRelativePath: string): string {
  return join(dataRoot(), 'libraries', libraryRelativePath)
}

export function buildClasspath(libraryPaths: string[], clientJarPath: string): string {
  const separator = process.platform === 'win32' ? ';' : ':'
  return [...libraryPaths, clientJarPath].join(separator)
}
