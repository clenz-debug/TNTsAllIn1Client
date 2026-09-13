import type { BrowserWindow } from 'electron'
import { dialog } from 'electron'
import { statfs } from 'node:fs/promises'
import { join, sep } from 'node:path'
import type { StorageInfo } from '../../shared/types'
import { dataRoot, RELOCATABLE_SUBDIRS, setDataRoot } from '../dataRoot'
import { loadLauncherSettings, saveLauncherSettings } from '../launcherSettings'
import { moveTree } from '../storageMove'

export async function getStorageInfo(): Promise<StorageInfo> {
  const path = dataRoot()
  try {
    const stats = await statfs(path)
    return { path, freeBytes: stats.bavail * stats.bsize }
  } catch {
    // statfs unsupported on this platform, or the folder doesn't exist yet (very first run,
    // nothing installed) - the renderer just hides the free-space line in that case.
    return { path, freeBytes: null }
  }
}

export type StorageMoveProgressCallback = (subfolder: string, completed: number, total: number, label?: string) => void

/**
 * Opens a native "choose folder" dialog (same pattern as the existing mod-jar/skin-PNG pickers in
 * `modsManager.ts`/`ipc/handlers.ts`) and, if the user picked a new folder, physically moves the
 * whole movable game-data tree (`dataRoot.ts#RELOCATABLE_SUBDIRS` - never "everything in the source
 * folder", since on the very first relocation the source root is `app.getPath('userData')` itself,
 * which also directly holds `launcher-settings.json`/`auth.json`/`shared-settings/` as siblings)
 * there. `dataRootOverride` is only persisted - and `dataRoot()`'s cache only updated - *after* the
 * physical move has fully succeeded: a crash mid-move leaves settings still pointing at the old
 * root, so a retry with the same target folder just resumes cheaply (`storageMove.ts#moveOrSkip`
 * skips whatever already made it across) instead of settings pointing at a half-populated root.
 */
export async function changeStorageLocation(
  parentWindow: BrowserWindow | null,
  onProgress: StorageMoveProgressCallback
): Promise<{ path: string } | null> {
  const dialogOptions: Electron.OpenDialogOptions = {
    title: 'Speicherort für Spieldaten wählen',
    properties: ['openDirectory', 'createDirectory']
  }
  const result = parentWindow
    ? await dialog.showOpenDialog(parentWindow, dialogOptions)
    : await dialog.showOpenDialog(dialogOptions)
  if (result.canceled || result.filePaths.length === 0) return null

  const newRoot = result.filePaths[0]
  const oldRoot = dataRoot()
  if (newRoot === oldRoot) return { path: oldRoot }
  if (newRoot.startsWith(oldRoot + sep)) {
    throw new Error('Der neue Speicherort darf nicht innerhalb des aktuellen Speicherorts liegen.')
  }

  for (const subfolder of RELOCATABLE_SUBDIRS) {
    await moveTree(join(oldRoot, subfolder), join(newRoot, subfolder), 8, (completed, total, label) =>
      onProgress(subfolder, completed, total, label)
    )
  }

  const settings = await loadLauncherSettings()
  await saveLauncherSettings({ ...settings, dataRootOverride: newRoot })
  setDataRoot(newRoot)
  return { path: newRoot }
}
