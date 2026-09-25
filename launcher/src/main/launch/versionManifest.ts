import { localizedError } from '../../shared/errorMessages'
import { cachedJson } from '../offline'

const VERSION_MANIFEST_URL = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'

interface VersionManifestEntry {
  id: string
  type: string
  url: string
  releaseTime: string
}

interface VersionManifest {
  versions: VersionManifestEntry[]
}

async function fetchManifest(signal?: AbortSignal): Promise<VersionManifest> {
  return cachedJson(
    'mojang-version-manifest',
    async () => {
      const manifestResponse = await fetch(VERSION_MANIFEST_URL, { signal })
      if (!manifestResponse.ok) {
        throw localizedError('launch.versionManifestFetchFailed', { status: manifestResponse.status })
      }
      return (await manifestResponse.json()) as VersionManifest
    },
    signal
  )
}

/** Raw Mojang manifest entries (release/snapshot/old_beta/old_alpha, newest first) — used by
 * `versionList.ts` to cross-reference against Fabric's supported-game-versions list for the
 * Phase 6a version picker. */
export async function fetchManifestVersions(): Promise<VersionManifestEntry[]> {
  const manifest = await fetchManifest()
  return manifest.versions
}

export interface Rule {
  action: 'allow' | 'disallow'
  os?: { name?: string; arch?: string }
  features?: Record<string, boolean>
}

export interface ConditionalArgument {
  rules: Rule[]
  value: string | string[]
}

export interface LibraryArtifact {
  path: string
  url: string
  sha1: string
}

export interface LibraryEntry {
  name: string
  downloads?: { artifact?: LibraryArtifact; classifiers?: Record<string, LibraryArtifact> }
  rules?: Rule[]
  /** Only in versions up to 1.18.2 (LWJGL 3.2 and older): OS name -> key into
   * `downloads.classifiers` for the jar holding that OS's native libraries (DLLs), which the
   * launcher itself has to extract - see `nativesExtractor.ts`. May contain `${arch}`. */
  natives?: Record<string, string>
  extract?: { exclude?: string[] }
}

export interface VersionDetail {
  id: string
  assetIndex: { id: string; url: string; sha1: string }
  downloads: { client: { url: string; sha1: string } }
  libraries: LibraryEntry[]
  mainClass: string
  arguments?: {
    jvm: Array<string | ConditionalArgument>
    game: Array<string | ConditionalArgument>
  }
  /** The Java runtime Mojang's own launcher would download for this version (e.g.
   * `{ component: 'java-runtime-delta', majorVersion: 21 }`). We don't manage per-version
   * runtimes ourselves (see `gameProcess.ts`'s `getInstalledJavaMajorVersion` doc comment) - this
   * is only read to pre-flight-check the system `java` on PATH before spawning it. */
  javaVersion?: { component: string; majorVersion: number }
}

/** Cached per version as a whole (see `offline.ts`), so an offline launch doesn't even need the
 * manifest to find the detail's URL first. */
export async function fetchVersionDetail(versionId: string, signal?: AbortSignal): Promise<VersionDetail> {
  return cachedJson(
    `version-${versionId}`,
    async () => {
      const manifest = await fetchManifest(signal)
      const entry = manifest.versions.find((v) => v.id === versionId)
      if (!entry) {
        throw localizedError('launch.versionNotFound', { versionId })
      }

      const detailResponse = await fetch(entry.url, { signal })
      if (!detailResponse.ok) {
        throw localizedError('launch.versionDetailFetchFailed', { versionId, status: detailResponse.status })
      }
      return (await detailResponse.json()) as VersionDetail
    },
    signal
  )
}
