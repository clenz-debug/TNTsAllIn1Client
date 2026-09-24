import { createHash } from 'node:crypto'

/**
 * Proof of ownership: the launcher sends the player's own Minecraft access token (the same one
 * it already uses for Mojang's skin upload), and we ask Mojang whose token it is. The cape is then
 * only ever written under *that* UUID - no shared secret in the launcher, and nobody can touch
 * someone else's cape (the gap the old Backblaze design had to accept).
 */
const PROFILE_URL = 'https://api.minecraftservices.com/minecraft/profile'
const CACHE_MS = 5 * 60 * 1000

export interface VerifiedProfile {
  /** UUID without dashes, as Mojang returns it. */
  id: string
  name: string
}

/** Keyed by a hash of the token, so raw tokens never sit in memory longer than one request. */
const cache = new Map<string, { profile: VerifiedProfile; expires: number }>()

export async function verifyAccessToken(token: string): Promise<VerifiedProfile | null> {
  const key = createHash('sha256').update(token).digest('hex')
  const cached = cache.get(key)
  if (cached && cached.expires > Date.now()) return cached.profile

  const response = await fetch(PROFILE_URL, {
    headers: { Authorization: `Bearer ${token}` },
    signal: AbortSignal.timeout(10_000)
  })
  if (response.status === 401 || response.status === 403 || response.status === 404) return null
  if (!response.ok) throw new Error(`Mojang profile lookup failed: HTTP ${response.status}`)

  const data = (await response.json()) as { id?: unknown; name?: unknown }
  if (typeof data.id !== 'string' || !/^[0-9a-f]{32}$/i.test(data.id) || typeof data.name !== 'string') {
    throw new Error('Mojang profile lookup returned an unexpected body')
  }
  const profile = { id: data.id.toLowerCase(), name: data.name }
  cache.set(key, { profile, expires: Date.now() + CACHE_MS })
  return profile
}

/** Drops expired entries - called periodically so the cache can't grow without bound. */
export function pruneTokenCache(): void {
  const now = Date.now()
  for (const [key, entry] of cache) {
    if (entry.expires <= now) cache.delete(key)
  }
}
