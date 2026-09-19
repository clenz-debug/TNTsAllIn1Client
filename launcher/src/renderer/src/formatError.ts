/**
 * Turns a caught IPC error into the plain message a main-process handler actually threw - own user
 * request, after `installModrinthMod`'s incompatibility check surfaced how it really looks
 * unformatted: Electron wraps every error a `ipcMain.handle` callback throws before it reaches
 * `ipcRenderer.invoke`'s rejection, so `err.message` here is normally
 * `Error invoking remote method '<channel>': Error: <the actual message>` - technically correct,
 * but the "Error invoking remote method" part means nothing to a user and was never meant to be
 * shown to one. Every screen's error handling (`setError(err instanceof Error ? err.message :
 * String(err))`) should go through this instead so the wrapper never appears anywhere.
 */
export function formatError(err: unknown): string {
  if (!(err instanceof Error)) return String(err)
  return err.message.replace(/^Error invoking remote method '[^']+':\s*(?:Error:\s*)?/, '')
}
