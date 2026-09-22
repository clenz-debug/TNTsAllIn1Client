import { parseLocalizedError, type ErrorParams } from '../../shared/errorMessages'
import type { useTranslations } from './i18n/LanguageContext'

type Translations = ReturnType<typeof useTranslations>

/** Walks a dot-path (`"cape.notConfigured"`) into `t.errors` - codes are nested by domain (see
 * `i18n/de.ts`'s `errors` doc comment), a flat lookup table would need one entry per code anyway. */
function resolveErrorEntry(errors: Translations['errors'], code: string): unknown {
  return code.split('.').reduce<unknown>((obj, key) => (obj as Record<string, unknown> | undefined)?.[key], errors)
}

/** Strips Electron's `ipcMain.handle` wrapper (`Error invoking remote method '<channel>': Error:
 * <message>`) off a caught IPC error's message, leaving whatever the main process actually threw -
 * shared by {@link formatError} and {@link errorCode} so neither has to know Electron's wrapper
 * shape on its own. */
function stripIpcWrapper(err: unknown): string | null {
  if (!(err instanceof Error)) return null
  return err.message.replace(/^Error invoking remote method '[^']+':\s*(?:Error:\s*)?/, '')
}

/** The bare `localizedError` code behind a caught IPC error (e.g. `"launch.cancelled"`), or `null`
 * for anything else (a not-yet-migrated error, or a non-`Error` throw) - lets a caller branch on
 * *which* error this was before falling back to {@link formatError}'s translated text, for the rare
 * case a specific code needs different handling entirely rather than just a different string (own
 * user request: `PlayScreen`'s Cancel button needs to show a plain "Abgebrochen." status, not a red
 * error log line, when `launch.cancelled` comes back from an aborted `LaunchPlay`). */
export function errorCode(err: unknown): string | null {
  const stripped = stripIpcWrapper(err)
  return stripped ? (parseLocalizedError(stripped)?.code ?? null) : null
}

/**
 * Turns a caught IPC error into the plain, localized message a main-process handler actually meant -
 * own user request, after `installModrinthMod`'s incompatibility check surfaced how it really looks
 * unformatted: Electron wraps every error a `ipcMain.handle` callback throws before it reaches
 * `ipcRenderer.invoke`'s rejection, so `err.message` here is normally
 * `Error invoking remote method '<channel>': Error: <the actual message>` - technically correct,
 * but the "Error invoking remote method" part means nothing to a user and was never meant to be
 * shown to one. Every screen's error handling (`setError(formatError(err, t))`) should go through
 * this instead so the wrapper never appears anywhere.
 *
 * A migrated main-process throw site (`shared/errorMessages.ts#localizedError`) encodes a code+params
 * pair into the message instead of a hardcoded German string - if present, this looks the code up in
 * `t.errors` (own follow-up request: error messages should respect the language setting too, not
 * just the UI chrome). An error that was never migrated (a raw Node/fetch exception, for instance)
 * has no such encoding and is shown as-is, same as before.
 */
export function formatError(err: unknown, t: Translations): string {
  const stripped = stripIpcWrapper(err)
  if (stripped === null) return String(err)
  const localized = parseLocalizedError(stripped)
  if (localized) {
    const entry = resolveErrorEntry(t.errors, localized.code)
    if (typeof entry === 'function') return (entry as (params: ErrorParams) => string)(localized.params ?? {})
    if (typeof entry === 'string') return entry
  }
  return stripped
}
