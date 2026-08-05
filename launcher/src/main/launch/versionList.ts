import type { GameVersionSummary, GameVersionType } from '../../shared/types'
import { fetchFabricGameVersions } from './fabricMeta'
import { fetchManifestVersions } from './versionManifest'

/** Versions-picker data source (Phase 6a): intersects the full Mojang manifest with the set of
 * game versions Fabric actually supports, so nothing unlaunchable-through-Fabric ever shows up
 * in the dropdown. Only `release`/`snapshot` entries are kept — old_beta/old_alpha predate
 * Fabric entirely and never appear in Fabric's set anyway, filtered here too for clarity. Sorted
 * newest-first, same order as Mojang's own manifest. */
export async function fetchAvailableVersions(): Promise<GameVersionSummary[]> {
  const [manifestVersions, fabricVersions] = await Promise.all([
    fetchManifestVersions(),
    fetchFabricGameVersions()
  ])

  return manifestVersions
    .filter((v) => (v.type === 'release' || v.type === 'snapshot') && fabricVersions.has(v.id))
    .map((v) => ({ id: v.id, type: v.type as GameVersionType, releaseTime: v.releaseTime }))
}
