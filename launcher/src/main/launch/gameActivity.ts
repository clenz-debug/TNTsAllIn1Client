import { readFile, rm } from 'node:fs/promises'
import { join } from 'node:path'
import type { FriendActivity } from '../../shared/types'

/**
 * Where the running game currently is, for the friends presence (Phase 8). Our mod writes it to
 * `config/tntsallin1client-activity.json` in the game directory (`ActivityReporter.java`) whenever
 * it changes; this side only reads that file while a launch it started is running.
 */

let runningGameDir: string | null = null

function activityFile(gameDir: string): string {
  return join(gameDir, 'config', 'tntsallin1client-activity.json')
}

/** Called right before the game starts - drops a file a previous run may have left behind. */
export async function setGameRunning(gameDir: string): Promise<void> {
  await rm(activityFile(gameDir), { force: true })
  runningGameDir = gameDir
}

export function setGameStopped(): void {
  runningGameDir = null
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
