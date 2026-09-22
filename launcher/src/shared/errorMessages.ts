export type ErrorParams = Record<string, string | number>

/**
 * Marker prefixing an encoded error message - astronomically unlikely to occur in any real message,
 * so `formatError.ts` can tell an intentionally-localized error apart from a plain Node/network
 * exception (fs errors, fetch rejections, ...) without a false-positive risk.
 */
const MARKER = '\u0000i18nError\u0000'

/**
 * Main-process throw sites use this instead of a hardcoded German string, so the renderer can show
 * the error in whichever language (`de`/`en`) the user picked - Electron's IPC error propagation
 * only carries `err.message` across to the renderer (see `formatError.ts`), so the code+params are
 * JSON-encoded straight into the message rather than passed as a separate property that IPC would
 * drop. `code` must match a key path under `errors` in `i18n/de.ts`/`i18n/en.ts`.
 */
export function localizedErrorMessage(code: string, params?: ErrorParams): string {
  return MARKER + JSON.stringify({ code, params })
}

export function localizedError(code: string, params?: ErrorParams): Error {
  return new Error(localizedErrorMessage(code, params))
}

export function parseLocalizedError(message: string): { code: string; params?: ErrorParams } | null {
  if (!message.startsWith(MARKER)) return null
  try {
    const parsed = JSON.parse(message.slice(MARKER.length)) as { code: string; params?: ErrorParams }
    return typeof parsed.code === 'string' ? parsed : null
  } catch {
    return null
  }
}

/**
 * Main-process-only: a plain-text stand-in for a caught error, for contexts that never reach the
 * renderer's translation dictionary (`sendLog`'s console window text) - shows the raw code+params
 * instead of the encoded marker string a swallowed `localizedError` would otherwise leak as-is.
 */
export function describeError(err: unknown): string {
  const message = err instanceof Error ? err.message : String(err)
  const localized = parseLocalizedError(message)
  if (!localized) return message
  return localized.params ? `${localized.code} ${JSON.stringify(localized.params)}` : localized.code
}
