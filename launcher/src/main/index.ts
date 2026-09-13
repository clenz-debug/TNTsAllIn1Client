import { app, BrowserWindow, shell } from 'electron'
import { join } from 'node:path'
import { registerAutoUpdater } from './autoUpdate'
import { initDataRoot } from './dataRoot'
import { registerIpcHandlers } from './ipc/handlers'
import { consolidateInstanceStorage } from './launch/storageConsolidation'

function createWindow(): BrowserWindow {
  const window = new BrowserWindow({
    width: 1000,
    height: 680,
    minWidth: 800,
    minHeight: 560,
    autoHideMenuBar: true,
    backgroundColor: '#0d1f0d',
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

  registerIpcHandlers()
  const window = createWindow()
  registerAutoUpdater(window)

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})
