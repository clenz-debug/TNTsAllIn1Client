import { createServer, type IncomingMessage, type ServerResponse } from 'node:http'
import { capeUrl, deleteCape, saveCape } from './capes.js'
import {
  DECISIONS,
  ModerationError,
  capeBans,
  decideReports,
  openReports,
  pruneReportedCapes,
  reportCape,
  reportedCape,
  unbanCapes,
  type Decision
} from './moderation.js'
import { CAPE_MAX_BYTES, config } from './config.js'
import {
  ACTIVITY_KINDS,
  FriendsError,
  LOGO_COLOR_KEYS,
  STATUSES,
  acceptRequest,
  blockPlayer,
  clientUsers,
  deleteAccount,
  dismissInvite,
  goOffline,
  isCapeBanned,
  overview,
  pruneInvites,
  registerPlayer,
  removeFriend,
  removeRequest,
  revokeInvites,
  sendInvite,
  sendRequest,
  setLogoColors,
  setPresence,
  unblockPlayer,
  type Activity,
  type ActivityKind,
  type LogoColors,
  type Status
} from './friends.js'
import { LookupRefused, hasJoined, pruneTokenCache, verifyAccessToken, type VerifiedProfile } from './mojang.js'
import { checkCapePng } from './png.js'
import { RateLimiter } from './rateLimit.js'
import {
  SERVER_ID_PATTERN,
  SESSION_TOKEN_PREFIX,
  createChallenge,
  createSession,
  endSession,
  endSessionsOf,
  pruneSessions,
  sessionProfile,
  takeChallenge
} from './sessions.js'

/**
 * Custom-cape upload and friends service. Apache proxies `https://nxlc.de/app/` here (prefix
 * stripped) and serves the cape PNGs itself from `~/www/tntcapes/` - this process only ever writes
 * cape files, it never serves cape images. Routes (all but /health and the first two /auth ones
 * need `Authorization: Bearer <session token>`, see `sessions.ts`; launchers up to 0.14.1 send
 * their Minecraft access token there instead, see {@link authenticate}):
 *   GET    /health                        - liveness check
 *   POST   /auth/challenge                - returns { serverId } for the sign-in handshake
 *   POST   /auth/session                  - body: { name, serverId }; returns { token, expires }
 *   POST   /auth/logout                   - ends the caller's session
 *   PUT    /capes                         - body: PNG; sets the caller's active cape
 *   DELETE /capes                         - removes the caller's active cape
 *   POST   /capes/report                  - body: { uuid, name, reason }; reports that player's cape
 *   GET    /admin/reports                 - moderators only: open reports and banned accounts
 *   GET    /admin/reported-capes/<sha>    - moderators only: the copy of a reported cape (PNG)
 *   POST   /admin/decide                  - moderators only, body: { target, decision, reason }
 *   POST   /admin/unban                   - moderators only, body: { uuid }
 *   POST   /presence                      - body: { status, hideServer, activity, logoColors? }; returns the friends overview
 *   POST   /presence/offline              - launcher closing
 *   GET    /friends                       - friends overview (friends with presence, incoming/outgoing requests)
 *   POST   /friends/requests              - body: { name }; send (or, if they asked first, accept) a request
 *   POST   /friends/requests/<uuid>/accept
 *   DELETE /friends/requests/<uuid>       - decline an incoming or withdraw an outgoing request
 *   DELETE /friends/<uuid>                - remove a friend
 *   POST   /friends/blocks                - body: { uuid }; block a player
 *   DELETE /friends/blocks/<uuid>         - unblock
 *   DELETE /account                       - delete everything stored about the caller, cape included
 *   POST   /invites                       - body: { to, address, version }; invite a friend into the caller's world
 *   DELETE /invites                       - withdraw all of the caller's invitations (world closed)
 *   DELETE /invites/<uuid>                - withdraw the invitation to one friend
 *   POST   /invites/<uuid>/dismiss        - invited player declines (or used) the invitation from <uuid>
 *   POST   /players/lookup                - body: { uuids }; which of these players use our client (nametag logo)
 * Errors are JSON `{ error: <code> }` so the launcher can map them to its own de/en messages.
 */

/** Uploads/deletes per player - generous for trying out a few designs, stops scripted spam. */
const writeLimiter = new RateLimiter(20, 10 * 60 * 1000)
/** Requests per client IP, counted before the Mojang lookup so bad tokens can't hammer Mojang through us. */
const ipLimiter = new RateLimiter(60, 10 * 60 * 1000)
/** Friends/presence calls per player - a 20 s presence ping is 30 per window, the rest is headroom for actions. */
const friendsLimiter = new RateLimiter(300, 10 * 60 * 1000)
/** Same per IP, for several players behind one address (a household, a LAN party). */
const friendsIpLimiter = new RateLimiter(1000, 10 * 60 * 1000)
/** Sign-in attempts per client IP - a launcher start needs two calls, and each attempt can cost one Mojang request. */
const authIpLimiter = new RateLimiter(30, 10 * 60 * 1000)
/** Mojang lookups for access tokens per client IP (launchers up to 0.14.1): a real launcher needs one
 * every five minutes, the rest is headroom for a household - and a stop for made-up tokens. */
const tokenLookupLimiter = new RateLimiter(20, 10 * 60 * 1000)
/** Reports per player - enough for someone who really meets a few bad capes, useless for flooding. */
const reportLimiter = new RateLimiter(10, 60 * 60 * 1000)
const JSON_MAX_BYTES = 4 * 1024
/** Checked loosely - Mojang decides whether the player exists. */
const PLAYER_NAME_PATTERN = /^[^\u0000- \u007f]{1,32}$/
const UUID_PATTERN = /^[0-9a-f]{32}$/
/** Players per nametag-logo lookup - 100 UUIDs still fit into {@link JSON_MAX_BYTES}. */
const LOOKUP_MAX_UUIDS = 100
const HEX_COLOR_PATTERN = /^#[0-9a-fA-F]{6}$/
/** e4mc hands out host names like `abc-def.eu.e4mc.link`, optionally with a port. Nothing else is
 * accepted - an invitation must not be able to send a friend to an arbitrary server. */
const ADDRESS_PATTERN = /^(?=.{1,253}(?::|$))([a-z0-9-]+\.)+e4mc\.link(:\d{1,5})?$/i
const VERSION_PATTERN = /^[\w.+-]{1,32}$/

class HttpError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    readonly details: Record<string, unknown> = {}
  ) {
    super(code)
  }
}

function sendJson(res: ServerResponse, status: number, body: Record<string, unknown>): void {
  const payload = JSON.stringify(body)
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(payload),
    'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff'
  })
  res.end(payload)
}

/**
 * Apache's mod_proxy appends the real client address to X-Forwarded-For; the socket is always
 * 127.0.0.1. Only that last entry counts - everything before it was sent by the client itself and
 * could be made up to dodge the per-IP limits.
 */
function clientIp(req: IncomingMessage): string {
  const forwarded = req.headers['x-forwarded-for']
  const last = (Array.isArray(forwarded) ? forwarded.join(',') : forwarded)?.split(',').pop()?.trim()
  return last || req.socket.remoteAddress || 'unknown'
}

function bearerToken(req: IncomingMessage): string {
  const header = req.headers.authorization
  return header?.startsWith('Bearer ') ? header.slice('Bearer '.length).trim() : ''
}

/** Players who signed in with their Minecraft access token since the last log line - only counted,
 * to see when that way in can be closed. */
const accessTokenUsers = new Set<string>()

/**
 * Who is calling. Current launchers send a session token of ours (`sessions.ts`). Launchers up to
 * 0.14.1 send the player's Minecraft access token, which we then have to show Mojang - accepted
 * only while `config.acceptAccessTokens` is on, until those launchers have updated.
 */
async function authenticate(req: IncomingMessage): Promise<VerifiedProfile> {
  const token = bearerToken(req)
  if (!token) throw new HttpError(401, 'unauthorized')
  if (token.startsWith(SESSION_TOKEN_PREFIX)) {
    const profile = sessionProfile(token)
    if (!profile) throw new HttpError(401, 'unauthorized')
    return profile
  }
  if (!config.acceptAccessTokens) throw new HttpError(401, 'unauthorized')

  const profile = await verifyAccessToken(token, () => tokenLookupLimiter.take(clientIp(req))).catch((error: unknown) => {
    if (error instanceof LookupRefused) throw new HttpError(429, 'rate_limited')
    console.error('[auth] Mojang lookup failed:', error)
    throw new HttpError(502, 'auth_unavailable')
  })
  if (!profile) throw new HttpError(401, 'unauthorized')
  accessTokenUsers.add(profile.id)
  return profile
}

/** Streams the body with a hard size cap - an oversized upload is cut off, never buffered whole. */
function readBody(req: IncomingMessage, maxBytes: number): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    const chunks: Buffer[] = []
    let total = 0
    req.on('data', (chunk: Buffer) => {
      total += chunk.length
      if (total > maxBytes) {
        reject(new HttpError(413, 'too_large', { maxBytes }))
        req.destroy()
        return
      }
      chunks.push(chunk)
    })
    req.on('end', () => resolve(Buffer.concat(chunks)))
    req.on('error', reject)
  })
}

async function handleUpload(req: IncomingMessage, res: ServerResponse): Promise<void> {
  const declaredLength = Number(req.headers['content-length'] ?? 0)
  if (declaredLength > CAPE_MAX_BYTES) throw new HttpError(413, 'too_large', { maxBytes: CAPE_MAX_BYTES })

  const profile = await authenticate(req)
  if (!writeLimiter.take(profile.id)) throw new HttpError(429, 'rate_limited')
  if (isCapeBanned(profile.id)) throw new HttpError(403, 'cape_banned')

  const body = await readBody(req, CAPE_MAX_BYTES)
  const check = checkCapePng(body)
  if (!check.ok) {
    throw new HttpError(400, check.error, { width: check.width, height: check.height })
  }

  await saveCape(profile.id, check.png)
  console.log(`[capes] ${profile.name} (${profile.id}) uploaded ${check.width}x${check.height}, ${check.png.length} bytes`)
  sendJson(res, 200, { url: capeUrl(profile.id), width: check.width, height: check.height })
}

async function handleDelete(req: IncomingMessage, res: ServerResponse): Promise<void> {
  const profile = await authenticate(req)
  if (!writeLimiter.take(profile.id)) throw new HttpError(429, 'rate_limited')
  await deleteCape(profile.id)
  console.log(`[capes] ${profile.name} (${profile.id}) removed their cape`)
  sendJson(res, 200, { ok: true })
}

/** Free text a moderator will read: one line, bounded, no control characters. */
function cleanText(value: unknown, maxLength: number): string {
  return typeof value === 'string' ? value.replace(/[\u0000-\u001f\u007f]/g, ' ').trim().slice(0, maxLength) : ''
}

async function handleReport(req: IncomingMessage, res: ServerResponse): Promise<void> {
  const profile = await authenticate(req)
  if (!reportLimiter.take(profile.id)) throw new HttpError(429, 'rate_limited')
  const { uuid, name, reason } = await readJson(req)
  if (typeof uuid !== 'string' || !UUID_PATTERN.test(uuid) || typeof name !== 'string' || !/^\w{1,16}$/.test(name)) {
    throw new HttpError(400, 'invalid_body')
  }
  await reportCape(profile.id, uuid, name, cleanText(reason, 200))
  sendJson(res, 200, { ok: true })
}

/** Deciding about reported capes - for the accounts in `config.moderators` only. */
async function routeAdmin(req: IncomingMessage, res: ServerResponse, path: string): Promise<void> {
  if (!ipLimiter.take(clientIp(req))) throw new HttpError(429, 'rate_limited')
  const profile = await authenticate(req)
  if (!config.moderators.has(profile.id)) throw new HttpError(403, 'forbidden')

  if (path === '/admin/reports' && req.method === 'GET') {
    return sendJson(res, 200, { reports: await openReports(), bans: capeBans() })
  }
  if (path.startsWith('/admin/reported-capes/') && req.method === 'GET') {
    const png = await reportedCape(path.slice('/admin/reported-capes/'.length))
    if (!png) throw new HttpError(404, 'not_found')
    res.writeHead(200, { 'Content-Type': 'image/png', 'Content-Length': png.length, 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' })
    res.end(png)
    return
  }
  if (path === '/admin/decide' && req.method === 'POST') {
    const { target, decision, reason } = await readJson(req)
    if (typeof target !== 'string' || !UUID_PATTERN.test(target) || typeof decision !== 'string' || !(DECISIONS as readonly string[]).includes(decision)) {
      throw new HttpError(400, 'invalid_body')
    }
    const text = cleanText(reason, 200)
    if (decision === 'ban' && !text) throw new HttpError(400, 'invalid_body')
    await decideReports(target, decision as Decision, text)
    console.log(`[moderation] ${profile.name} decided "${decision}" about the cape of ${target}`)
    return sendJson(res, 200, { reports: await openReports(), bans: capeBans() })
  }
  if (path === '/admin/unban' && req.method === 'POST') {
    const uuid = (await readJson(req))['uuid']
    if (typeof uuid !== 'string' || !UUID_PATTERN.test(uuid)) throw new HttpError(400, 'invalid_body')
    unbanCapes(uuid)
    console.log(`[moderation] ${profile.name} lifted the cape ban of ${uuid}`)
    return sendJson(res, 200, { reports: await openReports(), bans: capeBans() })
  }
  throw new HttpError(404, 'not_found')
}

async function readJson(req: IncomingMessage): Promise<Record<string, unknown>> {
  const body = await readBody(req, JSON_MAX_BYTES)
  try {
    const parsed: unknown = JSON.parse(body.toString('utf8') || '{}')
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) return parsed as Record<string, unknown>
  } catch {
    // falls through to the error below
  }
  throw new HttpError(400, 'invalid_body')
}

function parseActivity(value: unknown): Activity | null {
  if (value === null || value === undefined) return null
  if (typeof value !== 'object') throw new HttpError(400, 'invalid_body')
  const { kind, server } = value as { kind?: unknown; server?: unknown }
  if (typeof kind !== 'string' || !(ACTIVITY_KINDS as readonly string[]).includes(kind)) throw new HttpError(400, 'invalid_body')
  if (kind === 'multiplayer' && typeof server === 'string' && server.trim()) {
    // Shown to friends as plain text - no control characters, bounded length.
    return { kind, server: server.replace(/[\u0000-\u001f\u007f]/g, '').trim().slice(0, 100) }
  }
  return { kind: kind as ActivityKind }
}

/** Optional - launchers before the nametag logo don't send it, and a malformed one is just ignored. */
function parseLogoColors(value: unknown): LogoColors | null {
  if (!value || typeof value !== 'object') return null
  const colors = value as Record<string, unknown>
  const valid = LOGO_COLOR_KEYS.every((key) => {
    const color = colors[key]
    return typeof color === 'string' && HEX_COLOR_PATTERN.test(color)
  })
  if (!valid) return null
  return Object.fromEntries(LOGO_COLOR_KEYS.map((key) => [key, (colors[key] as string).toLowerCase()])) as LogoColors
}

async function handlePresence(req: IncomingMessage, res: ServerResponse, uuid: string): Promise<void> {
  const body = await readJson(req)
  const status = body['status']
  if (typeof status !== 'string' || !(STATUSES as readonly string[]).includes(status)) throw new HttpError(400, 'invalid_body')
  setPresence(uuid, status as Status, body['hideServer'] === true, parseActivity(body['activity']))
  const logoColors = parseLogoColors(body['logoColors'])
  if (logoColors) setLogoColors(uuid, logoColors)
  sendJson(res, 200, { ...overview(uuid) })
}

async function routeFriends(req: IncomingMessage, res: ServerResponse, path: string): Promise<void> {
  if (!friendsIpLimiter.take(clientIp(req))) throw new HttpError(429, 'rate_limited')
  const profile = await authenticate(req)
  if (!friendsLimiter.take(profile.id)) throw new HttpError(429, 'rate_limited')
  registerPlayer(profile)
  const me = profile.id
  const segments = path.split('/').filter(Boolean)

  try {
    if (path === '/presence' && req.method === 'POST') return await handlePresence(req, res, me)
    if (path === '/presence/offline' && req.method === 'POST') {
      goOffline(me)
      return sendJson(res, 200, { ok: true })
    }
    if (path === '/friends' && req.method === 'GET') return sendJson(res, 200, { ...overview(me) })
    if (path === '/friends/requests' && req.method === 'POST') {
      const name = (await readJson(req))['name']
      if (typeof name !== 'string' || !/^\w{1,16}$/.test(name.trim())) throw new HttpError(400, 'invalid_name')
      return sendJson(res, 200, { ...sendRequest(me, name) })
    }
    if (segments[0] === 'friends' && segments[1] === 'requests' && segments[2] && UUID_PATTERN.test(segments[2])) {
      if (segments.length === 4 && segments[3] === 'accept' && req.method === 'POST') return sendJson(res, 200, { ...acceptRequest(me, segments[2]) })
      if (segments.length === 3 && req.method === 'DELETE') return sendJson(res, 200, { ...removeRequest(me, segments[2]) })
    }
    if (path === '/friends/blocks' && req.method === 'POST') {
      const uuid = (await readJson(req))['uuid']
      if (typeof uuid !== 'string' || !UUID_PATTERN.test(uuid)) throw new HttpError(400, 'invalid_body')
      return sendJson(res, 200, { ...blockPlayer(me, uuid) })
    }
    if (segments[0] === 'friends' && segments[1] === 'blocks' && segments.length === 3 && UUID_PATTERN.test(segments[2] ?? '') && req.method === 'DELETE') {
      return sendJson(res, 200, { ...unblockPlayer(me, segments[2] ?? '') })
    }
    if (path === '/account' && req.method === 'DELETE') {
      deleteAccount(me)
      await deleteCape(me)
      endSessionsOf(me)
      console.log(`[account] ${me} deleted their data`)
      return sendJson(res, 200, { ok: true })
    }
    if (segments[0] === 'friends' && segments.length === 2 && UUID_PATTERN.test(segments[1]) && req.method === 'DELETE') {
      return sendJson(res, 200, { ...removeFriend(me, segments[1]) })
    }
    if (path === '/invites' && req.method === 'POST') {
      const body = await readJson(req)
      const { to, address, version } = body
      if (typeof to !== 'string' || !UUID_PATTERN.test(to) || typeof address !== 'string' || !ADDRESS_PATTERN.test(address)) {
        throw new HttpError(400, 'invalid_body')
      }
      if (typeof version !== 'string' || !VERSION_PATTERN.test(version)) throw new HttpError(400, 'invalid_body')
      return sendJson(res, 200, { ...sendInvite(me, to, address, version) })
    }
    if (path === '/invites' && req.method === 'DELETE') return sendJson(res, 200, { ...revokeInvites(me, null) })
    if (segments[0] === 'invites' && segments[1] && UUID_PATTERN.test(segments[1])) {
      if (segments.length === 2 && req.method === 'DELETE') return sendJson(res, 200, { ...revokeInvites(me, segments[1]) })
      if (segments.length === 3 && segments[2] === 'dismiss' && req.method === 'POST') return sendJson(res, 200, { ...dismissInvite(me, segments[1]) })
    }
    if (path === '/players/lookup' && req.method === 'POST') {
      const uuids = (await readJson(req))['uuids']
      if (!Array.isArray(uuids) || uuids.length > LOOKUP_MAX_UUIDS || !uuids.every((uuid) => typeof uuid === 'string' && UUID_PATTERN.test(uuid))) {
        throw new HttpError(400, 'invalid_body')
      }
      return sendJson(res, 200, { clientUsers: clientUsers(uuids as string[]) })
    }
  } catch (error) {
    if (error instanceof FriendsError) throw new HttpError(error.status, error.code)
    throw error
  }
  throw new HttpError(404, 'not_found')
}

/** The sign-in handshake of `sessions.ts`. */
async function routeAuth(req: IncomingMessage, res: ServerResponse, path: string): Promise<void> {
  if (req.method !== 'POST') throw new HttpError(405, 'method_not_allowed')
  if (path === '/auth/logout') {
    const token = bearerToken(req)
    if (token.startsWith(SESSION_TOKEN_PREFIX)) endSession(token)
    return sendJson(res, 200, { ok: true })
  }
  if (!authIpLimiter.take(clientIp(req))) throw new HttpError(429, 'rate_limited')
  if (path === '/auth/challenge') {
    const serverId = createChallenge()
    if (!serverId) throw new HttpError(503, 'busy')
    return sendJson(res, 200, { serverId })
  }
  if (path === '/auth/session') {
    const { name, serverId } = await readJson(req)
    if (typeof name !== 'string' || !PLAYER_NAME_PATTERN.test(name) || typeof serverId !== 'string' || !SERVER_ID_PATTERN.test(serverId)) {
      throw new HttpError(400, 'invalid_body')
    }
    if (!takeChallenge(serverId)) throw new HttpError(400, 'invalid_challenge')
    const profile = await hasJoined(name, serverId).catch((error: unknown) => {
      console.error('[auth] Mojang hasJoined failed:', error)
      throw new HttpError(502, 'auth_unavailable')
    })
    if (!profile) throw new HttpError(401, 'unauthorized')
    return sendJson(res, 200, { ...createSession(profile), moderator: config.moderators.has(profile.id) })
  }
  throw new HttpError(404, 'not_found')
}

async function route(req: IncomingMessage, res: ServerResponse): Promise<void> {
  const path = new URL(req.url ?? '/', 'http://localhost').pathname

  if (path === '/health' && req.method === 'GET') {
    sendJson(res, 200, { ok: true })
    return
  }
  if (path.startsWith('/auth/')) return routeAuth(req, res, path)
  if (path === '/capes/report' && req.method === 'POST') {
    if (!ipLimiter.take(clientIp(req))) throw new HttpError(429, 'rate_limited')
    return handleReport(req, res)
  }
  if (path.startsWith('/admin/')) return routeAdmin(req, res, path)
  if (path === '/capes') {
    if (!ipLimiter.take(clientIp(req))) throw new HttpError(429, 'rate_limited')
    if (req.method === 'PUT') return handleUpload(req, res)
    if (req.method === 'DELETE') return handleDelete(req, res)
    throw new HttpError(405, 'method_not_allowed')
  }
  if (['/friends', '/presence', '/invites', '/players', '/account'].some((prefix) => path === prefix || path.startsWith(`${prefix}/`))) {
    return routeFriends(req, res, path)
  }
  throw new HttpError(404, 'not_found')
}

const server = createServer((req, res) => {
  route(req, res).catch((error: unknown) => {
    if (res.headersSent) return
    if (error instanceof HttpError) {
      sendJson(res, error.status, { error: error.code, ...error.details })
    } else if (error instanceof ModerationError) {
      sendJson(res, error.status, { error: error.code })
    } else {
      console.error('[server] Unexpected error:', error)
      sendJson(res, 500, { error: 'internal' })
    }
  })
})

// Slow or stalled uploads shouldn't pin a connection forever.
server.requestTimeout = 60_000
server.headersTimeout = 15_000

setInterval(() => {
  pruneTokenCache()
  pruneSessions()
  authIpLimiter.prune()
  tokenLookupLimiter.prune()
  if (accessTokenUsers.size > 0) {
    console.log(`[auth] ${accessTokenUsers.size} player(s) still signed in with a Minecraft access token (launcher 0.14.1 or older)`)
    accessTokenUsers.clear()
  }
  reportLimiter.prune()
  pruneReportedCapes()
  writeLimiter.prune()
  ipLimiter.prune()
  friendsLimiter.prune()
  friendsIpLimiter.prune()
  pruneInvites()
}, 10 * 60 * 1000).unref()

server.listen(config.port, config.host, () => {
  console.log(`[server] Listening on http://${config.host}:${config.port}, capes in ${config.capesDir}, data in ${config.dataDir}`)
})
