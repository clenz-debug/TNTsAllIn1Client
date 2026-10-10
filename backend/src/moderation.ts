import { createHash } from 'node:crypto'
import { mkdirSync, readdirSync, rmSync, writeFileSync } from 'node:fs'
import { readFile } from 'node:fs/promises'
import { join } from 'node:path'
import { deleteCape, readCape } from './capes.js'
import { config } from './config.js'
import { banCapes, capeBans, db, unbanCapes } from './friends.js'

/**
 * Reporting capes and deciding about the reports. Any signed-in player can report another player's
 * cape; the moderators (`config.moderators`) see the open reports in their launcher and dismiss
 * them, remove the cape or bar the account from uploading.
 *
 * A report keeps a copy of the cape as it was at that moment (`reported-capes/<sha256>.png` in the
 * data folder, never public) - otherwise swapping the cape right after showing it around would make
 * every report pointless. The copies go once the reports about them are decided.
 */

/** Open reports all together - reporting is rate-limited per player, this is the hard stop behind it. */
const MAX_OPEN_REPORTS = 2000
const SHA_PATTERN = /^[0-9a-f]{64}$/

const evidenceDir = join(config.dataDir, 'reported-capes')
mkdirSync(evidenceDir, { recursive: true })

export class ModerationError extends Error {
  constructor(
    readonly status: number,
    readonly code: string
  ) {
    super(code)
  }
}

function evidencePath(sha: string): string {
  return join(evidenceDir, `${sha}.png`)
}

const countReports = db.prepare('SELECT COUNT(*) AS n FROM cape_reports')
const insertReport = db.prepare(`
  INSERT OR IGNORE INTO cape_reports (reporter, target, target_name, reason, cape_sha, created) VALUES (?, ?, ?, ?, ?, ?)
`)
const selectReports = db.prepare('SELECT target, target_name, reason, cape_sha, created FROM cape_reports ORDER BY created')
const deleteReportsAbout = db.prepare('DELETE FROM cape_reports WHERE target = ?')
const selectEvidenceInUse = db.prepare('SELECT DISTINCT cape_sha FROM cape_reports')

/** Reports `targetUuid`'s current cape. Reporting the same cape twice changes nothing. */
export async function reportCape(reporterUuid: string, targetUuid: string, targetName: string, reason: string): Promise<void> {
  if (reporterUuid === targetUuid) throw new ModerationError(400, 'cannot_report_self')
  const cape = await readCape(targetUuid)
  if (!cape) throw new ModerationError(404, 'no_cape')
  if ((countReports.get() as { n: number }).n >= MAX_OPEN_REPORTS) throw new ModerationError(503, 'busy')
  const sha = createHash('sha256').update(cape).digest('hex')
  writeFileSync(evidencePath(sha), cape, { mode: 0o600 })
  insertReport.run(reporterUuid, targetUuid, targetName, reason, sha, Date.now())
}

export interface OpenReport {
  target: string
  /** As the first reporter typed it - a label, not checked against Mojang. */
  name: string
  /** Which copy the reports are about, see {@link reportedCape}. */
  sha: string
  count: number
  /** What the reporters wrote, without the empty ones. */
  reasons: string[]
  created: number
  /** The player still wears exactly this cape. */
  current: boolean
}

/** Open reports, one entry per player and reported cape, oldest first. Who reported is not part of it. */
export async function openReports(): Promise<OpenReport[]> {
  const rows = selectReports.all() as unknown as Array<{ target: string; target_name: string; reason: string; cape_sha: string; created: number }>
  const grouped = new Map<string, OpenReport>()
  for (const row of rows) {
    const key = `${row.target}/${row.cape_sha}`
    const entry = grouped.get(key) ?? { target: row.target, name: row.target_name, sha: row.cape_sha, count: 0, reasons: [], created: row.created, current: false }
    entry.count++
    if (row.reason && entry.reasons.length < 10) entry.reasons.push(row.reason)
    grouped.set(key, entry)
  }
  const reports = [...grouped.values()]
  for (const report of reports) {
    const cape = await readCape(report.target)
    report.current = cape !== null && createHash('sha256').update(cape).digest('hex') === report.sha
  }
  return reports
}

/** The copy of a reported cape, `null` if there is none (any more). */
export async function reportedCape(sha: string): Promise<Buffer | null> {
  if (!SHA_PATTERN.test(sha)) return null
  return readFile(evidencePath(sha)).catch(() => null)
}

/** Copies no report refers to any more - also covers reports that went with a deleted account. */
export function pruneReportedCapes(): void {
  const inUse = new Set((selectEvidenceInUse.all() as unknown as Array<{ cape_sha: string }>).map((row) => `${row.cape_sha}.png`))
  for (const file of readdirSync(evidenceDir)) {
    if (!inUse.has(file)) rmSync(join(evidenceDir, file), { force: true })
  }
}

export type Decision = 'dismiss' | 'remove' | 'ban'
export const DECISIONS: readonly Decision[] = ['dismiss', 'remove', 'ban']

/**
 * Closes every report about `targetUuid`: `dismiss` leaves the cape alone, `remove` deletes it,
 * `ban` deletes it and refuses further uploads from that account (`reason` is kept with the ban).
 */
export async function decideReports(targetUuid: string, decision: Decision, reason: string): Promise<void> {
  if (decision === 'ban') banCapes(targetUuid, reason)
  if (decision !== 'dismiss') await deleteCape(targetUuid)
  deleteReportsAbout.run(targetUuid)
  pruneReportedCapes()
}

export { capeBans, unbanCapes }
