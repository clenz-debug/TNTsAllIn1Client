import { app } from 'electron'
import type { MinecraftProfile } from '../../shared/types'
import { tryRestoreSession } from '../auth'
import { loadCachedAuth } from '../auth/tokenCache'

/**
 * Signing in to our own server (`backend/`, behind `https://nxlc.de/app/`) without ever sending it
 * the player's Minecraft access token - the same handshake the game uses to prove who is joining a
 * Minecraft server (`backend/src/sessions.ts`):
 *
 *  1. our server hands out a random, single-use server id,
 *  2. we tell Mojang "this account joins server <id>" - the access token goes to Mojang only,
 *  3. our server asks Mojang whether that player joined that id and answers with a session token of
 *     its own, which is all the cape and friends calls carry from then on.
 *
 * That session token is worth something on our server only. It is kept in memory and never written
 * to disk; a new launcher start simply signs in again.
 */

/** `TNT_BACKEND_URL` points a dev launcher (`npm run dev`) at a backend started locally - the
 * installed launcher ignores it and only ever talks to the real server. */
const API_BASE = (!app.isPackaged && process.env['TNT_BACKEND_URL']) || 'https://nxlc.de/app'
const MOJANG_JOIN_URL = 'https://sessionserver.mojang.com/session/minecraft/join'
const HANDSHAKE_TIMEOUT_MS = 10_000
/** A session this close to running out is replaced before the next call rather than during it. */
const RENEW_BEFORE_MS = 60_000

/** Our server said no, or signing in failed - `code` is one of the server's own error codes. */
export class BackendError extends Error {
  constructor(readonly code: string) {
    super(code)
  }
}

let session: { uuid: string; token: string; expires: number; moderator: boolean } | null = null
let opening: Promise<string> | null = null

async function errorCodeOf(response: Response): Promise<string> {
  const data = (await response.json().catch(() => ({}))) as { error?: unknown }
  return typeof data.error === 'string' ? data.error : `http_${response.status}`
}

async function postJson(url: string, body?: unknown): Promise<Response> {
  return fetch(url, {
    method: 'POST',
    headers: body !== undefined ? { 'Content-Type': 'application/json' } : {},
    body: body !== undefined ? JSON.stringify(body) : undefined,
    signal: AbortSignal.timeout(HANDSHAKE_TIMEOUT_MS)
  })
}

async function handshake(profile: MinecraftProfile): Promise<string> {
  const challenge = await postJson(`${API_BASE}/auth/challenge`)
  if (!challenge.ok) throw new BackendError(await errorCodeOf(challenge))
  const { serverId } = (await challenge.json()) as { serverId?: unknown }
  // Whatever our server answers is passed on to Mojang as a plain id and nothing else
  if (typeof serverId !== 'string' || !/^[0-9a-f]{40}$/.test(serverId)) throw new BackendError('unknown')

  const joined = await postJson(MOJANG_JOIN_URL, { accessToken: profile.accessToken, selectedProfile: profile.id, serverId })
  if (!joined.ok) {
    const reason = await errorCodeOf(joined)
    // The account itself is not allowed to play multiplayer (Xbox privacy settings, or banned) -
    // a new access token would not change that
    if (reason === 'InsufficientPrivilegesException' || reason === 'UserBannedException') throw new BackendError('multiplayer_blocked')
    if (joined.status === 401 || joined.status === 403) throw new BackendError('unauthorized')
    throw new BackendError('auth_unavailable')
  }

  const opened = await postJson(`${API_BASE}/auth/session`, { name: profile.name, serverId })
  if (!opened.ok) throw new BackendError(await errorCodeOf(opened))
  const { token, expires, moderator } = (await opened.json()) as { token?: unknown; expires?: unknown; moderator?: unknown }
  if (typeof token !== 'string' || typeof expires !== 'number') throw new BackendError('unknown')
  session = { uuid: profile.id, token, expires, moderator: moderator === true }
  return token
}

/** Signs in as the logged-in player; if Mojang no longer accepts the cached access token (they last
 * 24 hours) or the player was renamed, renews the Minecraft session once and tries again. */
async function openSession(): Promise<string> {
  const cached = await loadCachedAuth()
  if (!cached) throw new BackendError('not_logged_in')
  try {
    return await handshake(cached.profile)
  } catch (err) {
    if (!(err instanceof BackendError) || err.code !== 'unauthorized') throw err
    const renewed = await tryRestoreSession()
    if (!renewed || renewed.offline) throw err
    return handshake(renewed)
  }
}

async function sessionToken(): Promise<string> {
  const cached = await loadCachedAuth()
  if (!cached) throw new BackendError('not_logged_in')
  if (session && session.uuid === cached.profile.id && session.expires - Date.now() > RENEW_BEFORE_MS) return session.token
  session = null
  // Friends ping, game bridge and a cape upload may all want a session at once - one sign-in for all
  opening ??= openSession().finally(() => {
    opening = null
  })
  return opening
}

/** Whether the server counts the logged-in account among the project's moderators (reported capes,
 * `cape/capeModeration.ts`). Only decides what the launcher shows - the server checks every call
 * itself. `false` whenever signing in fails. */
export async function isBackendModerator(): Promise<boolean> {
  try {
    await sessionToken()
    return session?.moderator === true
  } catch {
    return false
  }
}

export interface BackendRequest {
  /** Sent as JSON. */
  json?: unknown
  /** Sent as is, with `contentType`. */
  body?: Uint8Array
  contentType?: string
  timeoutMs?: number
}

/**
 * Calls our server as the logged-in player. A session the server no longer knows (it ran out, or the
 * service was restarted) is replaced once. Throws {@link BackendError} when signing in fails - an
 * answer from the server itself, OK or not, is returned for the caller to read.
 */
export async function backendFetch(method: string, path: string, request: BackendRequest = {}): Promise<Response> {
  const send = async (token: string): Promise<Response> => {
    const contentType = request.json !== undefined ? 'application/json' : request.contentType
    return fetch(`${API_BASE}${path}`, {
      method,
      headers: { Authorization: `Bearer ${token}`, ...(contentType ? { 'Content-Type': contentType } : {}) },
      body: request.json !== undefined ? JSON.stringify(request.json) : request.body,
      signal: request.timeoutMs ? AbortSignal.timeout(request.timeoutMs) : undefined
    })
  }
  let response = await send(await sessionToken())
  if (response.status === 401) {
    session = null
    response = await send(await sessionToken())
  }
  return response
}

/** Logging out, or switching to offline mode: the session is ended on the server too, so the token
 * is worthless from now on. Never holds anything up - without a connection it just runs out. */
export async function endBackendSession(): Promise<void> {
  const ended = session
  session = null
  if (!ended) return
  await fetch(`${API_BASE}/auth/logout`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${ended.token}` },
    signal: AbortSignal.timeout(2_000)
  }).catch(() => undefined)
}
