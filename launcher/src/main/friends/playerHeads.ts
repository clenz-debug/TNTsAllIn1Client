import { fetchTextureDataUri } from '../auth/skinApi'

/**
 * A friend's skin texture for the Friends list's head icons (`PlayerHeadIcon` crops the face). Any
 * player's current skin is public: Mojang's session server returns it for a UUID, base64-encoded in
 * the `textures` property. Kept in memory for a while so the 20 s friends refresh doesn't refetch.
 */

const SESSION_PROFILE_URL = 'https://sessionserver.mojang.com/session/minecraft/profile/'
const CACHE_MS = 30 * 60 * 1000

const cache = new Map<string, { value: Promise<string | null>; expires: number }>()

async function fetchSkin(uuid: string): Promise<string | null> {
  const response = await fetch(`${SESSION_PROFILE_URL}${uuid}`, { signal: AbortSignal.timeout(10_000) })
  if (!response.ok) return null
  const profile = (await response.json()) as { properties?: Array<{ name: string; value: string }> }
  const textures = profile.properties?.find((property) => property.name === 'textures')
  if (!textures) return null
  const decoded = JSON.parse(Buffer.from(textures.value, 'base64').toString('utf8')) as { textures?: { SKIN?: { url?: string } } }
  const url = decoded.textures?.SKIN?.url
  return url ? fetchTextureDataUri(url) : null
}

/** `null` for a player without a custom skin (the list shows a placeholder then) or on any error. */
export function getPlayerSkin(uuid: string): Promise<string | null> {
  const key = uuid.replace(/-/g, '').toLowerCase()
  const cached = cache.get(key)
  if (cached && cached.expires > Date.now()) return cached.value
  const value = fetchSkin(key).catch(() => null)
  cache.set(key, { value, expires: Date.now() + CACHE_MS })
  return value
}
