import { contextBridge, ipcRenderer } from 'electron'
import { IpcChannel } from '../shared/ipc'
import type {
  AuthProgressEvent,
  GameLogEvent,
  GameVersionSummary,
  LaunchProgressEvent,
  LauncherSettings,
  MinecraftProfile,
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
  loginMock: (): Promise<MinecraftProfile> => ipcRenderer.invoke(IpcChannel.AuthLoginMock),
  play: (profile: MinecraftProfile, instanceId: string): Promise<void> =>
    ipcRenderer.invoke(IpcChannel.LaunchPlay, profile, instanceId),
  openExternal: (url: string): Promise<void> => ipcRenderer.invoke(IpcChannel.ShellOpenExternal, url),
  listVersions: (): Promise<GameVersionSummary[]> => ipcRenderer.invoke(IpcChannel.VersionsList),
  loadSettings: (): Promise<LauncherSettings> => ipcRenderer.invoke(IpcChannel.SettingsLoad),
  saveSettings: (settings: LauncherSettings): Promise<void> => ipcRenderer.invoke(IpcChannel.SettingsSave, settings),
  listBundledMods: (): Promise<string[]> => ipcRenderer.invoke(IpcChannel.ModsListBundled),
  listCustomMods: (instanceId: string): Promise<string[]> => ipcRenderer.invoke(IpcChannel.ModsListCustom, instanceId),
  addCustomMods: (instanceId: string): Promise<string[]> => ipcRenderer.invoke(IpcChannel.ModsAddCustom, instanceId),
  removeCustomMod: (instanceId: string, fileName: string): Promise<string[]> =>
    ipcRenderer.invoke(IpcChannel.ModsRemoveCustom, instanceId, fileName),
  deleteInstance: (instanceId: string): Promise<LauncherSettings> => ipcRenderer.invoke(IpcChannel.InstancesDelete, instanceId),
  installUpdateNow: (): Promise<void> => ipcRenderer.invoke(IpcChannel.UpdateInstallNow),
  fetchSkinTexture: (url: string): Promise<string> => ipcRenderer.invoke(IpcChannel.SkinFetchTexture, url),
  uploadSkin: (profile: MinecraftProfile, variant: 'classic' | 'slim'): Promise<MinecraftProfile | null> =>
    ipcRenderer.invoke(IpcChannel.SkinUpload, profile, variant),

  onAuthProgress: (callback: (event: AuthProgressEvent) => void): (() => void) =>
    subscribe(IpcChannel.AuthProgress, callback),
  onLaunchProgress: (callback: (event: LaunchProgressEvent) => void): (() => void) =>
    subscribe(IpcChannel.LaunchProgress, callback),
  onGameLog: (callback: (event: GameLogEvent) => void): (() => void) => subscribe(IpcChannel.GameLog, callback),
  onUpdateStatus: (callback: (event: UpdateStatus) => void): (() => void) => subscribe(IpcChannel.UpdateStatus, callback)
}

contextBridge.exposeInMainWorld('api', api)

export type LauncherApi = typeof api
