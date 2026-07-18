import { contextBridge, ipcRenderer } from 'electron'
import { IpcChannel } from '../shared/ipc'
import type { AuthProgressEvent, GameLogEvent, LaunchProgressEvent, MinecraftProfile } from '../shared/types'

function subscribe<T>(channel: string, callback: (event: T) => void): () => void {
  const listener = (_: Electron.IpcRendererEvent, payload: T): void => callback(payload)
  ipcRenderer.on(channel, listener)
  return () => ipcRenderer.removeListener(channel, listener)
}

const api = {
  restoreSession: (): Promise<MinecraftProfile | null> => ipcRenderer.invoke(IpcChannel.AuthRestore),
  login: (): Promise<MinecraftProfile> => ipcRenderer.invoke(IpcChannel.AuthLogin),
  loginMock: (): Promise<MinecraftProfile> => ipcRenderer.invoke(IpcChannel.AuthLoginMock),
  play: (profile: MinecraftProfile): Promise<void> => ipcRenderer.invoke(IpcChannel.LaunchPlay, profile),

  onAuthProgress: (callback: (event: AuthProgressEvent) => void): (() => void) =>
    subscribe(IpcChannel.AuthProgress, callback),
  onLaunchProgress: (callback: (event: LaunchProgressEvent) => void): (() => void) =>
    subscribe(IpcChannel.LaunchProgress, callback),
  onGameLog: (callback: (event: GameLogEvent) => void): (() => void) => subscribe(IpcChannel.GameLog, callback)
}

contextBridge.exposeInMainWorld('api', api)

export type LauncherApi = typeof api
