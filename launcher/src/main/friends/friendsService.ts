import { app, BrowserWindow } from 'electron'
import { randomUUID } from 'node:crypto'
import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { localizedError } from '../../shared/errorMessages'
import { IpcChannel } from '../../shared/ipc'
import { DEFAULT_THEME_COLORS } from '../../shared/types'
import type { FriendsOverview, FriendsPrefs, FriendsState, FriendsStatus, ThemeColors } from '../../shared/types'
import { tryRestoreSession } from '../auth'
import { loadCachedAuth } from '../auth/tokenCache'
import {
  currentGameActivity,
  currentGamePlayers,
  isGameRunning,
  resetGameInbox,
  takeOutboxCommands,
  writeGameInbox
} from '../launch/gameActivity'
import { loadLauncherSettings } from '../launcherSettings'
import { isNetworkError } from '../offline'

/**
 * Friends and presence (Phase 8, own user request) against our backend (`backend/src/friends.ts`
 * behind `https://nxlc.de/app/`). Lives in the main process so presence keeps going no matter which
 * screen is open: every {@link PING_INTERVAL_MS} it reports status + activity and gets the whole
 * friends overview back in the same answer, which is then pushed to every window. Auth is the
 * cached Minecraft access token; an expired one (24 h) is renewed once through the normal silent
 * re-login.
 */

const API_BASE = 'https://nxlc.de/app'
const PING_INTERVAL_MS = 20_000
const REQUEST_TIMEOUT_MS = 10_000
const STATUSES: readonly FriendsStatus[] = ['online', 'away', 'dnd', 'invisible']

const DEFAULT_PREFS: FriendsPrefs = { enabled: true, status: 'online', hideServer: false }

let prefs: FriendsPrefs = DEFAULT_PREFS
let overview: FriendsOverview | null = null
let error: string | null = null
let timer: NodeJS.Timeout | null = null
let prefsLoaded = false
/** The play screen asked for friends (online profile shown) - whether they actually run also depends
 * on {@link FriendsPrefs.enabled}. */
let wanted = false

function prefsPath(): string {
  return join(app.getPath('userData'), 'friends-settings.json')
}

async function loadPrefs(): Promise<void> {
  if (prefsLoaded) return
  prefsLoaded = true
  try {
    const parsed = JSON.parse(await readFile(prefsPath(), 'utf8')) as Partial<FriendsPrefs>
    prefs = {
      enabled: parsed.enabled !== false,
      status: STATUSES.includes(parsed.status as FriendsStatus) ? (parsed.status as FriendsStatus) : DEFAULT_PREFS.status,
      hideServer: parsed.hideServer === true
    }
  } catch {
    prefs = DEFAULT_PREFS
  }
}

export function getFriendsState(): FriendsState {
  return { overview, prefs, error }
}

function broadcast(): void {
  const state = getFriendsState()
  for (const window of BrowserWindow.getAllWindows()) {
    window.webContents.send(IpcChannel.FriendsState, state)
  }
}

/** A backend answer that isn't OK - `code` is the backend's own error code, shown via i18n. */
class FriendsApiError extends Error {
  constructor(readonly code: string) {
    super(code)
  }
}

async function send(token: string, method: string, path: string, body?: unknown): Promise<Response> {
  return fetch(`${API_BASE}${path}`, {
    method,
    headers: { Authorization: `Bearer ${token}`, ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}) },
    body: body !== undefined ? JSON.stringify(body) : undefined,
    signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS)
  })
}

/** Calls the backend as the logged-in player; on 401 renews the session once and retries. */
async function call<T>(method: string, path: string, body?: unknown): Promise<T> {
  const cached = await loadCachedAuth()
  if (!cached) throw new FriendsApiError('not_logged_in')
  let response = await send(cached.profile.accessToken, method, path, body)
  if (response.status === 401) {
    const renewed = await tryRestoreSession()
    if (!renewed || renewed.offline) throw new FriendsApiError('unauthorized')
    response = await send(renewed.accessToken, method, path, body)
  }
  const data = (await response.json().catch(() => ({}))) as { error?: unknown }
  if (!response.ok) {
    throw new FriendsApiError(typeof data.error === 'string' ? data.error : `http_${response.status}`)
  }
  return data as T
}

/** Every code the renderer has a de/en text for (`errors.friends` in i18n) - anything else is
 * shown as `unknown` rather than as a raw code. */
const KNOWN_ERROR_CODES = new Set([
  'not_logged_in',
  'unauthorized',
  'unreachable',
  'auth_unavailable',
  'rate_limited',
  'invalid_name',
  'player_not_found',
  'cannot_add_self',
  'already_friends',
  'already_requested',
  'too_many_friends',
  'too_many_requests',
  'request_not_found',
  'not_friends',
  'invalid_body',
  'unknown'
])

function errorCode(err: unknown): string {
  if (err instanceof FriendsApiError) return KNOWN_ERROR_CODES.has(err.code) ? err.code : 'unknown'
  if (isNetworkError(err)) return 'unreachable'
  return 'unknown'
}

/** The theme colors other players' games draw our nametag logo in - the same ones `Logo.tsx` uses. */
type LogoColors = Pick<ThemeColors, 'background1' | 'background2' | 'accent1' | 'accent2' | 'accent3' | 'accent4'>

async function currentLogoColors(): Promise<LogoColors> {
  const { background1, background2, accent1, accent2, accent3, accent4 } =
    (await loadLauncherSettings()).themeColors ?? DEFAULT_THEME_COLORS
  return { background1, background2, accent1, accent2, accent3, accent4 }
}

async function ping(): Promise<void> {
  try {
    const activity = (await currentGameActivity()) ?? { kind: 'launcher' }
    const logoColors = await currentLogoColors()
    overview = await call<FriendsOverview>('POST', '/presence', { status: prefs.status, hideServer: prefs.hideServer, activity, logoColors })
    error = null
  } catch (err) {
    error = errorCode(err)
  }
  broadcast()
}

// --- Bridge to the running game (Phase 8b) -----------------------------------------------------
// See launch/gameActivity.ts for the files. Checked every second while friends are running, so an
// invitation sent from the pause menu reaches the backend right away, not only with the next ping.

const BRIDGE_INTERVAL_MS = 1_000
const JOIN_REQUEST_TTL_MS = 30_000
const MAX_RESULTS = 20

let bridgeTimer: NodeJS.Timeout | null = null
let bridgeBusy = false
/** The mod invited someone during this game - withdrawn automatically when the game ends, in case
 * the mod couldn't do it itself (crash, killed process). */
let invitedThisGame = false
let pendingJoin: { id: string; address: string; expires: number } | null = null
const results = new Map<string, string>()

// Nametag logo: the mod lists the players in its tab list, we ask the backend which of them use our
// client (and in which colors) and hand the answer back in the inbox. Due players are looked up at
// most every LOOKUP_INTERVAL_MS, batched, so even a busy server stays far below the backend's
// per-player rate limit. Every answer is asked again after RECHECK_MS - picks up someone who installed
// the client meanwhile, and changed theme colors.
const LOOKUP_INTERVAL_MS = 10_000
const LOOKUP_MAX_UUIDS = 100
const RECHECK_MS = 10 * 60_000

interface ClientUser {
  uuid: string
  /** `null` when that player's launcher hasn't reported colors yet - the mod uses the default theme. */
  colors: LogoColors | null
}

/** Per UUID: the backend's answer (`null` = not a client user) and when it was asked. */
const clientUserCache = new Map<string, { user: ClientUser | null; checked: number }>()
let lastLookup = 0

async function clientUsersAmong(players: string[]): Promise<ClientUser[]> {
  const now = Date.now()
  const due = players.filter((uuid) => {
    const entry = clientUserCache.get(uuid)
    return !entry || now - entry.checked > RECHECK_MS
  })
  if (due.length > 0 && now - lastLookup >= LOOKUP_INTERVAL_MS) {
    lastLookup = now
    const batch = due.slice(0, LOOKUP_MAX_UUIDS)
    try {
      const { clientUsers } = await call<{ clientUsers: ClientUser[] }>('POST', '/players/lookup', { uuids: batch })
      const found = new Map(clientUsers.map((user) => [user.uuid, user]))
      for (const uuid of batch) clientUserCache.set(uuid, { user: found.get(uuid) ?? null, checked: now })
    } catch (err) {
      // Unreachable, or a backend without the lookup yet - no logos for now, asked again next interval.
      console.warn('[friends] Client user lookup failed:', err instanceof FriendsApiError ? err.code : err)
    }
  }
  return players.flatMap((uuid) => {
    const user = clientUserCache.get(uuid)?.user
    return user ? [user] : []
  })
}

async function bridgeTick(): Promise<void> {
  if (bridgeBusy) return
  bridgeBusy = true
  try {
    if (!isGameRunning()) {
      pendingJoin = null
      results.clear()
      clientUserCache.clear()
      lastLookup = 0
      resetGameInbox()
      if (invitedThisGame) {
        invitedThisGame = false
        overview = await call<FriendsOverview>('DELETE', '/invites').catch(() => overview)
      }
      return
    }

    const commands = await takeOutboxCommands()
    for (const command of commands) {
      try {
        if (command.type === 'invite') {
          overview = await call<FriendsOverview>('POST', '/invites', { to: command.to, address: command.address, version: command.version })
          invitedThisGame = true
        } else if (command.type === 'revoke') {
          overview = await call<FriendsOverview>('DELETE', `/invites/${encodeURIComponent(command.to)}`)
        } else if (command.type === 'revokeAll') {
          overview = await call<FriendsOverview>('DELETE', '/invites')
          invitedThisGame = false
        } else if (command.type === 'dismiss') {
          overview = await call<FriendsOverview>('POST', `/invites/${encodeURIComponent(command.from)}/dismiss`)
        }
        results.set(command.id, 'ok')
      } catch (err) {
        results.set(command.id, errorCode(err))
      }
      while (results.size > MAX_RESULTS) results.delete(results.keys().next().value as string)
    }

    if (commands.length > 0) broadcast()
    if (pendingJoin && pendingJoin.expires < Date.now()) pendingJoin = null
    await writeGameInbox({
      dnd: prefs.status === 'dnd',
      friends: (overview?.friends ?? [])
        .filter((friend) => friend.status !== 'offline')
        .map((friend) => ({ uuid: friend.uuid, name: friend.name, status: friend.status })),
      invites: overview?.invites ?? [],
      join: pendingJoin ? { id: pendingJoin.id, address: pendingJoin.address } : null,
      results: Object.fromEntries(results),
      clientUsers: await clientUsersAmong(await currentGamePlayers())
    })
  } catch (err) {
    console.warn('[friends] Game bridge failed:', err)
  } finally {
    bridgeBusy = false
  }
}

/** Friends screen's "join" while the game already runs: the mod connects there itself. */
export function joinInGame(address: string): void {
  pendingJoin = { id: randomUUID(), address, expires: Date.now() + JOIN_REQUEST_TTL_MS }
  void bridgeTick()
}

/** Starts pinging - a no-op if already running, or while the player has friends switched off. */
async function run(): Promise<void> {
  if (timer || !prefs.enabled) return
  timer = setInterval(() => void ping(), PING_INTERVAL_MS)
  bridgeTimer = setInterval(() => void bridgeTick(), BRIDGE_INTERVAL_MS)
  await ping()
}

/** PlayScreen shown with an online profile. */
export async function startFriends(): Promise<void> {
  await loadPrefs()
  wanted = true
  await run()
  // Switched off: no ping pushes the state, but the screen still has to learn that it is off
  if (!timer) broadcast()
}

/** Logout or offline mode. */
export async function stopFriends(announce: boolean): Promise<void> {
  wanted = false
  await halt(announce)
}

/** Stops pinging and forgets what we knew. Friends see us as offline once the backend timeout
 * passes (or right away if `announce` is set). */
async function halt(announce: boolean): Promise<void> {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  if (bridgeTimer) {
    clearInterval(bridgeTimer)
    bridgeTimer = null
  }
  const wasRunning = overview !== null
  overview = null
  error = null
  broadcast()
  if (announce && wasRunning) await call('POST', '/presence/offline').catch(() => undefined)
}

export async function setFriendsPrefs(next: FriendsPrefs): Promise<FriendsState> {
  await loadPrefs()
  prefs = {
    enabled: next.enabled !== false,
    status: STATUSES.includes(next.status) ? next.status : prefs.status,
    hideServer: next.hideServer === true
  }
  await mkdir(app.getPath('userData'), { recursive: true })
  await writeFile(prefsPath(), JSON.stringify(prefs, null, 2), 'utf8')
  if (!prefs.enabled) {
    // Switched off: nothing goes to the friends server anymore, and a running game stops showing
    // friends, invitations and client logos.
    const wasRunning = timer !== null
    await halt(true)
    if (wasRunning && isGameRunning()) {
      await writeGameInbox({ dnd: false, friends: [], invites: [], join: null, results: {}, clientUsers: [] }).catch(() => undefined)
    }
  } else if (timer) {
    await ping()
  } else if (wanted) {
    await run()
  } else {
    broadcast()
  }
  return getFriendsState()
}

/** Runs one friends action and pushes the new overview; failures are thrown to the caller (the
 * Friends screen shows them next to the action) and don't touch the ping error. */
async function action(method: string, path: string, body?: unknown): Promise<FriendsState> {
  try {
    overview = await call<FriendsOverview>(method, path, body)
  } catch (err) {
    throw localizedError(`friends.${errorCode(err)}`)
  }
  broadcast()
  return getFriendsState()
}

export function sendFriendRequest(name: string): Promise<FriendsState> {
  return action('POST', '/friends/requests', { name: name.trim() })
}

export function acceptFriendRequest(uuid: string): Promise<FriendsState> {
  return action('POST', `/friends/requests/${encodeURIComponent(uuid)}/accept`)
}

export function removeFriendRequest(uuid: string): Promise<FriendsState> {
  return action('DELETE', `/friends/requests/${encodeURIComponent(uuid)}`)
}

export function removeFriend(uuid: string): Promise<FriendsState> {
  return action('DELETE', `/friends/${encodeURIComponent(uuid)}`)
}

/** Invited player declines a world invitation - or has just used it to join. */
export function dismissInvite(fromUuid: string): Promise<FriendsState> {
  return action('POST', `/invites/${encodeURIComponent(fromUuid)}/dismiss`)
}

/** Launcher closing: tell friends right away instead of after the backend's timeout - but never
 * hold up quitting for more than {@link QUIT_ANNOUNCE_MAX_MS}. */
const QUIT_ANNOUNCE_MAX_MS = 2_000

export async function announceOffline(): Promise<void> {
  if (!timer) return
  await Promise.race([stopFriends(true), new Promise((resolve) => setTimeout(resolve, QUIT_ANNOUNCE_MAX_MS))])
}
