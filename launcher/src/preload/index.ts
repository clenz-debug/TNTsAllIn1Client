import { contextBridge, ipcRenderer } from 'electron'
import { IpcChannel } from '../shared/ipc'
import type {
  AuthProgressEvent,
  CapeLibraryEntry,
  CapeUploadResult,
  ClientImportResult,
  CustomCapeStatus,
  CustomModEntry,
  ResourcepackEntry,
  GameLogEvent,
  GameVersionSummary,
  LaunchProgressEvent,
  LauncherSettings,
  MinecraftProfile,
  ModBundleUpdateInfo,
  ModrinthSearchPage,
  ModrinthSortIndex,
  SkinLibraryEntry,
  SkinUploadResult,
  SkinVariant,
  StorageInfo,
  StorageMoveProgressEvent,
  SystemMemoryInfo,
  UpdateStatus
} from '../shared/types'

function subscribe<T>(channel: string, callback: (event: T) => void): () => void {
  const listener = (_: Electron.IpcRendererEvent, payload: T): void => callback(payload)
  ipcRenderer.on(channel, listener)
  return () => ipcRenderer.removeListener(channel, listener)
}

const api = {
  restoreSession: (): Promise<MinecraftProfile | null> => ipcRenderer.invoke(IpcChannel.AuthRestore),
  login: (): Promise<MinecraftProfile> => ipcRenderer.invoke(IpcChannel.AuthLogin),
  play: (profile: MinecraftProfile, instanceId: string): Promise<void> =>
    ipcRenderer.invoke(IpcChannel.LaunchPlay, profile, instanceId),
  cancelLaunch: (): Promise<void> => ipcRenderer.invoke(IpcChannel.LaunchCancel),
  isLaunchBusy: (): Promise<boolean> => ipcRenderer.invoke(IpcChannel.LaunchIsBusy),
  openExternal: (url: string): Promise<void> => ipcRenderer.invoke(IpcChannel.ShellOpenExternal, url),
  listVersions: (): Promise<GameVersionSummary[]> => ipcRenderer.invoke(IpcChannel.VersionsList),
  loadSettings: (): Promise<LauncherSettings> => ipcRenderer.invoke(IpcChannel.SettingsLoad),
  saveSettings: (settings: LauncherSettings): Promise<void> => ipcRenderer.invoke(IpcChannel.SettingsSave, settings),
  listBundledMods: (versionId: string): Promise<string[]> => ipcRenderer.invoke(IpcChannel.ModsListBundled, versionId),
  listBundledModProjectIds: (versionId: string): Promise<string[]> =>
    ipcRenderer.invoke(IpcChannel.ModsListBundledProjectIds, versionId),
  listCustomMods: (instanceId: string): Promise<CustomModEntry[]> => ipcRenderer.invoke(IpcChannel.ModsListCustom, instanceId),
  listCustomModProjectIds: (instanceId: string): Promise<string[]> =>
    ipcRenderer.invoke(IpcChannel.ModsListCustomProjectIds, instanceId),
  addCustomMods: (instanceId: string): Promise<CustomModEntry[]> => ipcRenderer.invoke(IpcChannel.ModsAddCustom, instanceId),
  removeCustomMod: (instanceId: string, fileName: string): Promise<CustomModEntry[]> =>
    ipcRenderer.invoke(IpcChannel.ModsRemoveCustom, instanceId, fileName),
  setCustomModEnabled: (instanceId: string, fileName: string, enabled: boolean): Promise<CustomModEntry[]> =>
    ipcRenderer.invoke(IpcChannel.ModsSetCustomEnabled, instanceId, fileName, enabled),
  searchModrinthMods: (query: string, gameVersion: string, offset: number, sortIndex: ModrinthSortIndex): Promise<ModrinthSearchPage> =>
    ipcRenderer.invoke(IpcChannel.ModsSearchModrinth, query, gameVersion, offset, sortIndex),
  installModrinthMod: (instanceId: string, projectId: string, gameVersion: string): Promise<CustomModEntry[]> =>
    ipcRenderer.invoke(IpcChannel.ModsInstallModrinthMod, instanceId, projectId, gameVersion),
  deleteInstance: (instanceId: string): Promise<LauncherSettings> => ipcRenderer.invoke(IpcChannel.InstancesDelete, instanceId),
  cloneInstance: (instanceId: string, newName: string): Promise<LauncherSettings> =>
    ipcRenderer.invoke(IpcChannel.InstancesClone, instanceId, newName),
  listInstanceWorlds: (instanceId: string): Promise<string[]> =>
    ipcRenderer.invoke(IpcChannel.InstancesListWorlds, instanceId),
  getWorldIcon: (instanceId: string, worldName: string): Promise<string | null> =>
    ipcRenderer.invoke(IpcChannel.InstancesWorldIcon, instanceId, worldName),
  moveWorldBetweenInstances: (sourceInstanceId: string, worldName: string, targetInstanceId: string): Promise<{ movedTo: string }> =>
    ipcRenderer.invoke(IpcChannel.InstancesMoveWorld, sourceInstanceId, worldName, targetInstanceId),
  copyWorldBetweenInstances: (sourceInstanceId: string, worldName: string, targetInstanceId: string): Promise<{ copiedTo: string }> =>
    ipcRenderer.invoke(IpcChannel.InstancesCopyWorld, sourceInstanceId, worldName, targetInstanceId),
  importWorlds: (instanceId: string, kind: 'folder' | 'zip', dialogTitle: string): Promise<string[]> =>
    ipcRenderer.invoke(IpcChannel.InstancesImportWorlds, instanceId, kind, dialogTitle),
  deleteWorld: (instanceId: string, worldName: string): Promise<string[]> =>
    ipcRenderer.invoke(IpcChannel.InstancesDeleteWorld, instanceId, worldName),
  listResourcepacks: (instanceId: string): Promise<ResourcepackEntry[]> => ipcRenderer.invoke(IpcChannel.ResourcepacksList, instanceId),
  addResourcepacks: (instanceId: string, dialogTitle: string): Promise<ResourcepackEntry[]> =>
    ipcRenderer.invoke(IpcChannel.ResourcepacksAdd, instanceId, dialogTitle),
  removeResourcepack: (instanceId: string, name: string): Promise<ResourcepackEntry[]> =>
    ipcRenderer.invoke(IpcChannel.ResourcepacksRemove, instanceId, name),
  removeAllResourcepacks: (instanceId: string): Promise<ResourcepackEntry[]> =>
    ipcRenderer.invoke(IpcChannel.ResourcepacksRemoveAll, instanceId),
  pickExternalClientFolder: (): Promise<string | null> => ipcRenderer.invoke(IpcChannel.ClientImportPickFolder),
  importFromExternalClient: (sourceFolder: string, instanceId: string, versionId: string): Promise<ClientImportResult> =>
    ipcRenderer.invoke(IpcChannel.ClientImportApply, sourceFolder, instanceId, versionId),
  installUpdateNow: (): Promise<void> => ipcRenderer.invoke(IpcChannel.UpdateInstallNow),
  fetchSkinTexture: (url: string): Promise<string> => ipcRenderer.invoke(IpcChannel.SkinFetchTexture, url),
  uploadSkin: (profile: MinecraftProfile, pngBytes: ArrayBuffer, variant: SkinVariant, name: string): Promise<SkinUploadResult> =>
    ipcRenderer.invoke(IpcChannel.SkinUpload, profile, pngBytes, variant, name),
  loadSkinPngForEditor: (): Promise<{ dataUri: string; width: number; height: number } | null> =>
    ipcRenderer.invoke(IpcChannel.SkinEditorLoadPng),
  loadSkinTemplate: (variant: SkinVariant): Promise<string> => ipcRenderer.invoke(IpcChannel.SkinEditorLoadTemplate, variant),
  exportSkinPng: (pngBytes: ArrayBuffer, suggestedFileName: string): Promise<{ path: string } | null> =>
    ipcRenderer.invoke(IpcChannel.SkinEditorExportPng, pngBytes, suggestedFileName),
  listSkinLibrary: (): Promise<SkinLibraryEntry[]> => ipcRenderer.invoke(IpcChannel.SkinLibraryList),
  saveSkinToLibrary: (pngBytes: ArrayBuffer, variant: SkinVariant, name: string, existingId?: string): Promise<SkinLibraryEntry> =>
    ipcRenderer.invoke(IpcChannel.SkinLibrarySave, pngBytes, variant, name, existingId),
  deleteSkinFromLibrary: (id: string): Promise<void> => ipcRenderer.invoke(IpcChannel.SkinLibraryDelete, id),
  useSkinFromLibrary: (profile: MinecraftProfile, id: string): Promise<MinecraftProfile | null> =>
    ipcRenderer.invoke(IpcChannel.SkinLibraryUse, profile, id),
  loadSkinFromLibraryForEdit: (id: string): Promise<SkinLibraryEntry | null> =>
    ipcRenderer.invoke(IpcChannel.SkinLibraryLoadForEdit, id),
  renameSkinInLibrary: (id: string, name: string): Promise<SkinLibraryEntry | null> =>
    ipcRenderer.invoke(IpcChannel.SkinLibraryRename, id, name),
  selectCapePng: (): Promise<{ dataUri: string; width: number; height: number } | null> =>
    ipcRenderer.invoke(IpcChannel.CapeSelectPng),
  uploadCape: (profile: MinecraftProfile, pngDataUri: string): Promise<CapeUploadResult> =>
    ipcRenderer.invoke(IpcChannel.CapeUpload, profile, pngDataUri),
  deleteCape: (profile: MinecraftProfile): Promise<void> => ipcRenderer.invoke(IpcChannel.CapeDelete, profile),
  getCapeStatus: (profile: MinecraftProfile): Promise<CustomCapeStatus> => ipcRenderer.invoke(IpcChannel.CapeStatus, profile),
  listCapeLibrary: (): Promise<CapeLibraryEntry[]> => ipcRenderer.invoke(IpcChannel.CapeLibraryList),
  saveCapeToLibrary: (pngDataUri: string, name: string): Promise<CapeLibraryEntry> =>
    ipcRenderer.invoke(IpcChannel.CapeLibrarySave, pngDataUri, name),
  deleteCapeFromLibrary: (id: string): Promise<void> => ipcRenderer.invoke(IpcChannel.CapeLibraryDelete, id),
  updateCapeInLibrary: (id: string, pngDataUri: string, name: string): Promise<CapeLibraryEntry | null> =>
    ipcRenderer.invoke(IpcChannel.CapeLibraryUpdate, id, pngDataUri, name),
  activateLibraryCape: (profile: MinecraftProfile, id: string): Promise<CapeUploadResult> =>
    ipcRenderer.invoke(IpcChannel.CapeLibraryActivate, profile, id),
  checkModBundleUpdate: (versionId: string): Promise<ModBundleUpdateInfo> =>
    ipcRenderer.invoke(IpcChannel.ModBundleCheckUpdate, versionId),
  applyModBundleUpdate: (versionId: string): Promise<LauncherSettings> =>
    ipcRenderer.invoke(IpcChannel.ModBundleApplyUpdate, versionId),
  listBundleCompatibleVersions: (): Promise<string[]> => ipcRenderer.invoke(IpcChannel.ModBundleListCompatibleVersions),
  getStorageInfo: (): Promise<StorageInfo> => ipcRenderer.invoke(IpcChannel.StorageInfo),
  changeStorageLocation: (): Promise<{ path: string } | null> => ipcRenderer.invoke(IpcChannel.StorageChangeLocation),
  getSystemMemoryInfo: (): Promise<SystemMemoryInfo> => ipcRenderer.invoke(IpcChannel.SystemMemoryInfo),
  openConsoleWindow: (): Promise<void> => ipcRenderer.invoke(IpcChannel.ConsoleWindowOpen),

  onAuthProgress: (callback: (event: AuthProgressEvent) => void): (() => void) =>
    subscribe(IpcChannel.AuthProgress, callback),
  onLaunchProgress: (callback: (event: LaunchProgressEvent) => void): (() => void) =>
    subscribe(IpcChannel.LaunchProgress, callback),
  onLaunchBusyChanged: (callback: (busy: boolean) => void): (() => void) =>
    subscribe(IpcChannel.LaunchBusyChanged, callback),
  onGameLog: (callback: (event: GameLogEvent) => void): (() => void) => subscribe(IpcChannel.GameLog, callback),
  onUpdateStatus: (callback: (event: UpdateStatus) => void): (() => void) => subscribe(IpcChannel.UpdateStatus, callback),
  onStorageMoveProgress: (callback: (event: StorageMoveProgressEvent) => void): (() => void) =>
    subscribe(IpcChannel.StorageMoveProgress, callback)
}

contextBridge.exposeInMainWorld('api', api)

export type LauncherApi = typeof api
