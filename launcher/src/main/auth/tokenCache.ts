import { app, safeStorage } from 'electron'
import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import type { MinecraftProfile } from '../../shared/types'

interface CachedAuth {
  msRefreshToken: string
  profile: MinecraftProfile
}

/**
 * What is actually on disk: the sign-in, encrypted by the operating system for this user account
 * (Electron's `safeStorage` - DPAPI on Windows). Someone who copies the file, reads the disk from
 * another system or is logged in as a different user gets nothing usable out of it. It is no
 * protection against a program that runs as the same user - that can ask the system to decrypt, just
 * like we do.
 */
interface StoredAuth {
  encrypted: string
}

function cachePath(): string {
  return join(app.getPath('userData'), 'auth.json')
}

/** Whatever the file holds - including the token-less entry {@link signOutCachedAuth} leaves behind. */
async function readStored(): Promise<CachedAuth | null> {
  try {
    const stored = JSON.parse(await readFile(cachePath(), 'utf-8')) as Partial<StoredAuth & CachedAuth>
    if (typeof stored.encrypted === 'string') {
      return JSON.parse(safeStorage.decryptString(Buffer.from(stored.encrypted, 'base64'))) as CachedAuth
    }
    // Written in plain text by a launcher up to 0.14.1 - used once more, and stored encrypted right away
    if (typeof stored.msRefreshToken !== 'string' || !stored.profile) return null
    const plain: CachedAuth = { msRefreshToken: stored.msRefreshToken, profile: stored.profile }
    await saveCachedAuth(plain).catch(() => undefined)
    return plain
  } catch {
    return null
  }
}

/** The stored sign-in, `null` when there is none or the player has logged out. */
export async function loadCachedAuth(): Promise<CachedAuth | null> {
  const stored = await readStored()
  return stored && stored.msRefreshToken ? stored : null
}

export async function saveCachedAuth(entry: CachedAuth): Promise<void> {
  await mkdir(app.getPath('userData'), { recursive: true })
  // No encryption on this system (a Linux desktop without a keyring) - plain text as before, rather
  // than asking for the Microsoft login on every start
  const stored: StoredAuth | CachedAuth = safeStorage.isEncryptionAvailable()
    ? { encrypted: safeStorage.encryptString(JSON.stringify(entry)).toString('base64') }
    : entry
  await writeFile(cachePath(), JSON.stringify(stored, null, 2), 'utf-8')
}

/**
 * Logging out: both tokens are gone, so nothing on this PC can sign in as the player any more and
 * the next start shows the login screen. Name, UUID and skin stay - that is all offline mode needs
 * (own user request: playing offline must still work after logging out).
 */
export async function signOutCachedAuth(): Promise<void> {
  const stored = await readStored()
  if (!stored) return
  await saveCachedAuth({ msRefreshToken: '', profile: { ...stored.profile, accessToken: '' } })
}

/** The player who logged out on this PC, for the login screen's "play offline" - `null` while
 * someone is signed in (offline mode then comes from the normal session restore) or nobody ever was. */
export async function loadSignedOutProfile(): Promise<MinecraftProfile | null> {
  const stored = await readStored()
  return stored && !stored.msRefreshToken ? { ...stored.profile, accessToken: '', offline: true } : null
}

/** Refreshes just the cached `profile` (e.g. after a skin upload, Phase 7) without touching the
 * cached `msRefreshToken` - a no-op if nothing's cached yet (shouldn't happen in practice, since
 * this is only ever called right after a successful authenticated API call). */
export async function updateCachedProfile(profile: MinecraftProfile): Promise<void> {
  const existing = await loadCachedAuth()
  if (!existing) return
  await saveCachedAuth({ ...existing, profile })
}
