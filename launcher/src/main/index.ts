import { app, BrowserWindow, shell } from 'electron'
import { join } from 'node:path'
import { DEFAULT_THEME_COLORS } from '../shared/types'
import { registerAutoUpdater } from './autoUpdate'
import { initDataRoot } from './dataRoot'
import { registerIpcHandlers } from './ipc/handlers'
import { consolidateInstanceStorage } from './launch/storageConsolidation'

/** `backgroundColor` is what Electron paints immediately on window creation, before the renderer
 * has loaded anything - has to match whatever theme is actually configured (own wishlist item's
 * Settings screen, `themeColors.background1`) or its default, otherwise a still-configured/still-
 * default color briefly flashes on startup before the real page paints over it in the *new*
 * color. */
/** `resources/icon.png` (own branding, generated from `designs/branding/N-Logo.svg`) - without
 * this, Windows shows Electron's own generic icon in the taskbar/title bar in dev mode (own
 * report). `resources/icon.ico` (see `electron-builder.yml`'s `win.icon`) covers the packaged
 * .exe itself, which is a separate thing from a `BrowserWindow`'s own runtime icon. */
const APP_ICON_PATH = join(__dirname, '../../resources/icon.png')

function createWindow(backgroundColor: string): BrowserWindow {
  const window = new BrowserWindow({
    width: 1000,
    height: 680,
    minWidth: 800,
    minHeight: 560,
    autoHideMenuBar: true,
    backgroundColor,
    icon: APP_ICON_PATH,
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true
    }
  })

  window.webContents.setWindowOpenHandler(({ url }) => {
    void shell.openExternal(url)
    return { action: 'deny' }
  })

  if (process.env['ELECTRON_RENDERER_URL']) {
    void window.loadURL(process.env['ELECTRON_RENDERER_URL'])
  } else {
    void window.loadFile(join(__dirname, '../renderer/index.html'))
  }

  return window
}

void app.whenReady().then(async () => {
  // Must resolve dataRoot() (and, in the same pass, consolidate any pre-shared-storage per-instance
  // duplicates into it - see storageConsolidation.ts) before anything else touches the filesystem:
  // registerIpcHandlers()'s LaunchPlay/StorageChangeLocation handlers and createWindow() itself
  // (autoUpdate downloads into userData) can all end up calling dataRoot(), which throws if it
  // isn't primed yet.
  const settings = await initDataRoot()
  await consolidateInstanceStorage(settings.instances)
  const backgroundColor = settings.themeColors?.background1 ?? DEFAULT_THEME_COLORS.background1

  registerIpcHandlers()
  const window = createWindow(backgroundColor)
  registerAutoUpdater(window)

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow(backgroundColor)
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})
