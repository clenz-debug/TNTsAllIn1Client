import { mkdir, readdir, readFile, rm, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import type { FriendActivity } from '../../shared/types'

/**
 * The file bridge between the launcher and our mod while a game it started is running (Phase 8,
 * friends). The launcher stays the only one talking to the friends backend; the mod and it just
 * exchange small files in the game's `config/`:
 *  - `tntsallin1client-activity.json` (mod → launcher): main menu / singleplayer / server, for the
 *    presence (`ActivityReporter.java`)
 *  - `tntsallin1client-friends.json` (launcher → mod): online friends, open world invitations,
 *    a pending "join this server" request, results of the mod's commands (`FriendsBridge.java`)
 *  - `tntsallin1client-outbox/<id>.json` (mod → launcher): one file per command (invite a friend,
 *    withdraw invitations), read and deleted by the launcher
 */

let runningGameDir: string | null = null

function configDir(gameDir: string): string {
  return join(gameDir, 'config')
}

function activityFile(gameDir: string): string {
  return join(configDir(gameDir), 'tntsallin1client-activity.json')
}

function inboxFile(gameDir: string): string {
  return join(configDir(gameDir), 'tntsallin1client-friends.json')
}

function outboxDir(gameDir: string): string {
  return join(configDir(gameDir), 'tntsallin1client-outbox')
}

/** Called right before the game starts - drops whatever a previous run left behind. */
export async function setGameRunning(gameDir: string): Promise<void> {
  await Promise.all([
    rm(activityFile(gameDir), { force: true }),
    rm(inboxFile(gameDir), { force: true }),
    rm(outboxDir(gameDir), { recursive: true, force: true })
  ])
  runningGameDir = gameDir
}

export function setGameStopped(): void {
  runningGameDir = null
}

export function isGameRunning(): boolean {
  return runningGameDir !== null
}

/** `null` when no game is running. A running game without the file (no mod in that version) is
 * `playing` - somewhere in the game, but unknown where. */
export async function currentGameActivity(): Promise<FriendActivity | null> {
  const gameDir = runningGameDir
  if (!gameDir) return null
  try {
    const parsed = JSON.parse(await readFile(activityFile(gameDir), 'utf8')) as { kind?: unknown; server?: unknown }
    if (parsed.kind === 'menu' || parsed.kind === 'singleplayer') return { kind: parsed.kind }
    if (parsed.kind === 'multiplayer') {
      return typeof parsed.server === 'string' && parsed.server ? { kind: 'multiplayer', server: parsed.server } : { kind: 'multiplayer' }
    }
  } catch {
    // not written (yet)
  }
  return { kind: 'playing' }
}

let lastInbox: string | null = null

/** Hands the mod the current friends data - only rewritten when it actually changed. */
export async function writeGameInbox(data: unknown): Promise<void> {
  const gameDir = runningGameDir
  if (!gameDir) return
  const serialized = JSON.stringify(data)
  if (serialized === lastInbox) return
  await mkdir(configDir(gameDir), { recursive: true })
  await writeFile(inboxFile(gameDir), serialized, 'utf8')
  lastInbox = serialized
}

export function resetGameInbox(): void {
  lastInbox = null
}

/** A command the mod left for the launcher (`FriendsBridge.java`). */
export type OutboxCommand =
  | { id: string; type: 'invite'; to: string; address: string; version: string }
  | { id: string; type: 'revoke'; to: string }
  | { id: string; type: 'revokeAll' }
  | { id: string; type: 'dismiss'; from: string }

/** Reads and removes every pending command, oldest first. Unreadable files are dropped - the mod
 * writes each one atomically (temp file + rename), so that's only ever garbage. */
export async function takeOutboxCommands(): Promise<OutboxCommand[]> {
  const gameDir = runningGameDir
  if (!gameDir) return []
  const dir = outboxDir(gameDir)
  const names = (await readdir(dir).catch(() => [] as string[])).filter((name) => name.endsWith('.json')).sort()
  const commands: OutboxCommand[] = []
  for (const name of names) {
    const path = join(dir, name)
    try {
      commands.push(JSON.parse(await readFile(path, 'utf8')) as OutboxCommand)
    } catch {
      // see above
    }
    await rm(path, { force: true })
  }
  return commands
}
