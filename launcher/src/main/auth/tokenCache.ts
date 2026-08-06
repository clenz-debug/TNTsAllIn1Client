import { app } from 'electron'
import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import type { MinecraftProfile } from '../../shared/types'

interface CachedAuth {
  msRefreshToken: string
  profile: MinecraftProfile
}

function cachePath(): string {
  return join(app.getPath('userData'), 'auth.json')
}

export async function loadCachedAuth(): Promise<CachedAuth | null> {
  try {
    const raw = await readFile(cachePath(), 'utf-8')
    return JSON.parse(raw) as CachedAuth
  } catch {
    return null
  }
}

export async function saveCachedAuth(entry: CachedAuth): Promise<void> {
  await mkdir(app.getPath('userData'), { recursive: true })
  await writeFile(cachePath(), JSON.stringify(entry, null, 2), 'utf-8')
}

/** Refreshes just the cached `profile` (e.g. after a skin upload, Phase 7) without touching the
 * cached `msRefreshToken` - a no-op if nothing's cached yet (shouldn't happen in practice, since
 * this is only ever called right after a successful authenticated API call). */
export async function updateCachedProfile(profile: MinecraftProfile): Promise<void> {
  const existing = await loadCachedAuth()
  if (!existing) return
  await saveCachedAuth({ ...existing, profile })
}
