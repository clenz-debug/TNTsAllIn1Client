import type { AuthProgressEvent, Language, MinecraftProfile } from '../../shared/types'
import { loginWithMicrosoft, refreshMsToken } from './msOAuth'
import { loginToXboxLive } from './xboxLive'
import { completeMinecraftLogin } from './minecraftAuth'
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

/** Silent re-login on startup using the cached MS refresh token. Returns null (never throws)
 * so the caller can just fall back to showing the login screen. */
export async function tryRestoreSession(): Promise<MinecraftProfile | null> {
  const cached = await loadCachedAuth()
  if (!cached) return null

  try {
    const msTokens = await refreshMsToken(cached.msRefreshToken)
    const xsts = await loginToXboxLive(msTokens.accessToken)
    const profile = await completeMinecraftLogin(xsts)
    await saveCachedAuth({ msRefreshToken: msTokens.refreshToken, profile })
    return profile
  } catch (error) {
    console.warn('[auth] Failed to restore session, showing login screen instead:', error)
    return null
  }
}
