import { mkdirSync } from 'node:fs'
import { join } from 'node:path'
import { DatabaseSync } from 'node:sqlite'
import { INVITE_TTL_MS, MAX_FRIENDS, MAX_OUTGOING_REQUESTS, PRESENCE_TIMEOUT_MS, config } from './config.js'
import type { VerifiedProfile } from './mojang.js'

/**
 * Friends and presence (Phase 8, own user request). Players are identified by their Mojang-verified
 * UUID only (see `mojang.ts`) - a player row exists once someone has used the launcher with our
 * backend, which is also the rule for who can be added as a friend. SQLite via Node's built-in
 * `node:sqlite`, so the service still has no runtime dependencies.
 *
 * Presence is pull-based: each launcher pings `POST /presence` about every 20 s with its status and
 * activity and gets its whole friends overview back in the same answer. Someone who stops pinging
 * shows as offline after {@link PRESENCE_TIMEOUT_MS}.
 */

/** What the player picked, Discord-style. `invisible` looks exactly like offline to friends. */
export const STATUSES = ['online', 'away', 'dnd', 'invisible'] as const
export type Status = (typeof STATUSES)[number]

/** What a friend sees - `invisible` never leaves the server. */
export type VisibleStatus = 'online' | 'away' | 'dnd' | 'offline'

/** `playing` = the game runs but doesn't say where (a version without our mod). */
export const ACTIVITY_KINDS = ['launcher', 'menu', 'singleplayer', 'multiplayer', 'playing'] as const
export type ActivityKind = (typeof ACTIVITY_KINDS)[number]

export interface Activity {
  kind: ActivityKind
  /** Multiplayer only, and only if the player doesn't hide it. */
  server?: string
}

export interface PlayerSummary {
  uuid: string
  name: string
}

export interface FriendEntry extends PlayerSummary {
  status: VisibleStatus
  activity: Activity | null
}

/** An invitation into a friend's singleplayer world (Phase 8b), as the invited player sees it.
 * `address` is the host's public e4mc address - only ever handed to the invited friend. */
export interface WorldInvite {
  from: PlayerSummary
  address: string
  /** Minecraft version of the host's world - the friend needs an instance with the same one. */
  version: string
  expires: number
}

export interface FriendsOverview {
  friends: FriendEntry[]
  incoming: PlayerSummary[]
  outgoing: PlayerSummary[]
  invites: WorldInvite[]
}

export class FriendsError extends Error {
  constructor(
    readonly status: number,
    readonly code: string
  ) {
    super(code)
  }
}

mkdirSync(config.dataDir, { recursive: true })
const db = new DatabaseSync(join(config.dataDir, 'friends.sqlite'))
db.exec(`
  PRAGMA journal_mode = WAL;
  PRAGMA foreign_keys = ON;
  CREATE TABLE IF NOT EXISTS players (
    uuid        TEXT PRIMARY KEY,
    name        TEXT NOT NULL,
    name_lower  TEXT NOT NULL,
    status      TEXT NOT NULL DEFAULT 'online',
    hide_server INTEGER NOT NULL DEFAULT 0,
    activity    TEXT,
    last_seen   INTEGER NOT NULL DEFAULT 0,
    created     INTEGER NOT NULL
  );
  CREATE INDEX IF NOT EXISTS players_name_lower ON players (name_lower);
  -- One row per pair, smaller UUID first.
  CREATE TABLE IF NOT EXISTS friendships (
    a     TEXT NOT NULL REFERENCES players (uuid),
    b     TEXT NOT NULL REFERENCES players (uuid),
    since INTEGER NOT NULL,
    PRIMARY KEY (a, b)
  );
  CREATE TABLE IF NOT EXISTS friend_requests (
    from_uuid TEXT NOT NULL REFERENCES players (uuid),
    to_uuid   TEXT NOT NULL REFERENCES players (uuid),
    created   INTEGER NOT NULL,
    PRIMARY KEY (from_uuid, to_uuid)
  );
  -- Phase 8b: one open invitation per host/friend pair, replaced when the host invites again.
  CREATE TABLE IF NOT EXISTS world_invites (
    from_uuid TEXT NOT NULL REFERENCES players (uuid),
    to_uuid   TEXT NOT NULL REFERENCES players (uuid),
    address   TEXT NOT NULL,
    version   TEXT NOT NULL,
    expires   INTEGER NOT NULL,
    PRIMARY KEY (from_uuid, to_uuid)
  );
`)

function pair(x: string, y: string): [string, string] {
  return x < y ? [x, y] : [y, x]
}

function transaction<T>(work: () => T): T {
  db.exec('BEGIN')
  try {
    const result = work()
    db.exec('COMMIT')
    return result
  } catch (error) {
    db.exec('ROLLBACK')
    throw error
  }
}

const upsertPlayer = db.prepare(`
  INSERT INTO players (uuid, name, name_lower, created) VALUES (?, ?, ?, ?)
  ON CONFLICT (uuid) DO UPDATE SET name = excluded.name, name_lower = excluded.name_lower
`)

/** Every authenticated request registers the caller and keeps their current name up to date. */
export function registerPlayer(profile: VerifiedProfile): void {
  upsertPlayer.run(profile.id, profile.name, profile.name.toLowerCase(), Date.now())
}

const updatePresence = db.prepare('UPDATE players SET status = ?, hide_server = ?, activity = ?, last_seen = ? WHERE uuid = ?')
const clearPresence = db.prepare('UPDATE players SET last_seen = 0 WHERE uuid = ?')

export function setPresence(uuid: string, status: Status, hideServer: boolean, activity: Activity | null): void {
  updatePresence.run(status, hideServer ? 1 : 0, activity ? JSON.stringify(activity) : null, Date.now(), uuid)
}

/** Launcher closing - show as offline right away instead of after the timeout. */
export function goOffline(uuid: string): void {
  clearPresence.run(uuid)
}

interface PlayerRow {
  uuid: string
  name: string
  status: string
  hide_server: number
  activity: string | null
  last_seen: number
}

const selectFriends = db.prepare(`
  SELECT p.uuid, p.name, p.status, p.hide_server, p.activity, p.last_seen
  FROM friendships f JOIN players p ON p.uuid = CASE WHEN f.a = ? THEN f.b ELSE f.a END
  WHERE f.a = ? OR f.b = ?
`)
const selectIncoming = db.prepare(`
  SELECT p.uuid, p.name FROM friend_requests r JOIN players p ON p.uuid = r.from_uuid
  WHERE r.to_uuid = ? ORDER BY r.created
`)
const selectOutgoing = db.prepare(`
  SELECT p.uuid, p.name FROM friend_requests r JOIN players p ON p.uuid = r.to_uuid
  WHERE r.from_uuid = ? ORDER BY r.created
`)

function toFriendEntry(row: PlayerRow, now: number): FriendEntry {
  const online = row.status !== 'invisible' && now - row.last_seen < PRESENCE_TIMEOUT_MS
  if (!online) return { uuid: row.uuid, name: row.name, status: 'offline', activity: null }
  let activity: Activity | null = null
  if (row.activity) {
    const parsed = JSON.parse(row.activity) as Activity
    activity = row.hide_server || parsed.kind !== 'multiplayer' ? { kind: parsed.kind } : parsed
  }
  return { uuid: row.uuid, name: row.name, status: row.status as VisibleStatus, activity }
}

const STATUS_ORDER: Record<VisibleStatus, number> = { online: 0, dnd: 1, away: 2, offline: 3 }

const selectInvites = db.prepare(`
  SELECT p.uuid, p.name, i.address, i.version, i.expires
  FROM world_invites i JOIN players p ON p.uuid = i.from_uuid
  WHERE i.to_uuid = ? AND i.expires > ? ORDER BY i.expires DESC
`)
const deleteExpiredInvites = db.prepare('DELETE FROM world_invites WHERE expires <= ?')

/** Called periodically by the server - expired invitations are never shown anyway. */
export function pruneInvites(): void {
  deleteExpiredInvites.run(Date.now())
}

export function overview(uuid: string): FriendsOverview {
  const now = Date.now()
  const friends = (selectFriends.all(uuid, uuid, uuid) as unknown as PlayerRow[])
    .map((row) => toFriendEntry(row, now))
    .sort((x, y) => STATUS_ORDER[x.status] - STATUS_ORDER[y.status] || x.name.localeCompare(y.name, undefined, { sensitivity: 'base' }))
  const invites = (selectInvites.all(uuid, now) as unknown as Array<PlayerSummary & { address: string; version: string; expires: number }>).map(
    (row) => ({ from: { uuid: row.uuid, name: row.name }, address: row.address, version: row.version, expires: row.expires })
  )
  return {
    friends,
    incoming: selectIncoming.all(uuid) as unknown as PlayerSummary[],
    outgoing: selectOutgoing.all(uuid) as unknown as PlayerSummary[],
    invites
  }
}

const findByName = db.prepare('SELECT uuid, name FROM players WHERE name_lower = ?')
const isFriend = db.prepare('SELECT 1 FROM friendships WHERE a = ? AND b = ?')
const hasRequest = db.prepare('SELECT 1 FROM friend_requests WHERE from_uuid = ? AND to_uuid = ?')
const countFriends = db.prepare('SELECT COUNT(*) AS n FROM friendships WHERE a = ? OR b = ?')
const countOutgoing = db.prepare('SELECT COUNT(*) AS n FROM friend_requests WHERE from_uuid = ?')
const insertRequest = db.prepare('INSERT INTO friend_requests (from_uuid, to_uuid, created) VALUES (?, ?, ?)')
const deleteRequest = db.prepare('DELETE FROM friend_requests WHERE from_uuid = ? AND to_uuid = ?')
const insertFriendship = db.prepare('INSERT OR IGNORE INTO friendships (a, b, since) VALUES (?, ?, ?)')
const deleteFriendship = db.prepare('DELETE FROM friendships WHERE a = ? AND b = ?')

function friendCount(uuid: string): number {
  return (countFriends.get(uuid, uuid) as { n: number }).n
}

function befriend(x: string, y: string): void {
  if (friendCount(x) >= MAX_FRIENDS || friendCount(y) >= MAX_FRIENDS) throw new FriendsError(409, 'too_many_friends')
  const [a, b] = pair(x, y)
  insertFriendship.run(a, b, Date.now())
  deleteRequest.run(x, y)
  deleteRequest.run(y, x)
}

/**
 * Friend request by Minecraft name. Only players who have used the client can be found. If the
 * other player already asked us, this simply accepts their request.
 */
export function sendRequest(fromUuid: string, targetName: string): FriendsOverview {
  const target = findByName.get(targetName.trim().toLowerCase()) as PlayerSummary | undefined
  if (!target) throw new FriendsError(404, 'player_not_found')
  if (target.uuid === fromUuid) throw new FriendsError(400, 'cannot_add_self')
  const [a, b] = pair(fromUuid, target.uuid)
  if (isFriend.get(a, b)) throw new FriendsError(409, 'already_friends')

  transaction(() => {
    if (hasRequest.get(target.uuid, fromUuid)) {
      befriend(fromUuid, target.uuid)
      return
    }
    if (hasRequest.get(fromUuid, target.uuid)) throw new FriendsError(409, 'already_requested')
    if ((countOutgoing.get(fromUuid) as { n: number }).n >= MAX_OUTGOING_REQUESTS) {
      throw new FriendsError(409, 'too_many_requests')
    }
    insertRequest.run(fromUuid, target.uuid, Date.now())
  })
  return overview(fromUuid)
}

export function acceptRequest(uuid: string, fromUuid: string): FriendsOverview {
  transaction(() => {
    if (!hasRequest.get(fromUuid, uuid)) throw new FriendsError(404, 'request_not_found')
    befriend(uuid, fromUuid)
  })
  return overview(uuid)
}

/** Declines an incoming request or withdraws an outgoing one - whichever exists. */
export function removeRequest(uuid: string, otherUuid: string): FriendsOverview {
  deleteRequest.run(otherUuid, uuid)
  deleteRequest.run(uuid, otherUuid)
  return overview(uuid)
}

const deleteInvite = db.prepare('DELETE FROM world_invites WHERE from_uuid = ? AND to_uuid = ?')

export function removeFriend(uuid: string, friendUuid: string): FriendsOverview {
  const [a, b] = pair(uuid, friendUuid)
  deleteFriendship.run(a, b)
  deleteInvite.run(uuid, friendUuid)
  deleteInvite.run(friendUuid, uuid)
  return overview(uuid)
}

const upsertInvite = db.prepare(`
  INSERT INTO world_invites (from_uuid, to_uuid, address, version, expires) VALUES (?, ?, ?, ?, ?)
  ON CONFLICT (from_uuid, to_uuid) DO UPDATE SET address = excluded.address, version = excluded.version, expires = excluded.expires
`)
const deleteInvitesFrom = db.prepare('DELETE FROM world_invites WHERE from_uuid = ?')

/** Phase 8b: invite a friend into the caller's world, reachable at `address` (e4mc). Friends only. */
export function sendInvite(fromUuid: string, toUuid: string, address: string, version: string): FriendsOverview {
  const [a, b] = pair(fromUuid, toUuid)
  if (!isFriend.get(a, b)) throw new FriendsError(403, 'not_friends')
  upsertInvite.run(fromUuid, toUuid, address, version, Date.now() + INVITE_TTL_MS)
  return overview(fromUuid)
}

/** Host withdraws the invitation to one friend, or to everyone (world closed) when `toUuid` is null. */
export function revokeInvites(fromUuid: string, toUuid: string | null): FriendsOverview {
  if (toUuid) deleteInvite.run(fromUuid, toUuid)
  else deleteInvitesFrom.run(fromUuid)
  return overview(fromUuid)
}

/** Invited player declines (or has just used it to join). */
export function dismissInvite(uuid: string, fromUuid: string): FriendsOverview {
  deleteInvite.run(fromUuid, uuid)
  return overview(uuid)
}
