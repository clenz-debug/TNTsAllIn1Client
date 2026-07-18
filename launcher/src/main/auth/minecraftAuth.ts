import type { MinecraftProfile } from '../../shared/types'
import type { XstsResult } from './xboxLive'

interface MinecraftLoginResponse {
  access_token: string
}

interface MinecraftProfileResponse {
  id: string
  name: string
}

const MOCK_PROFILE: MinecraftProfile = {
  id: '00000000-0000-0000-0000-000000000000',
  name: 'DevPlayer',
  accessToken: 'dev-fake-token',
  isMock: true
}

class MinecraftApiError extends Error {
  constructor(status: number, body: string) {
    super(`Minecraft API call failed (${status}): ${body}`)
    this.name = 'MinecraftApiError'
  }
}

async function loginWithXbox(xsts: XstsResult): Promise<string> {
  const response = await fetch('https://api.minecraftservices.com/authentication/login_with_xbox', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ identityToken: `XBL3.0 x=${xsts.userHash};${xsts.xstsToken}` })
  })
  if (!response.ok) {
    throw new MinecraftApiError(response.status, await response.text())
  }
  const data = (await response.json()) as MinecraftLoginResponse
  return data.access_token
}

async function fetchProfile(minecraftAccessToken: string): Promise<MinecraftProfileResponse> {
  const response = await fetch('https://api.minecraftservices.com/minecraft/profile', {
    headers: { Authorization: `Bearer ${minecraftAccessToken}` }
  })
  if (!response.ok) {
    throw new MinecraftApiError(response.status, await response.text())
  }
  return (await response.json()) as MinecraftProfileResponse
}

/**
 * Steps 4+5 of the auth chain. Until the Azure app is approved via aka.ms/mce-reviewappid,
 * api.minecraftservices.com answers with 403 here — in that case (or any other failure at this
 * stage) we fall back to a clearly-marked dev profile so the download/launch pipeline can be
 * built and tested without waiting on Mojang. Delete the catch block once the real calls work;
 * nothing else needs to change.
 */
export async function completeMinecraftLogin(xsts: XstsResult): Promise<MinecraftProfile> {
  try {
    const accessToken = await loginWithXbox(xsts)
    const profile = await fetchProfile(accessToken)
    return { id: profile.id, name: profile.name, accessToken, isMock: false }
  } catch (error) {
    console.warn('[auth] Minecraft API not available yet, falling back to dev mock profile:', error)
    return MOCK_PROFILE
  }
}

export function createMockProfile(): MinecraftProfile {
  return MOCK_PROFILE
}
