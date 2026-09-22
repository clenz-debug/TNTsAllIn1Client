import { localizedError } from '../../shared/errorMessages'

const FABRIC_META_BASE = 'https://meta.fabricmc.net/v2'

interface FabricLoaderVersionEntry {
  loader: { version: string; stable: boolean }
}

interface FabricGameVersionEntry {
  version: string
  stable: boolean
}

/** Game versions Fabric maintains intermediary mappings for — i.e. versions Fabric Loader can
 * actually run. Cross-referenced against the Mojang manifest in `versionList.ts` so the version
 * picker only ever offers versions this launcher can install through Fabric. */
export async function fetchFabricGameVersions(): Promise<Set<string>> {
  const response = await fetch(`${FABRIC_META_BASE}/versions/game`)
  if (!response.ok) {
    throw localizedError('launch.fabricGameVersionsFetchFailed', { status: response.status })
  }
  const versions = (await response.json()) as FabricGameVersionEntry[]
  return new Set(versions.map((v) => v.version))
}

export interface FabricLibrary {
  name: string
  url: string
  sha1?: string
}

export interface FabricProfile {
  id: string
  inheritsFrom: string
  mainClass: string
  arguments?: { game?: string[]; jvm?: string[] }
  libraries: FabricLibrary[]
}

export async function fetchLatestStableLoaderVersion(gameVersion: string, signal?: AbortSignal): Promise<string> {
  const response = await fetch(`${FABRIC_META_BASE}/versions/loader/${gameVersion}`, { signal })
  if (!response.ok) {
    throw localizedError('launch.fabricLoaderVersionsFetchFailed', { gameVersion, status: response.status })
  }
  const versions = (await response.json()) as FabricLoaderVersionEntry[]
  const stable = versions.find((v) => v.loader.stable) ?? versions[0]
  if (!stable) {
    throw localizedError('launch.noFabricLoaderVersion', { gameVersion })
  }
  return stable.loader.version
}

export async function fetchFabricProfile(gameVersion: string, loaderVersion: string, signal?: AbortSignal): Promise<FabricProfile> {
  const response = await fetch(`${FABRIC_META_BASE}/versions/loader/${gameVersion}/${loaderVersion}/profile/json`, { signal })
  if (!response.ok) {
    throw localizedError('launch.fabricProfileFetchFailed', { gameVersion, loaderVersion, status: response.status })
  }
  return (await response.json()) as FabricProfile
}
