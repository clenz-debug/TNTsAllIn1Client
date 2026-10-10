import { createHash } from 'node:crypto'

/**
 * Proof of ownership, in two ways - both end with Mojang telling us which player it is, so a cape or
 * a friends list is only ever touched under *that* UUID and no shared secret sits in the launcher:
 *
 *  - {@link hasJoined} (current launchers): the launcher tells Mojang "I am joining server <id>"
 *    with an id we handed out, and we ask Mojang whether that player did. The player's Minecraft
 *    access token never leaves their PC - see `sessions.ts`.
 *  - {@link verifyAccessToken} (launchers up to 0.14.1): the launcher sends the access token itself
 *    and we ask Mojang whose it is. Kept only until those launchers have updated.
 */
const PROFILE_URL = 'https://api.minecraftservices.com/minecraft/profile'
const HAS_JOINED_URL = 'https://sessionserver.mojang.com/session/minecraft/hasJoined'
const CACHE_MS = 5 * 60 * 1000
/** A token Mojang turned down is not asked about again for this long. */
const REJECTED_CACHE_MS = 60 * 1000
const TIMEOUT_MS = 10_000

export interface VerifiedProfile {
  /** UUID without dashes, as Mojang returns it. */
  id: string
  name: string
}

/** Thrown when the caller's budget for Mojang lookups is used up - see `allowLookup`. */
export class LookupRefused extends Error {}

function toProfile(data: { id?: unknown; name?: unknown }, source: string): VerifiedProfile {
  if (typeof data.id !== 'string' || !/^[0-9a-f]{32}$/i.test(data.id) || typeof data.name !== 'string' || data.name.length === 0 || data.name.length > 32) {
    throw new Error(`Mojang ${source} returned an unexpected body`)
  }
  return { id: data.id.toLowerCase(), name: data.name }
}

/**
 * Whether the player called `name` has just told Mojang that they join the server `serverId` -
 * which only the owner of that account can do. `null` if not.
 */
export async function hasJoined(name: string, serverId: string): Promise<VerifiedProfile | null> {
  const url = `${HAS_JOINED_URL}?username=${encodeURIComponent(name)}&serverId=${encodeURIComponent(serverId)}`
  const response = await fetch(url, { signal: AbortSignal.timeout(TIMEOUT_MS) })
  if (response.status === 204 || response.status === 403 || response.status === 404) return null
  if (!response.ok) throw new Error(`Mojang hasJoined failed: HTTP ${response.status}`)
  return toProfile((await response.json()) as { id?: unknown; name?: unknown }, 'hasJoined')
}

/** Keyed by a hash of the token, so raw tokens never sit in memory longer than one request. */
const cache = new Map<string, { profile: VerifiedProfile | null; expires: number }>()

/**
 * Whose Minecraft access token this is, `null` if Mojang does not accept it. `allowLookup` is asked
 * before Mojang is contacted (not for cached answers) and makes this throw {@link LookupRefused}
 * when it says no - otherwise anyone could have us ask Mojang about made-up tokens until Mojang
 * stops answering us at all.
 */
export async function verifyAccessToken(token: string, allowLookup: () => boolean): Promise<VerifiedProfile | null> {
  const key = createHash('sha256').update(token).digest('hex')
  const cached = cache.get(key)
  if (cached && cached.expires > Date.now()) return cached.profile
  if (!allowLookup()) throw new LookupRefused()

  const response = await fetch(PROFILE_URL, {
    headers: { Authorization: `Bearer ${token}` },
    signal: AbortSignal.timeout(TIMEOUT_MS)
  })
  if (response.status === 401 || response.status === 403 || response.status === 404) {
    cache.set(key, { profile: null, expires: Date.now() + REJECTED_CACHE_MS })
    return null
  }
  if (!response.ok) throw new Error(`Mojang profile lookup failed: HTTP ${response.status}`)

  const profile = toProfile((await response.json()) as { id?: unknown; name?: unknown }, 'profile lookup')
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
