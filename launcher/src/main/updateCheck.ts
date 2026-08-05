import { app } from 'electron'
import type { UpdateCheckResult } from '../shared/types'

/**
 * Self-hosted manifest (Phase 6d) - `launcher/update-manifest.json` in this same repo, fetched via
 * raw.githubusercontent.com rather than needing an actual GitHub Release to exist yet (there
 * isn't one). Must be bumped by hand alongside `launcher/package.json`'s `version` field whenever
 * a release-worthy version lands - nothing automated keeps them in sync, same "two lists, kept in
 * sync by hand" situation as the Credits screens (see their own comments).
 */
const UPDATE_MANIFEST_URL =
  'https://raw.githubusercontent.com/clenz-debug/TNTsAllIn1Client/main/launcher/update-manifest.json'

interface UpdateManifest {
  latestVersion: string
  releaseNotesUrl?: string
}

/** Compares plain dot-separated numeric versions ("0.1.0" vs "0.2.0") - this project's simple
 * major.minor.patch scheme doesn't need a semver dependency for three numbers. Missing trailing
 * parts count as 0 (`"0.2"` > `"0.1.5"` is false, `"0.2.0"` > `"0.1.5"` is true). */
function isNewer(latest: string, current: string): boolean {
  const latestParts = latest.split('.').map(Number)
  const currentParts = current.split('.').map(Number)
  const length = Math.max(latestParts.length, currentParts.length)
  for (let i = 0; i < length; i++) {
    const l = latestParts[i] ?? 0
    const c = currentParts[i] ?? 0
    if (l !== c) return l > c
  }
  return false
}

export async function checkForUpdate(): Promise<UpdateCheckResult> {
  const currentVersion = app.getVersion()
  const response = await fetch(UPDATE_MANIFEST_URL)
  if (!response.ok) {
    throw new Error(`Failed to fetch update manifest: ${response.status}`)
  }
  const manifest = (await response.json()) as UpdateManifest
  return {
    currentVersion,
    latestVersion: manifest.latestVersion,
    updateAvailable: isNewer(manifest.latestVersion, currentVersion),
    releaseNotesUrl: manifest.releaseNotesUrl
  }
}
