import { createHash } from 'node:crypto'
import { mkdir, readFile, readdir, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { MODRINTH_SEARCH_PAGE_SIZE, type ModrinthSearchPage, type ModrinthSortIndex } from '../../shared/types'
import { fetchTextureDataUri } from '../auth/skinApi'
import { downloadAndVerifySha1 } from '../downloadVerify'
import { instanceDir } from './installer'
import { listCustomMods } from './modsManager'
import { bundledModsDir } from './resourcePaths'

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

interface ModrinthDependency {
  project_id: string | null
  dependency_type: 'required' | 'optional' | 'incompatible' | 'embedded'
}

interface ModrinthVersion {
  version_type: 'release' | 'beta' | 'alpha'
  files: ModrinthVersionFile[]
  dependencies: ModrinthDependency[]
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

/** Newest compatible version of one project for `gameVersion` + Fabric - shared by both the
 * top-level install and dependency resolution below, so a required dependency is picked the exact
 * same "prefer stable" way (newest `release`, else newest of any type) as the mod the user actually
 * asked for, same convention already used for Fabric Loader version selection in `fabricMeta.ts`. */
async function resolveNewestCompatibleVersion(projectId: string, gameVersion: string): Promise<ModrinthVersion | null> {
  const versionsUrl = `${MODRINTH_API_BASE}/project/${projectId}/version?loaders=${encodeURIComponent(
    JSON.stringify(['fabric'])
  )}&game_versions=${encodeURIComponent(JSON.stringify([gameVersion]))}`
  const versionsResponse = await fetch(versionsUrl)
  if (!versionsResponse.ok) {
    throw new Error(`Konnte Modrinth-Versionen nicht laden (${versionsResponse.status})`)
  }
  const versions = (await versionsResponse.json()) as ModrinthVersion[]
  return versions.find((v) => v.version_type === 'release') ?? versions[0] ?? null
}

/** Project title for an error message naming a specific unresolvable dependency - best-effort only,
 * falls back to the raw id if the lookup itself fails (never lets a cosmetic detail hide the actual
 * error). */
async function projectTitle(projectId: string): Promise<string> {
  try {
    const response = await fetch(`${MODRINTH_API_BASE}/project/${projectId}`)
    if (!response.ok) return projectId
    const project = (await response.json()) as { title?: string }
    return project.title ?? projectId
  } catch {
    return projectId
  }
}

/** Modrinth project ids of every `.jar` actually sitting in `dir` right now, resolved from the real
 * files' SHA-1 hashes via Modrinth's batch `/version_files` lookup rather than trusted metadata -
 * works equally for a mods-bundle folder or an instance's `game/mods`, and for a Minecraft version
 * with no `mod-bundle-manifest.json` entry yet (tried keying off the manifest first - broke exactly
 * for the version most worth testing this on: a version only staged locally during development has
 * no manifest entry at all yet, per `mod-bundle-release-runbook.md`'s "only push once everything is
 * finished" rule). A jar Modrinth doesn't recognize (hand-built, removed from Modrinth, ...) is
 * silently left out rather than failing the whole lookup. */
async function resolveProjectIdsInDir(dir: string): Promise<Set<string>> {
  let fileNames: string[]
  try {
    fileNames = (await readdir(dir)).filter((name) => name.endsWith('.jar'))
  } catch {
    return new Set()
  }
  if (fileNames.length === 0) return new Set()

  const hashes = await Promise.all(
    fileNames.map(async (fileName) => {
      const buffer = await readFile(join(dir, fileName))
      return createHash('sha1').update(buffer).digest('hex')
    })
  )

  const response = await fetch(`${MODRINTH_API_BASE}/version_files`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ hashes, algorithm: 'sha1' })
  })
  if (!response.ok) {
    throw new Error(`Modrinth-Hash-Lookup fehlgeschlagen (${response.status})`)
  }
  const versionsByHash = (await response.json()) as Record<string, ModrinthVersion & { project_id: string }>
  return new Set(Object.values(versionsByHash).map((v) => v.project_id))
}

/**
 * Modrinth project ids of every mod jar actually sitting in `mods-bundle/<versionId>/` right now
 * (toggleable or always-on alike - see `modsManager.ts#isAlwaysEnabledBundledMod`), so the Mods
 * screen's search results can show "already bundled" instead of a misleading plain "Installieren"
 * button for e.g. Sodium, which would just download a redundant second copy as a custom mod.
 */
export async function getBundledModProjectIds(versionId: string): Promise<string[]> {
  return [...(await resolveProjectIdsInDir(bundledModsDir(versionId)))]
}

/**
 * Installs a mod's newest compatible release into an instance's `game/mods` folder, pulling in
 * every `required` Modrinth dependency (transitively) that isn't already satisfied by a bundled mod
 * or something already sitting in `game/mods` - own user request, after "Over The Limits for Female
 * Gender Mod" (needs fabric-language-kotlin, YACL, ModMenu and its own base mod) failed to launch
 * with Fabric Loader's "Incompatible mods found!" screen for exactly this reason. `optional`/
 * `incompatible` dependencies are never auto-installed (the user didn't ask for extras, and
 * resolving a real incompatibility is out of scope here); `embedded` ones are skipped too - their
 * code already ships inside the parent jar, installing them separately would just duplicate classes.
 *
 * Two phases on purpose: every project in the dependency tree is resolved to a concrete version
 * *before* anything is downloaded or written, so a dependency with no build for this Minecraft
 * version fails the whole install cleanly instead of leaving a mod half-installed without something
 * it needs - the exact broken state this feature exists to prevent in the first place.
 */
export async function installModrinthMod(instanceId: string, projectId: string, gameVersion: string): Promise<string[]> {
  const modsDir = join(instanceDir(instanceId), 'game', 'mods')
  await mkdir(modsDir, { recursive: true })

  const alreadySatisfied = new Set([
    ...(await resolveProjectIdsInDir(bundledModsDir(gameVersion))),
    ...(await resolveProjectIdsInDir(modsDir))
  ])

  const toResolve = [projectId]
  const resolved = new Map<string, ModrinthVersion>()
  while (toResolve.length > 0) {
    const currentProjectId = toResolve.shift()!
    if (resolved.has(currentProjectId) || alreadySatisfied.has(currentProjectId)) continue

    const chosen = await resolveNewestCompatibleVersion(currentProjectId, gameVersion)
    if (!chosen) {
      const label = currentProjectId === projectId ? '' : `Abhängigkeit "${await projectTitle(currentProjectId)}": `
      throw new Error(`${label}Kein passender Fabric-Build für Minecraft ${gameVersion} gefunden.`)
    }
    resolved.set(currentProjectId, chosen)

    for (const dep of chosen.dependencies) {
      if (dep.dependency_type === 'required' && dep.project_id && !resolved.has(dep.project_id) && !alreadySatisfied.has(dep.project_id)) {
        toResolve.push(dep.project_id)
      }
    }
  }

  for (const version of resolved.values()) {
    const file = version.files.find((f) => f.primary) ?? version.files[0]
    if (!file) {
      throw new Error('Eine benötigte Modrinth-Version hat keine herunterladbare Datei.')
    }
    const buffer = await downloadAndVerifySha1(file.url, file.hashes.sha1, file.filename)
    await writeFile(join(modsDir, file.filename), buffer)
  }

  return listCustomMods(instanceId)
}
