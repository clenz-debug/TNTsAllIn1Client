import { localizedError } from '../../shared/errorMessages'
import type { CapeDecision, CapeModerationState } from '../../shared/types'
import { BackendError, backendFetch, isBackendModerator } from '../backend/backendSession'

/**
 * Reporting another player's custom cape, and - for the project's moderators only - deciding about
 * the reports (`backend/src/moderation.ts`). Everybody reports by Minecraft name; the server keeps a
 * copy of the cape as it was at that moment, so swapping it afterwards doesn't help.
 */

const NAME_LOOKUP_URL = 'https://api.minecraftservices.com/minecraft/profile/lookup/name'
const TIMEOUT_MS = 10_000
/** Reported capes shown at once - each one is a download of up to 5 MB. */
const MAX_SHOWN_REPORTS = 30

/** A call to our server; anything but an OK answer becomes one of the launcher's own de/en messages. */
async function call(method: string, path: string, json?: unknown): Promise<Response> {
  let response: Response
  try {
    response = await backendFetch(method, path, { json, timeoutMs: TIMEOUT_MS })
  } catch (err) {
    if (!(err instanceof BackendError)) throw err
    throw localizedError(err.code === 'unauthorized' || err.code === 'not_logged_in' ? 'cape.unauthorized' : 'capeReport.failed', { detail: err.code })
  }
  if (response.ok) return response
  const { error } = (await response.json().catch(() => ({}))) as { error?: unknown }
  switch (error) {
    case 'no_cape':
      throw localizedError('capeReport.noCape')
    case 'cannot_report_self':
      throw localizedError('capeReport.self')
    case 'rate_limited':
      throw localizedError('capeReport.rateLimited')
    case 'unauthorized':
      throw localizedError('cape.unauthorized')
    default:
      throw localizedError('capeReport.failed', { detail: typeof error === 'string' ? error : `HTTP ${response.status}` })
  }
}

/** Reports the custom cape `name` is wearing right now. */
export async function reportCape(name: string, reason: string): Promise<void> {
  const trimmed = name.trim()
  if (!/^\w{1,16}$/.test(trimmed)) throw localizedError('capeReport.noPlayer')
  // Mojang knows the UUID behind a name - our server only knows players by UUID
  const lookup = await fetch(`${NAME_LOOKUP_URL}/${encodeURIComponent(trimmed)}`, { signal: AbortSignal.timeout(TIMEOUT_MS) })
  if (lookup.status === 404) throw localizedError('capeReport.noPlayer')
  if (!lookup.ok) throw localizedError('capeReport.failed', { detail: `Mojang HTTP ${lookup.status}` })
  const player = (await lookup.json()) as { id?: unknown; name?: unknown }
  if (typeof player.id !== 'string' || typeof player.name !== 'string') throw localizedError('capeReport.failed', { detail: 'Mojang' })
  await call('POST', '/capes/report', { uuid: player.id.replace(/-/g, '').toLowerCase(), name: player.name, reason: reason.trim() })
}

/** Whether the logged-in account is one of the project's moderators - never throws. */
export function isCapeModerator(): Promise<boolean> {
  return isBackendModerator()
}

interface ServerState {
  reports: Array<Omit<CapeModerationState['reports'][number], 'dataUri'>>
  bans: CapeModerationState['bans']
}

async function withPictures(state: ServerState): Promise<CapeModerationState> {
  const reports: CapeModerationState['reports'] = []
  for (const report of state.reports.slice(0, MAX_SHOWN_REPORTS)) {
    const picture = await backendFetch('GET', `/admin/reported-capes/${report.sha}`, { timeoutMs: TIMEOUT_MS }).catch(() => null)
    const dataUri = picture?.ok ? `data:image/png;base64,${Buffer.from(await picture.arrayBuffer()).toString('base64')}` : null
    reports.push({ ...report, dataUri })
  }
  return { reports, bans: state.bans }
}

export async function listCapeReports(): Promise<CapeModerationState> {
  return withPictures((await (await call('GET', '/admin/reports')).json()) as ServerState)
}

export async function decideCapeReports(target: string, decision: CapeDecision, reason: string): Promise<CapeModerationState> {
  return withPictures((await (await call('POST', '/admin/decide', { target, decision, reason: reason.trim() })).json()) as ServerState)
}

export async function liftCapeBan(uuid: string): Promise<CapeModerationState> {
  return withPictures((await (await call('POST', '/admin/unban', { uuid })).json()) as ServerState)
}
