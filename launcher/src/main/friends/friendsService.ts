import { app, BrowserWindow } from 'electron'
import { randomUUID } from 'node:crypto'
import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { localizedError } from '../../shared/errorMessages'
import { IpcChannel } from '../../shared/ipc'
import { DEFAULT_THEME_COLORS } from '../../shared/types'
import type { FriendsOverview, FriendsPrefs, FriendsState, FriendsStatus, ThemeColors } from '../../shared/types'
import { BackendError, backendFetch, endBackendSession } from '../backend/backendSession'
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
 * friends overview back in the same answer, which is then pushed to every window. Signing in is
 * `backendSession.ts` - the Minecraft access token is never sent to our backend.
 */

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

/** Calls the backend as the logged-in player (signed in through `backendSession.ts`). */
async function call<T>(method: string, path: string, body?: unknown): Promise<T> {
  let response: Response
  try {
    response = await backendFetch(method, path, { json: body, timeoutMs: REQUEST_TIMEOUT_MS })
  } catch (err) {
    throw err instanceof BackendError ? new FriendsApiError(err.code) : err
  }
  const data = (await response.json().catch(() => ({}))) as { error?: unknown }
  if (!response.ok) {
    throw new FriendsApiError(typeof data.error === 'string' ? data.error : `http_${response.status}`)
  }
  return data as T
}

/** Our server only ever passes on e4mc addresses (`backend/src/server.ts`) - checked here again, so
 * that not even the server could send a player to a Minecraft server of its choosing. */
const INVITE_ADDRESS_PATTERN = /^(?=.{1,253}(?::|$))([a-z0-9-]+\.)+e4mc\.link(:\d{1,5})?$/i

/** A call that answers with the friends overview. */
async function callOverview(method: string, path: string, body?: unknown): Promise<FriendsOverview> {
  const answer = await call<FriendsOverview>(method, path, body)
  return {
    ...answer,
    invites: (answer.invites ?? []).filter((invite) => INVITE_ADDRESS_PATTERN.test(invite.address)),
    // A server from before blocking existed does not send the list
    blocked: answer.blocked ?? []
  }
}

/** Every code the renderer has a de/en text for (`errors.friends` in i18n) - anything else is
 * shown as `unknown` rather than as a raw code. */
const KNOWN_ERROR_CODES = new Set([
  'not_logged_in',
  'unauthorized',
  'unreachable',
  'auth_unavailable',
  'multiplayer_blocked',
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
  'unblock_first',
  'too_many_blocked',
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
    const current = (await currentGameActivity()) ?? { kind: 'launcher' }
    // Nobody is shown the server while it is hidden or the player is invisible - so it is not sent either
    const secret = prefs.hideServer || prefs.status === 'invisible'
    const activity = secret && current.kind === 'multiplayer' ? { kind: current.kind } : current
    const logoColors = await currentLogoColors()
    overview = await callOverview('POST', '/presence', { status: prefs.status, hideServer: prefs.hideServer, activity, logoColors })
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
        overview = await callOverview('DELETE', '/invites').catch(() => overview)
      }
      return
    }

    const commands = await takeOutboxCommands()
    for (const command of commands) {
      try {
        if (command.type === 'invite') {
          overview = await callOverview('POST', '/invites', { to: command.to, address: command.address, version: command.version })
          invitedThisGame = true
        } else if (command.type === 'revoke') {
          overview = await callOverview('DELETE', `/invites/${encodeURIComponent(command.to)}`)
        } else if (command.type === 'revokeAll') {
          overview = await callOverview('DELETE', '/invites')
          invitedThisGame = false
        } else if (command.type === 'dismiss') {
          overview = await callOverview('POST', `/invites/${encodeURIComponent(command.from)}/dismiss`)
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
  if (!INVITE_ADDRESS_PATTERN.test(address)) return
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
  await endBackendSession()
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
    overview = await callOverview(method, path, body)
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

/** Blocking also removes the friendship and any open request between the two. */
export function blockPlayer(uuid: string): Promise<FriendsState> {
  return action('POST', '/friends/blocks', { uuid })
}

export function unblockPlayer(uuid: string): Promise<FriendsState> {
  return action('DELETE', `/friends/blocks/${encodeURIComponent(uuid)}`)
}

/**
 * "Delete my data": the server forgets the player completely - friends, requests, blocked players,
 * status and the active cape. Friends are switched off first and stay off: the next status report
 * would register the player again right away.
 */
export async function deleteServerData(): Promise<FriendsState> {
  await setFriendsPrefs({ ...prefs, enabled: false })
  try {
    await call('DELETE', '/account')
  } catch (err) {
    throw localizedError(`friends.${errorCode(err)}`)
  }
  await endBackendSession()
  return getFriendsState()
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
