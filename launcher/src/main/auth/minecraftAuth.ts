import { localizedErrorMessage } from '../../shared/errorMessages'
import type { MinecraftCape, MinecraftProfile, MinecraftSkin } from '../../shared/types'
import type { XstsResult } from './xboxLive'

interface MinecraftLoginResponse {
  access_token: string
}

interface MinecraftProfileResponse {
  id: string
  name: string
  skins: MinecraftSkin[]
  capes: MinecraftCape[]
}

class MinecraftApiError extends Error {
  constructor(status: number, body: string) {
    super(localizedErrorMessage('auth.minecraftApiFailed', { status, detail: body }))
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

/** Steps 4+5 of the auth chain: exchange the XSTS token for a Minecraft access token, then fetch
 * the account's profile. Any failure here (e.g. an account that doesn't own Minecraft) propagates
 * to the caller - the login screen shows it as a normal error. */
export async function completeMinecraftLogin(xsts: XstsResult): Promise<MinecraftProfile> {
  const accessToken = await loginWithXbox(xsts)
  const profile = await fetchProfile(accessToken)
  return { id: profile.id, name: profile.name, accessToken, skins: profile.skins, capes: profile.capes }
}
