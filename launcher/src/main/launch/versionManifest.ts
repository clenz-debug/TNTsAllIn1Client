import { MINECRAFT_VERSION } from '../../shared/types'

const VERSION_MANIFEST_URL = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'

interface VersionManifestEntry {
  id: string
  url: string
}

interface VersionManifest {
  versions: VersionManifestEntry[]
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
  downloads?: { artifact?: LibraryArtifact }
  rules?: Rule[]
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
}

export async function fetchVersionDetail(versionId: string = MINECRAFT_VERSION): Promise<VersionDetail> {
  const manifestResponse = await fetch(VERSION_MANIFEST_URL)
  if (!manifestResponse.ok) {
    throw new Error(`Failed to fetch version manifest: ${manifestResponse.status}`)
  }
  const manifest = (await manifestResponse.json()) as VersionManifest
  const entry = manifest.versions.find((v) => v.id === versionId)
  if (!entry) {
    throw new Error(`Minecraft version ${versionId} not found in version manifest.`)
  }

  const detailResponse = await fetch(entry.url)
  if (!detailResponse.ok) {
    throw new Error(`Failed to fetch version detail for ${versionId}: ${detailResponse.status}`)
  }
  return (await detailResponse.json()) as VersionDetail
}
