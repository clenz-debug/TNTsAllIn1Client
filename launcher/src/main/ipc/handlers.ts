import type { IpcMainInvokeEvent } from 'electron'
import { ipcMain } from 'electron'
import { join } from 'node:path'
import { IpcChannel } from '../../shared/ipc'
import type { GameLogEvent, LaunchStage, MinecraftProfile } from '../../shared/types'
import { loadMockProfile, performLogin, tryRestoreSession } from '../auth'
import { buildClasspath } from '../launch/classpath'
import { installFabricLoader } from '../launch/fabricInstaller'
import { checkJavaAvailable, launchGame } from '../launch/gameProcess'
import { installVersion } from '../launch/installer'
import { buildLaunchArgs } from '../launch/launchArgs'

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

  ipcMain.handle(IpcChannel.LaunchPlay, async (event: IpcMainInvokeEvent, profile: MinecraftProfile) => {
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

    const vanilla = await installVersion(sendProgress)
    const installed = await installFabricLoader(vanilla, sendProgress)
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
  })
}
