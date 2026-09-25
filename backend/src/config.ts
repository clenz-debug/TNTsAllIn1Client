import { homedir } from 'node:os'
import { join } from 'node:path'

/**
 * Everything deployment-specific, overridable via environment variables (the systemd unit in
 * `deploy/` sets none of them today - these defaults match the live server: Apache proxies
 * `https://nxlc.de/app/` to 127.0.0.1:1025 and serves `~/www/` as `https://nxlc.de/`).
 */
export const config = {
  host: process.env['TNTCAPES_HOST'] ?? '127.0.0.1',
  port: Number(process.env['TNTCAPES_PORT'] ?? 1025),
  /** Publicly served by Apache - one `<dashed-uuid>.png` per player with an active cape. */
  capesDir: process.env['TNTCAPES_DIR'] ?? join(homedir(), 'www', 'tntcapes'),
  publicBaseUrl: process.env['TNTCAPES_PUBLIC_BASE_URL'] ?? 'https://nxlc.de/tntcapes',
  /** Friends database (`friends.sqlite`) - outside the deployed code folder so redeploys never touch it. */
  dataDir: process.env['TNTCAPES_DATA_DIR'] ?? join(homedir(), 'tntcapes-data')
}

/** Own user decision: only the active cape lives on the server, up to 5 MB, 2:1 up to 2048x1024. */
export const CAPE_MAX_BYTES = 5 * 1024 * 1024
export const CAPE_MIN_WIDTH = 64
export const CAPE_MAX_WIDTH = 2048

/** A friend counts as offline once their launcher hasn't checked in for this long (it pings every ~20 s). */
export const PRESENCE_TIMEOUT_MS = 60 * 1000
export const MAX_FRIENDS = 200
export const MAX_OUTGOING_REQUESTS = 50
