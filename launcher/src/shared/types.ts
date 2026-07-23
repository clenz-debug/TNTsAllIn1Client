export interface MinecraftProfile {
  id: string
  name: string
  accessToken: string
  /** true when steps 4/5 of the auth chain (login_with_xbox, profile) were mocked because the
   * Mojang API allowlist request (aka.ms/mce-reviewappid) has not been approved yet. */
  isMock: boolean
}

export interface AuthProgressEvent {
  step: 'ms-oauth' | 'xbox-live' | 'xsts' | 'minecraft-login' | 'profile' | 'done' | 'error'
  message: string
}

export type LaunchStage =
  | 'manifest'
  | 'client-jar'
  | 'libraries'
  | 'assets'
  | 'fabric-meta'
  | 'fabric-libraries'
  | 'launching'
  | 'running'
  | 'done'
  | 'error'

export interface LaunchProgressEvent {
  stage: LaunchStage
  completed: number
  total: number
  label?: string
}

export interface GameLogEvent {
  source: 'launcher' | 'game'
  level: 'info' | 'error'
  message: string
}

export const MINECRAFT_VERSION = '1.21.11'
