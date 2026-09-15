import type { IpcMainInvokeEvent } from 'electron'
import { BrowserWindow, dialog, ipcMain, shell } from 'electron'
import { readFile, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { IpcChannel } from '../../shared/ipc'
import {
  isBundleCompatibleVersion,
  MINECRAFT_VERSION,
  type CapeUploadResult,
  type ClientImportResult,
  type CustomCapeStatus,
  type GameLogEvent,
  type LaunchStage,
  type LauncherSettings,
  type MinecraftProfile,
  type ModBundleUpdateInfo,
  type ModrinthSearchPage,
  type ModrinthSortIndex,
  type SkinLibraryEntry,
  type SkinUploadResult,
  type SkinVariant,
  type StorageInfo,
  type StorageMoveProgressEvent
} from '../../shared/types'
import { loadMockProfile, performLogin, tryRestoreSession } from '../auth'
import { fetchTextureDataUri, loadPngFileForEditor, uploadSkin, uploadSkinBuffer } from '../auth/skinApi'
import { updateCachedProfile } from '../auth/tokenCache'
import { installUpdateNow } from '../autoUpdate'
import { deleteCustomCape, getCustomCapeStatus, loadCapePngForPreview, uploadCustomCape } from '../cape/capeStorage'
import { syncBundledContent } from '../launch/bundleSync'
import { buildClasspath } from '../launch/classpath'
import { importFromExternalClient, pickExternalClientFolder } from '../launch/clientImport'
import { installFabricLoader } from '../launch/fabricInstaller'
import { launchGame } from '../launch/gameProcess'
import { installVersion } from '../launch/installer'
import { cloneInstance, deleteInstance } from '../launch/instanceManager'
import { ensureJavaRuntime } from '../launch/javaRuntime'
import { buildLaunchArgs } from '../launch/launchArgs'
import { applyModBundleUpdate, checkForModBundleUpdate } from '../launch/modBundleUpdater'
import { addCustomMods, listCustomMods, listToggleableBundledMods, removeCustomMod } from '../launch/modsManager'
import { installModrinthMod, searchModrinthMods } from '../launch/modrinthApi'
import { applySharedOptions, applySharedServers, saveSharedOptions, saveSharedServers } from '../launch/sharedSettings'
import { changeStorageLocation, getStorageInfo } from '../launch/storageManager'
import { fetchAvailableVersions } from '../launch/versionList'
import { fetchVersionDetail } from '../launch/versionManifest'
import { loadLauncherSettings, saveLauncherSettings } from '../launcherSettings'
import { loadDefaultSkinTemplate } from '../skin/defaultTemplate'
import {
  deleteSkinFromLibrary,
  getSkinLibraryEntry,
  listSkinLibrary,
  readSkinLibraryEntryForUpload,
  renameSkinInLibrary,
  saveSkinToLibrary
} from '../skin/skinLibrary'

function pngBufferToDataUri(buffer: Buffer): string {
  return `data:image/png;base64,${buffer.toString('base64')}`
}

/** Guards the two operations that can both touch the shared `versions/`/`libraries/`/`assets/`
 * tree at the same time: a `LaunchPlay` install and a `StorageChangeLocation` move. Without this,
 * `dataRoot()` could get read mid-flip inside a single `installVersion()` call (some paths resolved
 * against the old root, some against the new), or a move could `rm()` a file `downloadFile()` is
 * still mid-write to. Small, in-process, main-process-only (this app only ever runs one instance of
 * itself) - no need for anything heavier than a module-level flag. */
let storageBusy = false

function assertStorageNotBusy(): void {
  if (storageBusy) {
    throw new Error('Spieldaten werden gerade verschoben oder installiert — bitte kurz warten.')
  }
}

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

  ipcMain.handle(IpcChannel.ModsListCustom, async (_event: IpcMainInvokeEvent, instanceId: string) =>
    listCustomMods(instanceId)
  )

  ipcMain.handle(IpcChannel.ModsAddCustom, async (event: IpcMainInvokeEvent, instanceId: string) =>
    addCustomMods(instanceId, BrowserWindow.fromWebContents(event.sender))
  )

  ipcMain.handle(IpcChannel.ModsRemoveCustom, async (_event: IpcMainInvokeEvent, instanceId: string, fileName: string) =>
    removeCustomMod(instanceId, fileName)
  )

  ipcMain.handle(
    IpcChannel.ModsSearchModrinth,
    async (
      _event: IpcMainInvokeEvent,
      query: string,
      gameVersion: string,
      offset: number,
      sortIndex: ModrinthSortIndex
    ): Promise<ModrinthSearchPage> => searchModrinthMods(query, gameVersion, offset, sortIndex)
  )

  ipcMain.handle(
    IpcChannel.ModsInstallModrinthMod,
    async (_event: IpcMainInvokeEvent, instanceId: string, projectId: string, gameVersion: string) =>
      installModrinthMod(instanceId, projectId, gameVersion)
  )

  ipcMain.handle(IpcChannel.InstancesDelete, async (_event: IpcMainInvokeEvent, instanceId: string) =>
    deleteInstance(instanceId)
  )

  ipcMain.handle(IpcChannel.InstancesClone, async (_event: IpcMainInvokeEvent, instanceId: string, newName: string) =>
    cloneInstance(instanceId, newName)
  )

  ipcMain.handle(IpcChannel.ClientImportPickFolder, async (event: IpcMainInvokeEvent) =>
    pickExternalClientFolder(BrowserWindow.fromWebContents(event.sender))
  )

  ipcMain.handle(
    IpcChannel.ClientImportApply,
    async (_event: IpcMainInvokeEvent, sourceFolder: string, instanceId: string): Promise<ClientImportResult> =>
      importFromExternalClient(sourceFolder, instanceId)
  )

  ipcMain.handle(IpcChannel.UpdateInstallNow, async () => installUpdateNow())

  ipcMain.handle(IpcChannel.StorageInfo, async (): Promise<StorageInfo> => getStorageInfo())

  ipcMain.handle(IpcChannel.StorageChangeLocation, async (event: IpcMainInvokeEvent) => {
    assertStorageNotBusy()
    storageBusy = true
    try {
      const window = BrowserWindow.fromWebContents(event.sender)
      return await changeStorageLocation(window, (subfolder, completed, total, label) => {
        const progress: StorageMoveProgressEvent = { subfolder, completed, total, label }
        event.sender.send(IpcChannel.StorageMoveProgress, progress)
      })
    } finally {
      storageBusy = false
    }
  })

  ipcMain.handle(IpcChannel.SkinFetchTexture, async (_event: IpcMainInvokeEvent, url: string) => fetchTextureDataUri(url))

  // Skin PNGs picked via a native dialog (same "no HTML5 drag&drop under sandbox:true" reasoning
  // as ModsScreen's "add custom mod" flow, see modsManager.ts) - null return means the dialog was
  // cancelled, not an error. The upload endpoint's own response already contains the fresh
  // skins/capes, so the renderer never needs a separate refetch - just merge it into its copy of
  // the profile and hand it back here to keep auth.json's cached profile in sync too.
  ipcMain.handle(
    IpcChannel.SkinUpload,
    async (event: IpcMainInvokeEvent, profile: MinecraftProfile, variant: SkinVariant): Promise<SkinUploadResult | null> => {
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
      // So every skin the account has ever worn - regardless of whether it came from this direct
      // upload or the pixel editor - ends up in the "Meine Skins" library, not just editor saves.
      // Starts with a placeholder name - own user request was to name it *after* picking/uploading
      // the file, not before, so the renderer immediately follows up with a SkinLibraryRename call
      // using the id returned here.
      const libraryEntry = await saveSkinToLibrary(
        await readFile(result.filePaths[0]),
        variant,
        `Hochgeladen am ${new Date().toLocaleDateString('de-DE')}`
      )
      return { profile: updatedProfile, libraryEntryId: libraryEntry.id }
    }
  )

  ipcMain.handle(IpcChannel.SkinLibraryRename, async (_event: IpcMainInvokeEvent, id: string, name: string) =>
    renameSkinInLibrary(id, name)
  )

  ipcMain.handle(IpcChannel.SkinEditorLoadPng, async (event: IpcMainInvokeEvent) => {
    const window = BrowserWindow.fromWebContents(event.sender)
    const result = await loadPngFileForEditor(window)
    return result ? { dataUri: pngBufferToDataUri(result.buffer), width: result.width, height: result.height } : null
  })

  ipcMain.handle(IpcChannel.SkinEditorLoadTemplate, async (_event: IpcMainInvokeEvent, variant: SkinVariant) => {
    const buffer = await loadDefaultSkinTemplate(variant)
    return pngBufferToDataUri(buffer)
  })

  ipcMain.handle(
    IpcChannel.SkinEditorExportPng,
    async (event: IpcMainInvokeEvent, pngBytes: ArrayBuffer, suggestedFileName: string): Promise<{ path: string } | null> => {
      const window = BrowserWindow.fromWebContents(event.sender)
      const dialogOptions: Electron.SaveDialogOptions = {
        title: 'Skin als PNG exportieren',
        defaultPath: suggestedFileName,
        filters: [{ name: 'PNG-Bild', extensions: ['png'] }]
      }
      const result = window ? await dialog.showSaveDialog(window, dialogOptions) : await dialog.showSaveDialog(dialogOptions)
      if (result.canceled || !result.filePath) return null
      await writeFile(result.filePath, Buffer.from(pngBytes))
      return { path: result.filePath }
    }
  )

  ipcMain.handle(IpcChannel.SkinLibraryList, async (): Promise<SkinLibraryEntry[]> => listSkinLibrary())

  ipcMain.handle(
    IpcChannel.SkinLibrarySave,
    async (_event: IpcMainInvokeEvent, pngBytes: ArrayBuffer, variant: SkinVariant, name: string, existingId?: string) =>
      saveSkinToLibrary(Buffer.from(pngBytes), variant, name, existingId)
  )

  ipcMain.handle(IpcChannel.SkinLibraryDelete, async (_event: IpcMainInvokeEvent, id: string) => deleteSkinFromLibrary(id))

  ipcMain.handle(
    IpcChannel.SkinLibraryUse,
    async (_event: IpcMainInvokeEvent, profile: MinecraftProfile, id: string): Promise<MinecraftProfile | null> => {
      const entry = await readSkinLibraryEntryForUpload(id)
      if (!entry) return null
      const { skins, capes } = await uploadSkinBuffer(profile.accessToken, entry.buffer, entry.variant)
      const updatedProfile: MinecraftProfile = { ...profile, skins, capes }
      await updateCachedProfile(updatedProfile)
      return updatedProfile
    }
  )

  ipcMain.handle(IpcChannel.SkinLibraryLoadForEdit, async (_event: IpcMainInvokeEvent, id: string): Promise<SkinLibraryEntry | null> =>
    getSkinLibraryEntry(id)
  )

  ipcMain.handle(IpcChannel.CapeSelectPng, async (event: IpcMainInvokeEvent) => {
    const window = BrowserWindow.fromWebContents(event.sender)
    const result = await loadCapePngForPreview(window)
    return result ? { dataUri: pngBufferToDataUri(result.buffer), width: result.width, height: result.height } : null
  })

  ipcMain.handle(
    IpcChannel.CapeUpload,
    async (_event: IpcMainInvokeEvent, profile: MinecraftProfile, pngDataUri: string): Promise<CapeUploadResult> => {
      const base64 = pngDataUri.split(',')[1] ?? ''
      return uploadCustomCape(profile.id, Buffer.from(base64, 'base64'))
    }
  )

  ipcMain.handle(IpcChannel.CapeDelete, async (_event: IpcMainInvokeEvent, profile: MinecraftProfile): Promise<void> =>
    deleteCustomCape(profile.id)
  )

  ipcMain.handle(IpcChannel.CapeStatus, async (_event: IpcMainInvokeEvent, profile: MinecraftProfile): Promise<CustomCapeStatus> =>
    getCustomCapeStatus(profile.id)
  )

  ipcMain.handle(IpcChannel.ModBundleCheckUpdate, async (): Promise<ModBundleUpdateInfo> => checkForModBundleUpdate())

  ipcMain.handle(IpcChannel.ModBundleApplyUpdate, async (): Promise<LauncherSettings> => applyModBundleUpdate())

  ipcMain.handle(
    IpcChannel.LaunchPlay,
    async (event: IpcMainInvokeEvent, profile: MinecraftProfile, instanceId: string) => {
      assertStorageNotBusy()
      const sendProgress = (stage: LaunchStage, completed: number, total: number, label?: string): void => {
        event.sender.send(IpcChannel.LaunchProgress, { stage, completed, total, label })
      }
      const sendLog = (log: GameLogEvent): void => {
        event.sender.send(IpcChannel.GameLog, log)
      }

      const settings = await loadLauncherSettings()
      const instance = settings.instances.find((candidate) => candidate.id === instanceId)
      if (!instance) {
        throw new Error(`Unbekannte Instanz: ${instanceId}`)
      }
      const versionId = instance.versionId

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

      const vanilla = await installVersion(sendProgress, versionId, instance.id)
      const installed = await installFabricLoader(vanilla, sendProgress)
      await syncBundledContent(installed.instanceDir, sendProgress, bundleCompatible, instance.enabledBundledMods)
      const classpath = buildClasspath(installed.libraryPaths, installed.clientJarPath)
      const args = buildLaunchArgs({
        detail: installed.detail,
        instanceDir: installed.instanceDir,
        assetsDir: installed.assetsDir,
        classpath,
        profile
      })

      const gameDir = join(installed.instanceDir, 'game')
      // Carries options.txt (graphics/controls/sound/...) and the multiplayer server list across
      // instances, same reasoning as before the instance system existed when this carried settings
      // across version switches - these are personal preferences the player wants everywhere, not
      // something meaningfully different per instance. See sharedSettings.ts.
      await applySharedOptions(gameDir)
      await applySharedServers(gameDir)

      sendProgress('launching', 0, 1, installed.detail.id)
      sendLog({
        source: 'launcher',
        level: 'info',
        message: `Starte Minecraft ${installed.detail.id}${profile.isMock ? ' (Dev-Mock-Profil)' : ''}…`
      })

      await launchGame(javaBinaryPath, args, gameDir, sendLog)
      await saveSharedOptions(gameDir)
      await saveSharedServers(gameDir)
      sendProgress('done', 1, 1)
    }
  )
}
