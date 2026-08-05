import { app } from 'electron'
import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { DEFAULT_LAUNCHER_SETTINGS, type LauncherSettings } from '../shared/types'

/** Same hand-rolled JSON-in-userData pattern as `auth/tokenCache.ts` rather than pulling in
 * electron-store for two small fields (selected version, snapshot-visibility toggle). */
function settingsPath(): string {
  return join(app.getPath('userData'), 'launcher-settings.json')
}

export async function loadLauncherSettings(): Promise<LauncherSettings> {
  try {
    const raw = await readFile(settingsPath(), 'utf-8')
    return { ...DEFAULT_LAUNCHER_SETTINGS, ...(JSON.parse(raw) as Partial<LauncherSettings>) }
  } catch {
    return DEFAULT_LAUNCHER_SETTINGS
  }
}

export async function saveLauncherSettings(settings: LauncherSettings): Promise<void> {
  await mkdir(app.getPath('userData'), { recursive: true })
  await writeFile(settingsPath(), JSON.stringify(settings, null, 2), 'utf-8')
}
