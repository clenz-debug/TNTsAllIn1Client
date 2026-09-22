import { BrowserWindow } from 'electron'
import { join } from 'node:path'
import { DEFAULT_THEME_COLORS } from '../shared/types'
import { loadLauncherSettings } from './launcherSettings'

/**
 * Settings screen's "Konsole in separatem Fenster anzeigen" toggle (own wishlist item) -
 * `PlayScreen#handlePlay` opens this instead of relying purely on its own inline log panel. A
 * single module-level singleton is enough (same reasoning as `ipc/handlers.ts`'s `storageBusy`
 * flag): this app never runs more than one instance of itself, and there is only ever one useful
 * console window at a time - a second click just focuses the existing one.
 */
let consoleWindow: BrowserWindow | null = null

/** Loads the same renderer bundle as the main window, but with a `?console` query string -
 * `renderer/src/main.tsx` checks that to render `ConsoleWindowView` instead of the full `App`.
 * Avoids a second Vite/electron-vite entry point purely for a small log viewer.
 *
 * Reads `themeColors` itself (rather than taking it as a parameter) since this is only ever
 * called on demand, well after startup - a fresh read is cheap and always current, unlike
 * `main/index.ts`'s main window, which resolves it once during its own startup sequence. */
export async function openConsoleWindow(): Promise<void> {
  if (consoleWindow && !consoleWindow.isDestroyed()) {
    consoleWindow.focus()
    return
  }

  const settings = await loadLauncherSettings()
  const backgroundColor = settings.themeColors?.background1 ?? DEFAULT_THEME_COLORS.background1

  consoleWindow = new BrowserWindow({
    width: 640,
    height: 480,
    minWidth: 400,
    minHeight: 300,
    backgroundColor,
    icon: join(__dirname, '../../resources/icon.png'),
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true
    }
  })

  if (process.env['ELECTRON_RENDERER_URL']) {
    void consoleWindow.loadURL(`${process.env['ELECTRON_RENDERER_URL']}?console`)
  } else {
    void consoleWindow.loadFile(join(__dirname, '../renderer/index.html'), { search: 'console' })
  }

  consoleWindow.on('closed', () => {
    consoleWindow = null
  })
}

/** Mirrors a `launch:progress`/`game:log` event into the console window too, alongside whatever
 * `ipc/handlers.ts#LaunchPlay` already sent to the invoking (main) window's own `event.sender` -
 * a no-op whenever the console window isn't currently open. */
export function sendToConsoleWindow(channel: string, payload: unknown): void {
  if (consoleWindow && !consoleWindow.isDestroyed()) {
    consoleWindow.webContents.send(channel, payload)
  }
}
