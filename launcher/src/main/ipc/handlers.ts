import type { IpcMainInvokeEvent } from 'electron'
import { BrowserWindow, dialog, ipcMain, shell } from 'electron'
import { writeFile } from 'node:fs/promises'
import { totalmem } from 'node:os'
import { join } from 'node:path'
import { describeError, localizedError } from '../../shared/errorMessages'
import { IpcChannel } from '../../shared/ipc'
import {
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
  type StorageMoveProgressEvent,
  type SystemMemoryInfo
} from '../../shared/types'
import { loadMockProfile, performLogin, tryRestoreSession } from '../auth'
import { fetchTextureDataUri, loadPngFileForEditor, uploadSkinBuffer } from '../auth/skinApi'
import { updateCachedProfile } from '../auth/tokenCache'
import { installUpdateNow } from '../autoUpdate'
import { deleteCustomCape, getCustomCapeStatus, loadCapePngForPreview, uploadCustomCape } from '../cape/capeStorage'
import { openConsoleWindow, sendToConsoleWindow } from '../consoleWindow'
import { getBundleCompatibleVersions, hasLocalBundleContent, isVersionBundleCompatible } from '../launch/bundleCompat'
import { syncBundledContent } from '../launch/bundleSync'
import { buildClasspath } from '../launch/classpath'
import { importFromExternalClient, pickExternalClientFolder } from '../launch/clientImport'
import { installFabricLoader } from '../launch/fabricInstaller'
import { launchGame } from '../launch/gameProcess'
import { installVersion } from '../launch/installer'
import {
  cloneInstance,
  copyWorldBetweenInstances,
  deleteInstance,
  getWorldIcon,
  listInstanceWorlds,
  moveWorldBetweenInstances
} from '../launch/instanceManager'
import { ensureJavaRuntime } from '../launch/javaRuntime'
import { buildLaunchArgs } from '../launch/launchArgs'
import { applyModBundleUpdate, checkForModBundleUpdate } from '../launch/modBundleUpdater'
import { addCustomMods, listCustomMods, listToggleableBundledMods, removeCustomMod, setCustomModEnabled } from '../launch/modsManager'
import { getBundledModProjectIds, getCustomModProjectIds, installModrinthMod, searchModrinthMods } from '../launch/modrinthApi'
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
    throw localizedError('launcher.busy')
  }
}

/** Backs the Cancel button (own user request: "einen Button der den Launch-Prozess abbricht", shown
 * in the launcher or the separate console window depending on the Settings screen's toggle) - same
 * module-level-singleton reasoning as `storageBusy` above, since only one `LaunchPlay` ever runs at
 * a time. `signal` is threaded into every `fetch()`/`spawn()` down the install chain (downloader.ts,
 * downloadVerify.ts, javaRuntime.ts, installer.ts, fabricInstaller.ts, fabricMeta.ts,
 * versionManifest.ts, modBundleUpdater.ts, gameProcess.ts), so `LaunchCancel` aborts whichever of
 * those is in flight - including killing an already-spawned Minecraft process, which Node's `spawn`
 * does on its own once its `signal` aborts. */
let currentLaunchController: AbortController | null = null

function broadcastLaunchBusy(event: IpcMainInvokeEvent, busy: boolean): void {
  event.sender.send(IpcChannel.LaunchBusyChanged, busy)
  sendToConsoleWindow(IpcChannel.LaunchBusyChanged, busy)
}

/** Registered exactly once for the app's lifetime (not per-window) — ipcMain.handle throws if a
 * channel is registered twice, which would happen if this ran again from a second createWindow()
 * call (e.g. macOS "activate" after all windows closed). Replies go back via the invoking
 * event's own `sender`, so this stays correct even if multiple windows exist. */
export function registerIpcHandlers(): void {
  ipcMain.handle(IpcChannel.AuthRestore, async () => tryRestoreSession())

  ipcMain.handle(IpcChannel.AuthLogin, async (event: IpcMainInvokeEvent) => {
    // Own follow-up question: "ist die Nachricht immer auf Deutsch?" - the OAuth callback page
    // (rendered in msOAuth.ts, entirely outside the renderer's own i18n system) needs the current
    // language passed in explicitly. Same "read settings directly, fall back to 'de'" pattern the
    // cancelled-launch log line above already uses, for the same reason: this main-process code has
    // no access to the renderer's own `useTranslations()`.
    const language = await loadLauncherSettings()
      .then((s) => s.language)
      .catch(() => 'de' as const)
    return performLogin((progress) => event.sender.send(IpcChannel.AuthProgress, progress), language)
  })

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

  ipcMain.handle(IpcChannel.ModsListBundled, async (_event: IpcMainInvokeEvent, versionId: string) =>
    listToggleableBundledMods(versionId)
  )

  ipcMain.handle(IpcChannel.ModsListBundledProjectIds, async (_event: IpcMainInvokeEvent, versionId: string) =>
    getBundledModProjectIds(versionId)
  )

  ipcMain.handle(IpcChannel.ModsListCustom, async (_event: IpcMainInvokeEvent, instanceId: string) =>
    listCustomMods(instanceId)
  )

  ipcMain.handle(IpcChannel.ModsListCustomProjectIds, async (_event: IpcMainInvokeEvent, instanceId: string) =>
    getCustomModProjectIds(instanceId)
  )

  ipcMain.handle(IpcChannel.ModsAddCustom, async (event: IpcMainInvokeEvent, instanceId: string) =>
    addCustomMods(instanceId, BrowserWindow.fromWebContents(event.sender))
  )

  ipcMain.handle(IpcChannel.ModsRemoveCustom, async (_event: IpcMainInvokeEvent, instanceId: string, fileName: string) =>
    removeCustomMod(instanceId, fileName)
  )

  ipcMain.handle(
    IpcChannel.ModsSetCustomEnabled,
    async (_event: IpcMainInvokeEvent, instanceId: string, fileName: string, enabled: boolean) =>
      setCustomModEnabled(instanceId, fileName, enabled)
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

  ipcMain.handle(IpcChannel.InstancesListWorlds, async (_event: IpcMainInvokeEvent, instanceId: string) =>
    listInstanceWorlds(instanceId)
  )

  ipcMain.handle(IpcChannel.InstancesWorldIcon, async (_event: IpcMainInvokeEvent, instanceId: string, worldName: string) =>
    getWorldIcon(instanceId, worldName)
  )

  ipcMain.handle(
    IpcChannel.InstancesMoveWorld,
    async (_event: IpcMainInvokeEvent, sourceInstanceId: string, worldName: string, targetInstanceId: string) =>
      moveWorldBetweenInstances(sourceInstanceId, worldName, targetInstanceId)
  )

  ipcMain.handle(
    IpcChannel.InstancesCopyWorld,
    async (_event: IpcMainInvokeEvent, sourceInstanceId: string, worldName: string, targetInstanceId: string) =>
      copyWorldBetweenInstances(sourceInstanceId, worldName, targetInstanceId)
  )

  ipcMain.handle(IpcChannel.ClientImportPickFolder, async (event: IpcMainInvokeEvent) =>
    pickExternalClientFolder(BrowserWindow.fromWebContents(event.sender))
  )

  ipcMain.handle(
    IpcChannel.ClientImportApply,
    async (_event: IpcMainInvokeEvent, sourceFolder: string, instanceId: string, versionId: string): Promise<ClientImportResult> =>
      importFromExternalClient(sourceFolder, instanceId, versionId)
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

  // The renderer already picked the file (via SkinEditorLoadPng, shared with the pixel editor's
  // own "load PNG" step) and let the user set name + variant together on its pending-upload
  // preview screen before ever calling this - own user request, so there's no dialog and no
  // "cancelled" case left here, just the actual upload. The upload endpoint's own response already
  // contains the fresh skins/capes, so the renderer never needs a separate refetch - just merge it
  // into its copy of the profile and hand it back here to keep auth.json's cached profile in sync too.
  ipcMain.handle(
    IpcChannel.SkinUpload,
    async (
      _event: IpcMainInvokeEvent,
      profile: MinecraftProfile,
      pngBytes: ArrayBuffer,
      variant: SkinVariant,
      name: string
    ): Promise<SkinUploadResult> => {
      const buffer = Buffer.from(pngBytes)
      const { skins, capes } = await uploadSkinBuffer(profile.accessToken, buffer, variant)
      const updatedProfile: MinecraftProfile = { ...profile, skins, capes }
      await updateCachedProfile(updatedProfile)
      // So every skin the account has ever worn - regardless of whether it came from this direct
      // upload or the pixel editor - ends up in the "Meine Skins" library, not just editor saves.
      const libraryEntry = await saveSkinToLibrary(buffer, variant, name)
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

  ipcMain.handle(
    IpcChannel.ModBundleCheckUpdate,
    async (_event: IpcMainInvokeEvent, versionId: string): Promise<ModBundleUpdateInfo> => checkForModBundleUpdate(versionId)
  )

  ipcMain.handle(
    IpcChannel.ModBundleApplyUpdate,
    async (_event: IpcMainInvokeEvent, versionId: string): Promise<LauncherSettings> => applyModBundleUpdate(versionId)
  )

  ipcMain.handle(IpcChannel.ModBundleListCompatibleVersions, async (): Promise<string[]> => [
    ...(await getBundleCompatibleVersions())
  ])

  ipcMain.handle(IpcChannel.SystemMemoryInfo, (): SystemMemoryInfo => ({
    totalMb: Math.round(totalmem() / (1024 * 1024))
  }))

  ipcMain.handle(IpcChannel.ConsoleWindowOpen, () => openConsoleWindow())

  ipcMain.handle(
    IpcChannel.LaunchPlay,
    async (event: IpcMainInvokeEvent, profile: MinecraftProfile, instanceId: string) => {
      assertStorageNotBusy()
      const controller = new AbortController()
      currentLaunchController = controller
      const { signal } = controller
      broadcastLaunchBusy(event, true)

      const sendProgress = (stage: LaunchStage, completed: number, total: number, label?: string): void => {
        const payload = { stage, completed, total, label }
        event.sender.send(IpcChannel.LaunchProgress, payload)
        sendToConsoleWindow(IpcChannel.LaunchProgress, payload)
      }
      const sendLog = (log: GameLogEvent): void => {
        event.sender.send(IpcChannel.GameLog, log)
        sendToConsoleWindow(IpcChannel.GameLog, log)
      }

      try {
        const settings = await loadLauncherSettings()
        const instance = settings.instances.find((candidate) => candidate.id === instanceId)
        if (!instance) {
          throw localizedError('instance.unknown', { instanceId })
        }
        const versionId = instance.versionId

        // Cheap detail-only fetch (no downloads) purely to read `javaVersion` before committing to
        // the full (potentially large) installVersion download below - installVersion re-fetches
        // the same detail itself, a small duplicate JSON request is an easy trade for not
        // downloading gigabytes of assets for a runtime that then fails to provision.
        const targetDetail = await fetchVersionDetail(versionId, signal)
        // Versions old enough to predate Mojang's own javaVersion field (pre-1.17-ish) ran on
        // whatever JRE 8 provided - jre-legacy is Mojang's own component name for exactly that,
        // and still shows up in the runtime manifest today.
        const javaComponent = targetDetail.javaVersion?.component ?? 'jre-legacy'
        const javaBinaryPath = await ensureJavaRuntime(javaComponent, sendProgress, signal)
        sendLog({
          source: 'launcher',
          level: 'info',
          message: `Java-Runtime bereit (${javaComponent}).`
        })

        const bundleCompatible = await isVersionBundleCompatible(versionId)
        if (!bundleCompatible) {
          sendLog({
            source: 'launcher',
            level: 'info',
            message: `${versionId} ist aktuell nicht Mod-Bundle-kompatibel - gebündelte Mods/Resourcepacks (Sodium, Lithium, eigener Client-Mod, ...) werden übersprungen, es startet reines Fabric+Vanilla.`
          })
        } else if (!(await hasLocalBundleContent(versionId))) {
          // First time this version's bundle is actually needed - it was only ever added via the
          // manifest, never baked into this installer. Same download this version's "Aktualisieren"
          // banner would trigger later, just run automatically once up front instead of leaving a
          // freshly-added version's very first launch with nothing to show for it.
          sendLog({ source: 'launcher', level: 'info', message: `Lade Mod-Bundle für ${versionId} herunter…` })
          try {
            await applyModBundleUpdate(versionId, signal)
          } catch (err) {
            // A deliberate cancel must stop the whole launch, not just this one sub-step - rethrow
            // so the outer catch below turns it into `launch.cancelled` instead of this catch
            // quietly swallowing it and the launch continuing on as if nothing happened.
            if (signal.aborted) throw err
            sendLog({
              source: 'launcher',
              level: 'error',
              message: `Mod-Bundle-Download für ${versionId} fehlgeschlagen (${describeError(err)}) - startet ohne gebündelte Mods.`
            })
          }
        }

        const vanilla = await installVersion(sendProgress, versionId, instance.id, signal)
        const installed = await installFabricLoader(vanilla, sendProgress, signal)
        await syncBundledContent(installed.instanceDir, sendProgress, bundleCompatible, versionId, instance.enabledBundledMods)
        const classpath = buildClasspath(installed.libraryPaths, installed.clientJarPath)
        const args = buildLaunchArgs({
          detail: installed.detail,
          instanceDir: installed.instanceDir,
          assetsDir: installed.assetsDir,
          classpath,
          profile,
          maxMemoryMb: settings.maxMemoryMb
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

        await launchGame(javaBinaryPath, args, gameDir, sendLog, signal)
        await saveSharedOptions(gameDir)
        await saveSharedServers(gameDir)
        sendProgress('done', 1, 1)
      } catch (err) {
        if (signal.aborted) {
          // Language picked fresh here (not the `settings` local above, out of scope in a catch,
          // and possibly stale anyway) rather than routed through the renderer's `formatError`/`t`
          // machinery - this one line needs to reach both windows uniformly via the same `sendLog`
          // broadcast every other progress line already uses, not just whichever window happens to
          // be awaiting the `LaunchPlay` promise itself (only the console window otherwise has no
          // way to learn a launch it's displaying was cancelled at all).
          const language = await loadLauncherSettings()
            .then((s) => s.language)
            .catch(() => 'de')
          sendLog({ source: 'launcher', level: 'info', message: language === 'en' ? 'Launch cancelled.' : 'Start abgebrochen.' })
          throw localizedError('launch.cancelled')
        }
        throw err
      } finally {
        currentLaunchController = null
        broadcastLaunchBusy(event, false)
      }
    }
  )

  ipcMain.handle(IpcChannel.LaunchCancel, () => {
    currentLaunchController?.abort()
  })

  // Own follow-up to the `LaunchBusyChanged` broadcast above: `openConsoleWindow()` in
  // `PlayScreen#handlePlay` is fire-and-forget, so the console window's own renderer can still be
  // mid-load (React not mounted, no `onLaunchBusyChanged` listener attached yet) at the exact moment
  // `LaunchPlay` sends that first `true` - a message sent to a not-yet-listening `webContents` is
  // simply lost, not queued. This lets that window ask once on mount instead of only ever reacting
  // to a broadcast it may have missed.
  ipcMain.handle(IpcChannel.LaunchIsBusy, () => currentLaunchController !== null)
}
