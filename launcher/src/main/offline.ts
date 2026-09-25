import { app } from 'electron'
import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { join } from 'node:path'

/**
 * Offline mode (own user request: start and play without internet, as long as the account was
 * logged in and the instance launched online once before). Two building blocks live here:
 * telling a network failure apart from a real error, and a small on-disk cache for the metadata
 * JSON a launch needs (Mojang version manifest/details, asset index, Fabric meta, Java runtime
 * manifest) - the actual game files are already cached by `downloader.ts`, which skips every file
 * that's present with the right hash without touching the network.
 */

/** A fetch that never reached the server - no connection, DNS failure, refused, timed out. HTTP
 * error statuses are *not* network errors: the server answered, it just said no. */
export function isNetworkError(err: unknown): boolean {
  if (!(err instanceof Error)) return false
  if (err.name === 'TimeoutError') return true
  return err instanceof TypeError && err.message === 'fetch failed'
}

/** Set for the duration of a launch the renderer started in offline mode - {@link cachedJson} then
 * reads straight from the cache instead of first waiting for each request to fail. */
let preferCache = false

export function setPreferCache(value: boolean): void {
  preferCache = value
}

/** Next to `auth.json`/`launcher-settings.json` rather than under the movable data root: it's tiny,
 * and keeping it out of `RELOCATABLE_SUBDIRS` means a storage move never has to care about it. */
function cachePath(key: string): string {
  return join(app.getPath('userData'), 'meta-cache', `${key.replace(/[^a-zA-Z0-9._-]/g, '_')}.json`)
}

async function readCache<T>(key: string): Promise<T | null> {
  try {
    return JSON.parse(await readFile(cachePath(key), 'utf8')) as T
  } catch {
    return null
  }
}

/**
 * Fetches JSON through `fetcher` and keeps the last good answer on disk under `key`. If the fetch
 * fails for any reason other than a deliberate cancel (offline, server down, HTTP error), the
 * cached copy is used instead; only with no cached copy the original error is thrown. In
 * {@link setPreferCache} mode the cache is tried first and the network only as a fallback.
 */
export async function cachedJson<T>(key: string, fetcher: () => Promise<T>, signal?: AbortSignal): Promise<T> {
  if (preferCache) {
    const cached = await readCache<T>(key)
    if (cached !== null) return cached
  }
  try {
    const fresh = await fetcher()
    await mkdir(join(app.getPath('userData'), 'meta-cache'), { recursive: true })
    await writeFile(cachePath(key), JSON.stringify(fresh), 'utf8').catch(() => undefined)
    return fresh
  } catch (err) {
    if (signal?.aborted) throw err
    const cached = await readCache<T>(key)
    if (cached !== null) return cached
    throw err
  }
}
