const FABRIC_META_BASE = 'https://meta.fabricmc.net/v2'

interface FabricLoaderVersionEntry {
  loader: { version: string; stable: boolean }
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

export async function fetchLatestStableLoaderVersion(gameVersion: string): Promise<string> {
  const response = await fetch(`${FABRIC_META_BASE}/versions/loader/${gameVersion}`)
  if (!response.ok) {
    throw new Error(`Failed to fetch Fabric loader versions for ${gameVersion}: ${response.status}`)
  }
  const versions = (await response.json()) as FabricLoaderVersionEntry[]
  const stable = versions.find((v) => v.loader.stable) ?? versions[0]
  if (!stable) {
    throw new Error(`No Fabric loader version available for Minecraft ${gameVersion}`)
  }
  return stable.loader.version
}

export async function fetchFabricProfile(gameVersion: string, loaderVersion: string): Promise<FabricProfile> {
  const response = await fetch(`${FABRIC_META_BASE}/versions/loader/${gameVersion}/${loaderVersion}/profile/json`)
  if (!response.ok) {
    throw new Error(`Failed to fetch Fabric profile for ${gameVersion}/${loaderVersion}: ${response.status}`)
  }
  return (await response.json()) as FabricProfile
}
