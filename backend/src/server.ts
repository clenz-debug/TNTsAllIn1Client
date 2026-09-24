import { createServer, type IncomingMessage, type ServerResponse } from 'node:http'
import { capeUrl, deleteCape, saveCape } from './capes.js'
import { CAPE_MAX_BYTES, config } from './config.js'
import { pruneTokenCache, verifyAccessToken, type VerifiedProfile } from './mojang.js'
import { checkCapePng } from './png.js'
import { RateLimiter } from './rateLimit.js'

/**
 * Custom-cape upload service. Apache proxies `https://nxlc.de/app/` here (prefix stripped) and
 * serves the resulting PNGs itself from `~/www/tntcapes/` - this process only ever writes files,
 * it never serves cape images. Routes:
 *   GET    /health  - liveness check
 *   PUT    /capes   - body: PNG, header `Authorization: Bearer <Minecraft access token>`
 *   DELETE /capes   - same auth; removes the caller's active cape
 * Errors are JSON `{ error: <code> }` so the launcher can map them to its own de/en messages.
 */

/** Uploads/deletes per player - generous for trying out a few designs, stops scripted spam. */
const writeLimiter = new RateLimiter(20, 10 * 60 * 1000)
/** Requests per client IP, counted before the Mojang lookup so bad tokens can't hammer Mojang through us. */
const ipLimiter = new RateLimiter(60, 10 * 60 * 1000)

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
}, 10 * 60 * 1000).unref()

server.listen(config.port, config.host, () => {
  console.log(`[server] Listening on http://${config.host}:${config.port}, capes in ${config.capesDir}`)
})
