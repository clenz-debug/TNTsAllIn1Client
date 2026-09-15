import { mkdir, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { MODRINTH_SEARCH_PAGE_SIZE, type ModrinthSearchPage, type ModrinthSortIndex } from '../../shared/types'
import { fetchTextureDataUri } from '../auth/skinApi'
import { downloadAndVerifySha1 } from '../downloadVerify'
import { instanceDir } from './installer'
import { listCustomMods } from './modsManager'

const MODRINTH_API_BASE = 'https://api.modrinth.com/v2'

interface ModrinthSearchHit {
  project_id: string
  title: string
  description: string
  downloads: number
  icon_url: string | null
}

interface ModrinthSearchResponse {
  hits: ModrinthSearchHit[]
  total_hits: number
}

interface ModrinthVersionFile {
  hashes: { sha1: string }
  url: string
  filename: string
  primary: boolean
}

interface ModrinthVersion {
  version_type: 'release' | 'beta' | 'alpha'
  files: ModrinthVersionFile[]
}

/**
 * Searches (or, with an empty `query`, browses) Modrinth's mod catalog, scoped to Fabric + the
 * given game version - own user request, modeled on how Modrinth's own app lets you browse mods
 * by category/popularity, not just search by exact name. Request shape verified directly against
 * the real API before writing this (not guessed): `facets` is a JSON-encoded array-of-arrays, each
 * inner array OR'd together, every inner array AND'd with the others -
 * `[["project_type:mod"],["categories:fabric"],["versions:<gameVersion>"]]` means "mod AND fabric
 * AND this exact game version". An empty `query` is valid and simply drops relevance-to-text
 * scoring, which is what makes plain browsing (sorted by downloads/newest/etc.) work.
 */
export async function searchModrinthMods(
  query: string,
  gameVersion: string,
  offset: number,
  sortIndex: ModrinthSortIndex
): Promise<ModrinthSearchPage> {
  const facets = JSON.stringify([['project_type:mod'], ['categories:fabric'], [`versions:${gameVersion}`]])
  const url =
    `${MODRINTH_API_BASE}/search?query=${encodeURIComponent(query)}&facets=${encodeURIComponent(facets)}` +
    `&index=${encodeURIComponent(sortIndex)}&offset=${offset}&limit=${MODRINTH_SEARCH_PAGE_SIZE}`
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`Modrinth-Suche fehlgeschlagen (${response.status})`)
  }
  const { hits, total_hits: totalHits } = (await response.json()) as ModrinthSearchResponse

  // Icons are fetched here (not left as a raw `icon_url` for the renderer to load directly) for
  // the same CSP reason `skinApi.ts#fetchTextureDataUri` already exists - the renderer only
  // allows `img-src 'self' data:'`. A broken/missing icon isn't worth failing the whole search
  // over, so a single icon fetch failure just leaves that one result without a picture.
  const results = await Promise.all(
    hits.map(async (hit) => ({
      projectId: hit.project_id,
      title: hit.title,
      description: hit.description,
      downloads: hit.downloads,
      iconDataUri: hit.icon_url ? await fetchTextureDataUri(hit.icon_url).catch(() => null) : null
    }))
  )
  return { results, totalHits }
}

/**
 * Installs a mod's newest compatible release into an instance's `game/mods` folder - reuses
 * {@link listCustomMods} to report the refreshed list, since a freshly-downloaded jar sitting
 * there is indistinguishable from one added by hand (same filename-set-difference detection).
 *
 * Deliberately does **not** resolve `dependencies` - most Fabric mods only depend on Fabric API,
 * which this launcher already bundles unconditionally; a mod needing something else can be
 * searched for and installed the same way, by hand, same as the roadmap's own "keep v1 small"
 * pattern elsewhere in this project. Also deliberately no version picker - always the newest
 * `release` (falling back to the newest of any type if no release exists), same "prefer stable"
 * convention already used for Fabric Loader version selection in `fabricMeta.ts`.
 */
export async function installModrinthMod(instanceId: string, projectId: string, gameVersion: string): Promise<string[]> {
  const versionsUrl = `${MODRINTH_API_BASE}/project/${projectId}/version?loaders=${encodeURIComponent(
    JSON.stringify(['fabric'])
  )}&game_versions=${encodeURIComponent(JSON.stringify([gameVersion]))}`
  const versionsResponse = await fetch(versionsUrl)
  if (!versionsResponse.ok) {
    throw new Error(`Konnte Modrinth-Versionen nicht laden (${versionsResponse.status})`)
  }
  const versions = (await versionsResponse.json()) as ModrinthVersion[]
  const chosen = versions.find((v) => v.version_type === 'release') ?? versions[0]
  if (!chosen) {
    throw new Error('Kein passender Fabric-Build für diese Minecraft-Version gefunden.')
  }
  const file = chosen.files.find((f) => f.primary) ?? chosen.files[0]
  if (!file) {
    throw new Error('Diese Modrinth-Version hat keine herunterladbare Datei.')
  }

  const buffer = await downloadAndVerifySha1(file.url, file.hashes.sha1, file.filename)

  const modsDir = join(instanceDir(instanceId), 'game', 'mods')
  await mkdir(modsDir, { recursive: true })
  await writeFile(join(modsDir, file.filename), buffer)

  return listCustomMods(instanceId)
}
