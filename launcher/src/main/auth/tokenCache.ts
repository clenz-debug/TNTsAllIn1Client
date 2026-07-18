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
