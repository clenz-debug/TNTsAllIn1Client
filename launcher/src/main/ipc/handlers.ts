import type { IpcMainInvokeEvent } from 'electron'
import { ipcMain, shell } from 'electron'
import { join } from 'node:path'
import { IpcChannel } from '../../shared/ipc'
import {
  isBundleCompatibleVersion,
  MINECRAFT_VERSION,
  type GameLogEvent,
  type LaunchStage,
  type MinecraftProfile
} from '../../shared/types'
import { loadMockProfile, performLogin, tryRestoreSession } from '../auth'
import { syncBundledContent } from '../launch/bundleSync'
import { buildClasspath } from '../launch/classpath'
import { installFabricLoader } from '../launch/fabricInstaller'
import { checkJavaAvailable, launchGame } from '../launch/gameProcess'
import { installVersion } from '../launch/installer'
import { buildLaunchArgs } from '../launch/launchArgs'
import { fetchAvailableVersions } from '../launch/versionList'

/** Registered exactly once for the app's lifetime (not per-window) — ipcMain.handle throws if a
 * channel is registered twice, which would happen if this ran again from a second createWindow()
 * call (e.g. macOS "activate" after all windows closed). Replies go back via the invoking
 * event's own `sender`, so this stays correct even if multiple windows exist. */
export function registerIpcHandlers(): void {
  ipcMain.handle(IpcChannel.AuthRestore, async () => tryRestoreSession())

  ipcMain.handle(IpcChannel.AuthLogin, async (event: IpcMainInvokeEvent) =>
    performLogin((progress) => event.sender.send(IpcChannel.AuthProgress, progress))
  )

  ipcMain.handle(IpcChannel.AuthLoginMock, async () => loadMockProfile())

  // Renderer never gets direct filesystem/shell access (contextIsolation) - opening a link in the
  // system browser has to be proxied through the main process, same reasoning as the OAuth login
  // flow's own shell.openExternal call in msOAuth.ts.
  ipcMain.handle(IpcChannel.ShellOpenExternal, async (_event: IpcMainInvokeEvent, url: string) => {
    await shell.openExternal(url)
  })

  ipcMain.handle(IpcChannel.VersionsList, async () => fetchAvailableVersions())

  ipcMain.handle(
    IpcChannel.LaunchPlay,
    async (event: IpcMainInvokeEvent, profile: MinecraftProfile, versionId: string = MINECRAFT_VERSION) => {
      const sendProgress = (stage: LaunchStage, completed: number, total: number, label?: string): void => {
        event.sender.send(IpcChannel.LaunchProgress, { stage, completed, total, label })
      }
      const sendLog = (log: GameLogEvent): void => {
        event.sender.send(IpcChannel.GameLog, log)
      }

      const javaAvailable = await checkJavaAvailable()
      if (!javaAvailable) {
        const message = 'java wurde nicht gefunden. Ist JDK 21 installiert und im PATH?'
        sendLog({ source: 'launcher', level: 'error', message })
        throw new Error(message)
      }

      const bundleCompatible = isBundleCompatibleVersion(versionId)
      if (!bundleCompatible) {
        sendLog({
          source: 'launcher',
          level: 'info',
          message: `${versionId} weicht von ${MINECRAFT_VERSION} ab - gebündelte Mods/Resourcepacks (Sodium, Lithium, eigener Client-Mod, ...) werden übersprungen, es startet reines Fabric+Vanilla.`
        })
      }

      const vanilla = await installVersion(sendProgress, versionId)
      const installed = await installFabricLoader(vanilla, sendProgress)
      await syncBundledContent(installed.instanceDir, sendProgress, bundleCompatible)
      const classpath = buildClasspath(installed.libraryPaths, installed.clientJarPath)
      const args = buildLaunchArgs({
        detail: installed.detail,
        instanceDir: installed.instanceDir,
        classpath,
        profile
      })

      sendProgress('launching', 0, 1, installed.detail.id)
      sendLog({
        source: 'launcher',
        level: 'info',
        message: `Starte Minecraft ${installed.detail.id}${profile.isMock ? ' (Dev-Mock-Profil)' : ''}…`
      })

      await launchGame(args, join(installed.instanceDir, 'game'), sendLog)
      sendProgress('done', 1, 1)
    }
  )
}
