import { app, Menu, nativeImage, Tray, type BrowserWindow } from 'electron'
import { join } from 'node:path'
import { isGameRunning, onGameStopped } from './launch/gameActivity'
import { loadLauncherSettings } from './launcherSettings'

/**
 * Closing the launcher while Minecraft runs used to take the game down with it, hard, without saving
 * (tested live): the game is a regular child process, and Node puts those into a Windows job object
 * that kills them when the launcher exits. Own decision: the launcher keeps running in the background
 * instead - the window only hides, a tray icon brings it back, and once the game has ended the
 * launcher quits on its own (after a crash it shows itself again instead). That also keeps everything
 * that goes through the launcher working in the game (friends, world invitations, nametag logos).
 *
 * Quitting for real (tray menu, or the OS shutting down) still ends the game - the menu entry says so.
 */

const TRAY_ICON_PATH = join(__dirname, '../../resources', process.platform === 'win32' ? 'icon.ico' : 'icon.png')

let quitting = false
let tray: Tray | null = null
/** The window was hidden because of a running game - the launcher quits once that game ends. */
let hiddenForGame = false

async function trayTexts(): Promise<{ tooltip: string; open: string; quit: string }> {
  const language = await loadLauncherSettings()
    .then((settings) => settings.language)
    .catch(() => 'de')
  return language === 'en'
    ? { tooltip: "TNT's All-In-1 Client - Minecraft is running", open: 'Open launcher', quit: 'Quit launcher and Minecraft' }
    : { tooltip: "TNT's All-In-1 Client - Minecraft läuft", open: 'Launcher öffnen', quit: 'Launcher und Minecraft beenden' }
}

function showWindow(window: BrowserWindow): void {
  hiddenForGame = false
  if (window.isMinimized()) window.restore()
  window.show()
  window.focus()
  tray?.destroy()
  tray = null
}

async function showTray(window: BrowserWindow): Promise<void> {
  if (tray) return
  const texts = await trayTexts()
  tray = new Tray(nativeImage.createFromPath(TRAY_ICON_PATH))
  tray.setToolTip(texts.tooltip)
  tray.setContextMenu(
    Menu.buildFromTemplate([
      { label: texts.open, click: () => showWindow(window) },
      { type: 'separator' },
      { label: texts.quit, click: () => app.quit() }
    ])
  )
  tray.on('click', () => showWindow(window))
}

/** Hooks the main window up - call once, right after creating it. */
export function registerBackgroundMode(window: BrowserWindow): void {
  window.on('close', (event) => {
    if (quitting || !isGameRunning()) return
    event.preventDefault()
    hiddenForGame = true
    window.hide()
    void showTray(window)
  })

  // Launcher started again while it runs hidden: bring the existing window back instead of a second
  // launcher (see the single instance lock in index.ts).
  app.on('second-instance', () => {
    if (!window.isDestroyed()) showWindow(window)
  })

  onGameStopped((endedCleanly) => {
    if (hiddenForGame && endedCleanly) {
      app.quit()
    } else if (hiddenForGame) {
      // Crashed: show the launcher, so the player sees what happened instead of it silently vanishing.
      showWindow(window)
    } else {
      tray?.destroy()
      tray = null
    }
  })
}

app.on('before-quit', () => {
  quitting = true
})
