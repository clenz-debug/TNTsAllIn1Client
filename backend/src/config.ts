import { homedir } from 'node:os'
import { join } from 'node:path'

function megabytes(variable: string, fallback: number): number {
  const value = Number(process.env[variable])
  return (Number.isFinite(value) && value >= 0 ? value : fallback) * 1024 * 1024
}

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
  dataDir: process.env['TNTCAPES_DATA_DIR'] ?? join(homedir(), 'tntcapes-data'),
  /** Launchers up to 0.14.1 sign in by sending the player's Minecraft access token. Set
   * `TNTCAPES_ACCEPT_ACCESS_TOKENS=0` once they have updated - from then on the service never sees one. */
  acceptAccessTokens: process.env['TNTCAPES_ACCEPT_ACCESS_TOKENS'] !== '0',
  /** Uploads and copies of reported capes are refused once the disk has less than this free
   * (`TNTCAPES_MIN_FREE_MB`) - see `storage.ts`. */
  minFreeBytes: megabytes('TNTCAPES_MIN_FREE_MB', 500),
  /** Ceiling for all active capes together (`TNTCAPES_MAX_CAPES_MB`). */
  maxCapesBytes: megabytes('TNTCAPES_MAX_CAPES_MB', 1024),
  /** Ceiling for the copies of reported capes (`TNTCAPES_MAX_REPORTED_MB`). */
  maxReportedBytes: megabytes('TNTCAPES_MAX_REPORTED_MB', 200),
  /** UUIDs (comma-separated in `TNTCAPES_MODERATORS`) of the accounts that see reported capes in
   * their launcher and decide about them - see `moderation.ts`. Nobody, if unset. */
  moderators: new Set(
    (process.env['TNTCAPES_MODERATORS'] ?? '')
      .split(',')
      .map((uuid) => uuid.replace(/-/g, '').trim().toLowerCase())
      .filter((uuid) => /^[0-9a-f]{32}$/.test(uuid))
  )
}

/** Own user decision: only the active cape lives on the server, up to 5 MB, 2:1 up to 2048x1024. */
export const CAPE_MAX_BYTES = 5 * 1024 * 1024
export const CAPE_MIN_WIDTH = 64
export const CAPE_MAX_WIDTH = 2048

/** A friend counts as offline once their launcher hasn't checked in for this long (it pings every ~20 s). */
export const PRESENCE_TIMEOUT_MS = 60 * 1000
export const MAX_FRIENDS = 200
export const MAX_OUTGOING_REQUESTS = 50
export const MAX_BLOCKED = 500
/** A world invitation can be accepted this long (Phase 8b) - the host can always send a new one. */
export const INVITE_TTL_MS = 10 * 60 * 1000
