import { createServer, type IncomingMessage, type ServerResponse } from 'node:http'
import { capeUrl, deleteCape, saveCape } from './capes.js'
import { CAPE_MAX_BYTES, config } from './config.js'
import {
  ACTIVITY_KINDS,
  FriendsError,
  STATUSES,
  acceptRequest,
  goOffline,
  overview,
  registerPlayer,
  removeFriend,
  removeRequest,
  sendRequest,
  setPresence,
  type Activity,
  type ActivityKind,
  type Status
} from './friends.js'
import { pruneTokenCache, verifyAccessToken, type VerifiedProfile } from './mojang.js'
import { checkCapePng } from './png.js'
import { RateLimiter } from './rateLimit.js'

/**
 * Custom-cape upload and friends service. Apache proxies `https://nxlc.de/app/` here (prefix
 * stripped) and serves the cape PNGs itself from `~/www/tntcapes/` - this process only ever writes
 * cape files, it never serves cape images. Routes (all but /health need
 * `Authorization: Bearer <Minecraft access token>`):
 *   GET    /health                        - liveness check
 *   PUT    /capes                         - body: PNG; sets the caller's active cape
 *   DELETE /capes                         - removes the caller's active cape
 *   POST   /presence                      - body: { status, hideServer, activity }; returns the friends overview
 *   POST   /presence/offline              - launcher closing
 *   GET    /friends                       - friends overview (friends with presence, incoming/outgoing requests)
 *   POST   /friends/requests              - body: { name }; send (or, if they asked first, accept) a request
 *   POST   /friends/requests/<uuid>/accept
 *   DELETE /friends/requests/<uuid>       - decline an incoming or withdraw an outgoing request
 *   DELETE /friends/<uuid>                - remove a friend
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
const JSON_MAX_BYTES = 4 * 1024
const UUID_PATTERN = /^[0-9a-f]{32}$/

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

/** Apache's mod_proxy appends the real client address to X-Forwarded-For; the socket is always 127.0.0.1. */
function clientIp(req: IncomingMessage): string {
  const forwarded = req.headers['x-forwarded-for']
  const first = (Array.isArray(forwarded) ? forwarded[0] : forwarded)?.split(',')[0]?.trim()
  return first || req.socket.remoteAddress || 'unknown'
}

async function authenticate(req: IncomingMessage): Promise<VerifiedProfile> {
  const header = req.headers.authorization
  const token = header?.startsWith('Bearer ') ? header.slice('Bearer '.length).trim() : ''
  if (!token) throw new HttpError(401, 'unauthorized')
  const profile = await verifyAccessToken(token).catch((error: unknown) => {
    console.error('[auth] Mojang lookup failed:', error)
    throw new HttpError(502, 'auth_unavailable')
  })
  if (!profile) throw new HttpError(401, 'unauthorized')
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

  const body = await readBody(req, CAPE_MAX_BYTES)
  const check = checkCapePng(body)
  if (!check.ok) {
    throw new HttpError(400, check.error, { width: check.width, height: check.height })
  }

  await saveCape(profile.id, body)
  console.log(`[capes] ${profile.name} (${profile.id}) uploaded ${check.width}x${check.height}, ${body.length} bytes`)
  sendJson(res, 200, { url: capeUrl(profile.id), width: check.width, height: check.height })
}

async function handleDelete(req: IncomingMessage, res: ServerResponse): Promise<void> {
  const profile = await authenticate(req)
  if (!writeLimiter.take(profile.id)) throw new HttpError(429, 'rate_limited')
  await deleteCape(profile.id)
  console.log(`[capes] ${profile.name} (${profile.id}) removed their cape`)
  sendJson(res, 200, { ok: true })
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

async function handlePresence(req: IncomingMessage, res: ServerResponse, uuid: string): Promise<void> {
  const body = await readJson(req)
  const status = body['status']
  if (typeof status !== 'string' || !(STATUSES as readonly string[]).includes(status)) throw new HttpError(400, 'invalid_body')
  setPresence(uuid, status as Status, body['hideServer'] === true, parseActivity(body['activity']))
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
    if (segments[0] === 'friends' && segments.length === 2 && UUID_PATTERN.test(segments[1]) && req.method === 'DELETE') {
      return sendJson(res, 200, { ...removeFriend(me, segments[1]) })
    }
  } catch (error) {
    if (error instanceof FriendsError) throw new HttpError(error.status, error.code)
    throw error
  }
  throw new HttpError(404, 'not_found')
}

async function route(req: IncomingMessage, res: ServerResponse): Promise<void> {
  const path = new URL(req.url ?? '/', 'http://localhost').pathname

  if (path === '/health' && req.method === 'GET') {
    sendJson(res, 200, { ok: true })
    return
  }
  if (path === '/capes') {
    if (!ipLimiter.take(clientIp(req))) throw new HttpError(429, 'rate_limited')
    if (req.method === 'PUT') return handleUpload(req, res)
    if (req.method === 'DELETE') return handleDelete(req, res)
    throw new HttpError(405, 'method_not_allowed')
  }
  if (path === '/friends' || path.startsWith('/friends/') || path === '/presence' || path.startsWith('/presence/')) {
    return routeFriends(req, res, path)
  }
  throw new HttpError(404, 'not_found')
}

const server = createServer((req, res) => {
  route(req, res).catch((error: unknown) => {
    if (res.headersSent) return
    if (error instanceof HttpError) {
      sendJson(res, error.status, { error: error.code, ...error.details })
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
  writeLimiter.prune()
  ipLimiter.prune()
  friendsLimiter.prune()
  friendsIpLimiter.prune()
}, 10 * 60 * 1000).unref()

server.listen(config.port, config.host, () => {
  console.log(`[server] Listening on http://${config.host}:${config.port}, capes in ${config.capesDir}, data in ${config.dataDir}`)
})
