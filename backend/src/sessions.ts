import { createHash, randomBytes } from 'node:crypto'
import type { VerifiedProfile } from './mojang.js'

/**
 * Sign-in without the player's Minecraft access token ever reaching us - the same handshake a
 * Minecraft server uses to check who is joining:
 *
 *  1. `POST /auth/challenge` - we hand out a random, single-use server id.
 *  2. The launcher tells Mojang "this account joins server <id>" (Mojang's `join` call). That needs
 *     the access token, which goes to Mojang only.
 *  3. `POST /auth/session` with the player name and the id - we ask Mojang whether that player
 *     joined that id (`hasJoined`, see `mojang.ts`) and answer with a session token of our own.
 *
 * Our session token is worth something on this service only: whoever reads it here (or on the way)
 * can change that player's cape and friends list until it runs out, and nothing else. It cannot be
 * used at Mojang or Microsoft.
 *
 * Everything is kept in memory: nothing that signs anyone in is ever written to disk, and a restart
 * of the service just makes the launchers repeat the handshake.
 */

/** The launcher goes through steps 2 and 3 right away. */
const CHALLENGE_TTL_MS = 60 * 1000
export const SESSION_TTL_MS = 12 * 60 * 60 * 1000
/** Handing out challenges is rate-limited per address; this caps what all addresses together can pile up. */
const MAX_CHALLENGES = 10_000
/** The same account on a few PCs at once - the oldest session goes when there are more. */
const MAX_SESSIONS_PER_PLAYER = 5

/** Tells our tokens apart from the Minecraft access tokens old launchers send (those are JWTs). */
export const SESSION_TOKEN_PREFIX = 'tnt_'
export const SERVER_ID_PATTERN = /^[0-9a-f]{40}$/

const challenges = new Map<string, number>()
/** Keyed by a hash of the token: even a memory dump holds nothing that could be sent as a token. */
const sessions = new Map<string, { profile: VerifiedProfile; expires: number }>()

function hash(token: string): string {
  return createHash('sha256').update(token).digest('hex')
}

/** A fresh server id for step 1, `null` while too many are open. */
export function createChallenge(): string | null {
  if (challenges.size >= MAX_CHALLENGES) return null
  const serverId = randomBytes(20).toString('hex')
  challenges.set(serverId, Date.now() + CHALLENGE_TTL_MS)
  return serverId
}

/**
 * Whether we handed out this server id and it is still open - and uses it up either way. Only ids
 * from us count: a Minecraft server the player joins also learns a server id Mojang would confirm,
 * and must not be able to sign in here with it.
 */
export function takeChallenge(serverId: string): boolean {
  const expires = challenges.get(serverId)
  challenges.delete(serverId)
  return expires !== undefined && expires > Date.now()
}

export function createSession(profile: VerifiedProfile): { token: string; expires: number } {
  const own = [...sessions].filter(([, session]) => session.profile.id === profile.id)
  // Map order is insertion order, so the first ones are the oldest
  for (const [key] of own.slice(0, Math.max(0, own.length - MAX_SESSIONS_PER_PLAYER + 1))) sessions.delete(key)

  const token = SESSION_TOKEN_PREFIX + randomBytes(32).toString('base64url')
  const expires = Date.now() + SESSION_TTL_MS
  sessions.set(hash(token), { profile, expires })
  return { token, expires }
}

export function sessionProfile(token: string): VerifiedProfile | null {
  const session = sessions.get(hash(token))
  return session && session.expires > Date.now() ? session.profile : null
}

/** Sign-out: the token is worthless from now on. */
export function endSession(token: string): void {
  sessions.delete(hash(token))
}

/** "Delete my data": signed out everywhere. */
export function endSessionsOf(uuid: string): void {
  for (const [key, session] of sessions) {
    if (session.profile.id === uuid) sessions.delete(key)
  }
}

/** Drops what has run out - called periodically so neither map can grow without bound. */
export function pruneSessions(): void {
  const now = Date.now()
  for (const [serverId, expires] of challenges) {
    if (expires <= now) challenges.delete(serverId)
  }
  for (const [key, session] of sessions) {
    if (session.expires <= now) sessions.delete(key)
  }
}
