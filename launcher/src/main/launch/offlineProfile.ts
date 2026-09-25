import { app } from 'electron'
import { copyFile, mkdir, readFile, rm, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import type { MinecraftProfile } from '../../shared/types'
import { publicUrlFor } from '../cape/capeStorage'

/**
 * Own skin and cape in offline mode (own user request): Minecraft normally fetches both from the
 * internet, so offline the player would be a default Steve/Alex without a cape. Every online
 * launch keeps a copy of the account's active skin and cape here; an offline launch hands that
 * copy to our mod, which shows it for the local player (`OfflineProfile`/`SkinManagerMixin` in
 * `mod/`). Other players aren't affected - offline there are none anyway, except on LAN.
 */

interface CachedProfileInfo {
  model: 'slim' | 'wide'
  skin: boolean
  cape: boolean
}

/** Per account, next to `auth.json` - it belongs to the login, not to any instance. */
function cacheDir(uuid: string): string {
  return join(app.getPath('userData'), 'offline-profile', uuid.replace(/-/g, '').toLowerCase())
}

const FETCH_TIMEOUT_MS = 8_000

async function fetchPng(url: string): Promise<Buffer | null> {
  const response = await fetch(url, { signal: AbortSignal.timeout(FETCH_TIMEOUT_MS) })
  if (!response.ok) return null
  return Buffer.from(await response.arrayBuffer())
}

/**
 * Online only: stores the active skin and cape of `profile`. The cape is our own one from the cape
 * server if there is one (it's what Cape Provider shows online too), otherwise the active Mojang
 * cape. Never throws - a failed refresh just keeps the previous copy.
 */
export async function refreshOfflineProfileCache(profile: MinecraftProfile): Promise<void> {
  try {
    const dir = cacheDir(profile.id)
    await mkdir(dir, { recursive: true })

    const activeSkin = profile.skins.find((skin) => skin.state === 'ACTIVE') ?? profile.skins[0] ?? null
    const skin = activeSkin ? await fetchPng(activeSkin.url) : null
    const activeMojangCape = profile.capes.find((cape) => cape.state === 'ACTIVE') ?? null
    const cape = (await fetchPng(publicUrlFor(profile.id))) ?? (activeMojangCape ? await fetchPng(activeMojangCape.url) : null)

    if (skin) await writeFile(join(dir, 'skin.png'), skin)
    else await rm(join(dir, 'skin.png'), { force: true })
    if (cape) await writeFile(join(dir, 'cape.png'), cape)
    else await rm(join(dir, 'cape.png'), { force: true })

    const info: CachedProfileInfo = { model: activeSkin?.variant === 'SLIM' ? 'slim' : 'wide', skin: !!skin, cape: !!cape }
    await writeFile(join(dir, 'profile.json'), JSON.stringify(info, null, 2))
  } catch (err) {
    console.warn('[offline] Could not refresh the offline skin/cape copy:', err)
  }
}

/** The handoff file our mod reads (`OfflineProfile.java`), plus its PNGs in a folder next to it. */
function offlineFile(gameDir: string): string {
  return join(gameDir, 'config', 'tntsallin1client-offline.json')
}

function offlineAssetsDir(gameDir: string): string {
  return join(gameDir, 'config', 'tntsallin1client-offline')
}

/**
 * Right before launch: tells the mod whether this is an offline launch and, if so, gives it the
 * cached skin/cape. An online launch writes `offline: false`, so a copy left over from an earlier
 * offline launch never overrides the real skin.
 */
export async function writeOfflineProfileFiles(gameDir: string, profile: MinecraftProfile): Promise<void> {
  const assetsDir = offlineAssetsDir(gameDir)
  await rm(assetsDir, { recursive: true, force: true })
  await mkdir(join(gameDir, 'config'), { recursive: true })

  let info: CachedProfileInfo | null = null
  if (profile.offline) {
    const dir = cacheDir(profile.id)
    try {
      info = JSON.parse(await readFile(join(dir, 'profile.json'), 'utf8')) as CachedProfileInfo
      await mkdir(assetsDir, { recursive: true })
      if (info.skin) await copyFile(join(dir, 'skin.png'), join(assetsDir, 'skin.png'))
      if (info.cape) await copyFile(join(dir, 'cape.png'), join(assetsDir, 'cape.png'))
    } catch {
      // Never launched online with this account on this launcher version - vanilla's default skin then.
      info = null
    }
  }

  await writeFile(
    offlineFile(gameDir),
    JSON.stringify({ offline: !!profile.offline, model: info?.model ?? 'wide', skin: info?.skin ?? false, cape: info?.cape ?? false }, null, 2)
  )
}
