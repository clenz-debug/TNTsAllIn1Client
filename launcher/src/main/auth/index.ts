import type { AuthProgressEvent, Language, MinecraftProfile } from '../../shared/types'
import { loginWithMicrosoft, refreshMsToken } from './msOAuth'
import { loginToXboxLive } from './xboxLive'
import { completeMinecraftLogin } from './minecraftAuth'
import { isNetworkError } from '../offline'
import { loadCachedAuth, saveCachedAuth } from './tokenCache'

export type AuthProgressCallback = (event: AuthProgressEvent) => void

export async function performLogin(onProgress: AuthProgressCallback, language: Language): Promise<MinecraftProfile> {
  onProgress({ step: 'ms-oauth', message: 'Öffne Microsoft-Login im Browser…' })
  const msTokens = await loginWithMicrosoft(language)

  onProgress({ step: 'xbox-live', message: 'Melde bei Xbox Live an…' })
  const xsts = await loginToXboxLive(msTokens.accessToken)

  onProgress({ step: 'minecraft-login', message: 'Prüfe Minecraft-Zugriff…' })
  const profile = await completeMinecraftLogin(xsts)

  await saveCachedAuth({ msRefreshToken: msTokens.refreshToken, profile })

  onProgress({ step: 'done', message: `Angemeldet als ${profile.name}.` })
  return profile
}

/** How long the silent re-login may take before it counts as "no internet" - a Wi-Fi without a
 * working uplink can leave requests hanging far longer than a clean "no connection" error would. */
const RESTORE_TIMEOUT_MS = 15_000

async function refreshSession(msRefreshToken: string): Promise<MinecraftProfile> {
  const msTokens = await refreshMsToken(msRefreshToken)
  const xsts = await loginToXboxLive(msTokens.accessToken)
  const profile = await completeMinecraftLogin(xsts)
  await saveCachedAuth({ msRefreshToken: msTokens.refreshToken, profile })
  return profile
}

/** Silent re-login on startup using the cached MS refresh token. Returns null (never throws)
 * so the caller can just fall back to showing the login screen. Without internet it returns the
 * last cached profile marked `offline` instead (own user request: offline mode) - only for a
 * network failure, never when Microsoft/Mojang actually rejected the login. */
export async function tryRestoreSession(): Promise<MinecraftProfile | null> {
  const cached = await loadCachedAuth()
  if (!cached) return null

  try {
    const timeout = new Promise<never>((_, reject) =>
      setTimeout(() => reject(Object.assign(new Error('Session restore timed out'), { name: 'TimeoutError' })), RESTORE_TIMEOUT_MS)
    )
    return await Promise.race([refreshSession(cached.msRefreshToken), timeout])
  } catch (error) {
    if (isNetworkError(error)) {
      console.warn('[auth] No internet, continuing offline with the cached profile:', error)
      return { ...cached.profile, offline: true }
    }
    console.warn('[auth] Failed to restore session, showing login screen instead:', error)
    return null
  }
}
