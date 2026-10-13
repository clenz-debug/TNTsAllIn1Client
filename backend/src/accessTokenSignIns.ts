import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { config } from './config.js'

/**
 * How many players a day still sign in with their Minecraft access token (launcher 0.14.1 or
 * older), kept in `access-token-sign-ins.txt` next to the database: one `<date> <players>` line per
 * day, numbers only - no names, no UUIDs. The service's journal reaches back only about half a day,
 * too short to tell when that way in can be closed (`TNTCAPES_ACCEPT_ACCESS_TOKENS=0`).
 */
const FILE = join(config.dataDir, 'access-token-sign-ins.txt')
/** Older lines fall out, so the file never grows. */
const MAX_DAYS = 400

let day = ''
const players = new Set<string>()
let dirty = false

/** The server's own date, `YYYY-MM-DD`. */
function today(): string {
  const now = new Date()
  const pad = (value: number): string => String(value).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

export function noteAccessTokenSignIn(uuid: string): void {
  const date = today()
  if (date !== day) {
    flushAccessTokenSignIns()
    day = date
    players.clear()
  }
  if (players.has(uuid)) return
  players.add(uuid)
  dirty = true
}

/** Writes the current day's line. Counting must never take the service down, so a failure is only logged. */
export function flushAccessTokenSignIns(): void {
  if (!dirty) return
  try {
    mkdirSync(config.dataDir, { recursive: true })
    let lines: string[] = []
    try {
      lines = readFileSync(FILE, 'utf8').split('\n').filter((line) => /^\d{4}-\d{2}-\d{2} \d+$/.test(line))
    } catch {
      // No file yet
    }
    const index = lines.findIndex((line) => line.startsWith(`${day} `))
    // After a restart the set starts empty - the day's line keeps the higher number
    const before = index >= 0 ? Number(lines[index].split(' ')[1]) : 0
    const line = `${day} ${Math.max(before, players.size)}`
    if (index >= 0) lines[index] = line
    else lines.push(line)
    writeFileSync(FILE, `${lines.slice(-MAX_DAYS).join('\n')}\n`)
    dirty = false
  } catch (error) {
    console.error('[auth] Could not write the access token sign-in count:', error)
  }
}
