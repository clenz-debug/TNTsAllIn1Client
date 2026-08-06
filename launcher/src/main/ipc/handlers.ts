import type { IpcMainInvokeEvent } from 'electron'
import { BrowserWindow, dialog, ipcMain, shell } from 'electron'
import { join } from 'node:path'
import { IpcChannel } from '../../shared/ipc'
import {
  isBundleCompatibleVersion,
  MINECRAFT_VERSION,
  type GameLogEvent,
  type LaunchStage,
  type LauncherSettings,
  type MinecraftProfile
} from '../../shared/types'
import { loadMockProfile, performLogin, tryRestoreSession } from '../auth'
import { fetchTextureDataUri, uploadSkin, type SkinVariant } from '../auth/skinApi'
import { updateCachedProfile } from '../auth/tokenCache'
import { syncBundledContent } from '../launch/bundleSync'
import { buildClasspath } from '../launch/classpath'
import { installFabricLoader } from '../launch/fabricInstaller'
import { launchGame } from '../launch/gameProcess'
import { installVersion } from '../launch/installer'
import { ensureJavaRuntime } from '../launch/javaRuntime'
import { buildLaunchArgs } from '../launch/launchArgs'
import { addCustomMods, listCustomMods, listToggleableBundledMods, removeCustomMod } from '../launch/modsManager'
import { applySharedOptions, saveSharedOptions } from '../launch/sharedSettings'
import { fetchAvailableVersions } from '../launch/versionList'
import { fetchVersionDetail } from '../launch/versionManifest'
import { loadLauncherSettings, saveLauncherSettings } from '../launcherSettings'
import { checkForUpdate } from '../updateCheck'

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

  ipcMain.handle(IpcChannel.SettingsLoad, async () => loadLauncherSettings())

  ipcMain.handle(IpcChannel.SettingsSave, async (_event: IpcMainInvokeEvent, settings: LauncherSettings) =>
    saveLauncherSettings(settings)
  )

  ipcMain.handle(IpcChannel.ModsListBundled, async () => listToggleableBundledMods())

  ipcMain.handle(IpcChannel.ModsListCustom, async (_event: IpcMainInvokeEvent, versionId: string) =>
    listCustomMods(versionId)
  )

  ipcMain.handle(IpcChannel.ModsAddCustom, async (event: IpcMainInvokeEvent, versionId: string) =>
    addCustomMods(versionId, BrowserWindow.fromWebContents(event.sender))
  )

  ipcMain.handle(IpcChannel.ModsRemoveCustom, async (_event: IpcMainInvokeEvent, versionId: string, fileName: string) =>
    removeCustomMod(versionId, fileName)
  )

  ipcMain.handle(IpcChannel.UpdateCheck, async () => checkForUpdate())

  ipcMain.handle(IpcChannel.SkinFetchTexture, async (_event: IpcMainInvokeEvent, url: string) => fetchTextureDataUri(url))

  // Skin PNGs picked via a native dialog (same "no HTML5 drag&drop under sandbox:true" reasoning
  // as ModsScreen's "add custom mod" flow, see modsManager.ts) - null return means the dialog was
  // cancelled, not an error. The upload endpoint's own response already contains the fresh
  // skins/capes, so the renderer never needs a separate refetch - just merge it into its copy of
  // the profile and hand it back here to keep auth.json's cached profile in sync too.
  ipcMain.handle(
    IpcChannel.SkinUpload,
    async (event: IpcMainInvokeEvent, profile: MinecraftProfile, variant: SkinVariant): Promise<MinecraftProfile | null> => {
      const dialogOptions: Electron.OpenDialogOptions = {
        title: 'Skin-PNG auswählen',
        properties: ['openFile'],
        filters: [{ name: 'PNG-Bild', extensions: ['png'] }]
      }
      const window = BrowserWindow.fromWebContents(event.sender)
      const result = window ? await dialog.showOpenDialog(window, dialogOptions) : await dialog.showOpenDialog(dialogOptions)
      if (result.canceled || result.filePaths.length === 0) return null

      const { skins, capes } = await uploadSkin(profile.accessToken, result.filePaths[0], variant)
      const updatedProfile: MinecraftProfile = { ...profile, skins, capes }
      await updateCachedProfile(updatedProfile)
      return updatedProfile
    }
  )

  ipcMain.handle(
    IpcChannel.LaunchPlay,
    async (event: IpcMainInvokeEvent, profile: MinecraftProfile, versionId: string = MINECRAFT_VERSION) => {
      const sendProgress = (stage: LaunchStage, completed: number, total: number, label?: string): void => {
        event.sender.send(IpcChannel.LaunchProgress, { stage, completed, total, label })
      }
      const sendLog = (log: GameLogEvent): void => {
        event.sender.send(IpcChannel.GameLog, log)
      }

      // Cheap detail-only fetch (no downloads) purely to read `javaVersion` before committing to
      // the full (potentially large) installVersion download below - installVersion re-fetches
      // the same detail itself, a small duplicate JSON request is an easy trade for not
      // downloading gigabytes of assets for a runtime that then fails to provision.
      const targetDetail = await fetchVersionDetail(versionId)
      // Versions old enough to predate Mojang's own javaVersion field (pre-1.17-ish) ran on
      // whatever JRE 8 provided - jre-legacy is Mojang's own component name for exactly that,
      // and still shows up in the runtime manifest today.
      const javaComponent = targetDetail.javaVersion?.component ?? 'jre-legacy'
      const javaBinaryPath = await ensureJavaRuntime(javaComponent, sendProgress)
      sendLog({
        source: 'launcher',
        level: 'info',
        message: `Java-Runtime bereit (${javaComponent}).`
      })

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
      const { enabledBundledMods } = await loadLauncherSettings()
      await syncBundledContent(installed.instanceDir, sendProgress, bundleCompatible, enabledBundledMods)
      const classpath = buildClasspath(installed.libraryPaths, installed.clientJarPath)
      const args = buildLaunchArgs({
        detail: installed.detail,
        instanceDir: installed.instanceDir,
        classpath,
        profile
      })

      const gameDir = join(installed.instanceDir, 'game')
      // Carries options.txt (graphics/controls/sound/...) across version switches - instanceDir
      // is per-version since Phase 6a, but these are personal preferences the player wants
      // everywhere, not something meaningfully different per version. See sharedSettings.ts.
      await applySharedOptions(gameDir)

      sendProgress('launching', 0, 1, installed.detail.id)
      sendLog({
        source: 'launcher',
        level: 'info',
        message: `Starte Minecraft ${installed.detail.id}${profile.isMock ? ' (Dev-Mock-Profil)' : ''}…`
      })

      await launchGame(javaBinaryPath, args, gameDir, sendLog)
      await saveSharedOptions(gameDir)
      sendProgress('done', 1, 1)
    }
  )
}
