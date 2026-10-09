import type { AuthProgressEvent, Language, MinecraftProfile } from '../../shared/types'
import { loginWithMicrosoft, refreshMsToken } from './msOAuth'
import { loginToXboxLive } from './xboxLive'
import { completeMinecraftLogin, isRateLimited, profileForToken } from './minecraftAuth'
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

/** A cached Minecraft access token is used again while it is valid for at least this long - a game
 * started with it must not lose its session mid-play. Mojang issues them for 24 hours. */
const REUSE_TOKEN_MIN_REMAINING_MS = 12 * 60 * 60 * 1000

/** When a Minecraft access token (a JWT) runs out, 0 if it cannot be read. */
function accessTokenExpiry(accessToken: string): number {
  try {
    const payload = JSON.parse(Buffer.from(accessToken.split('.')[1], 'base64url').toString('utf8')) as { exp?: unknown }
    return typeof payload.exp === 'number' ? payload.exp * 1000 : 0
  } catch {
    return 0
  }
}

/** The session behind the cached access token, as long as that token still has most of its life
 * left: Mojang allows only a few `login_with_xbox` calls per account in a short time and answers
 * 429 after that, so a full login on every launcher start locks out someone who restarts often. */
async function reuseSession(cached: { msRefreshToken: string; profile: MinecraftProfile }): Promise<MinecraftProfile | null> {
  if (accessTokenExpiry(cached.profile.accessToken) - Date.now() < REUSE_TOKEN_MIN_REMAINING_MS) return null
  try {
    const profile = await profileForToken(cached.profile.accessToken)
    await saveCachedAuth({ msRefreshToken: cached.msRefreshToken, profile })
    return profile
  } catch (error) {
    if (isNetworkError(error)) throw error
    return null
  }
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
    return await Promise.race([reuseSession(cached).then((reused) => reused ?? refreshSession(cached.msRefreshToken)), timeout])
  } catch (error) {
    if (isNetworkError(error)) {
      console.warn('[auth] No internet, continuing offline with the cached profile:', error)
      return { ...cached.profile, offline: true }
    }
    if (isRateLimited(error) && accessTokenExpiry(cached.profile.accessToken) > Date.now()) {
      console.warn('[auth] Mojang limits logins right now, continuing with the cached session:', error)
      return cached.profile
    }
    console.warn('[auth] Failed to restore session, showing login screen instead:', error)
    return null
  }
}
