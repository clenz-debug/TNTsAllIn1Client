import { existsSync } from 'node:fs'
import { readFile } from 'node:fs/promises'
import { join } from 'node:path'
import type { GameLogEvent } from '../../shared/types'
import { downloadAll } from './downloader'
import { sharedClientJarPath, type InstallProgressCallback } from './installer'
import { buildNewTexturesPack, PACK_MARKER_FILE, type PackMarker } from './newTexturesBuilder'
import { NEW_TEXTURES_RULES } from './newTexturesRules'
import { fetchManifestVersions, fetchVersionDetail } from './versionManifest'

/**
 * The pack's folder name in an instance's `resourcepacks/` - also what the game lists it as, and
 * what our mod's "New Textures" switch looks for (`NewTextures.PACK_NAME` in `mod/1.8.9`).
 */
export const NEW_TEXTURES_PACK_NAME = 'TNT New Textures'

async function readMarker(packDir: string): Promise<PackMarker | null> {
  try {
    return JSON.parse(await readFile(join(packDir, PACK_MARKER_FILE), 'utf8')) as PackMarker
  } catch {
    return null
  }
}

/**
 * Makes sure a legacy instance has the "New Textures" resource pack: Minecraft's current look for
 * the old version's blocks, items and (where they still fit) mobs, switched on and off in our mod's
 * menu. Called on every start of a legacy version our mod exists for; does nothing once the pack is
 * there and up to date.
 *
 * The pictures come from the newest release's client jar - the one in the shared `versions/` folder
 * if the player has that version installed, otherwise downloaded there once from Mojang, the same
 * file and place a first start of that version would fetch. We ship only the table of which
 * texture replaces which (`newTexturesRules.ts`); Mojang's textures are never part of the launcher.
 *
 * A newer release alone does not trigger another download: an existing pack is only rebuilt when
 * its rules changed or when a newer version's jar is already on disk anyway.
 *
 * Never fails a launch - without internet on the first start, or with a file it can't read, the
 * game just starts without the pack (or with the one it had) and the reason goes to the log.
 */
export async function ensureNewTexturesPack(
  gameDir: string,
  legacyVersionId: string,
  legacyClientJarPath: string,
  onProgress: InstallProgressCallback,
  log: (event: GameLogEvent) => void,
  signal?: AbortSignal
): Promise<void> {
  const rules = NEW_TEXTURES_RULES[legacyVersionId]
  if (!rules) return

  const packDir = join(gameDir, 'resourcepacks', NEW_TEXTURES_PACK_NAME)
  try {
    const marker = await readMarker(packDir)
    const newest = (await fetchManifestVersions()).find((version) => version.type === 'release')?.id
    if (!newest) return
    const newestOnDisk = existsSync(sharedClientJarPath(newest))
    if (marker && marker.revision === rules.revision && (marker.source === newest || !newestOnDisk)) return

    let source = newest
    if (!newestOnDisk) {
      if (marker && existsSync(sharedClientJarPath(marker.source))) {
        // Only the rules changed - what the pack was built from is still there.
        source = marker.source
      } else {
        log({
          source: 'launcher',
          level: 'info',
          message: `Lade Minecraft ${newest} für das Paket „Neue Texturen“ herunter (einmalig, braucht Internet)…`
        })
        const detail = await fetchVersionDetail(newest, signal)
        await downloadAll(
          [{ url: detail.downloads.client.url, destination: sharedClientJarPath(newest), sha1: detail.downloads.client.sha1 }],
          1,
          (completed, total) => onProgress('client-jar', completed, total, newest),
          signal
        )
      }
    }

    onProgress('bundles', 0, 1, NEW_TEXTURES_PACK_NAME)
    const result = await buildNewTexturesPack(legacyClientJarPath, sharedClientJarPath(source), rules, packDir, {
      source,
      revision: rules.revision
    })
    onProgress('bundles', 1, 1, NEW_TEXTURES_PACK_NAME)
    log({
      source: 'launcher',
      level: 'info',
      message: `Paket „Neue Texturen“ aus Minecraft ${source} gebaut: ${result.replaced} Texturen ersetzt, ${result.skipped.length} bleiben alt.`
    })
  } catch (err) {
    // A deliberate cancel stops the whole launch.
    if (signal?.aborted) throw err
    log({
      source: 'launcher',
      level: 'error',
      message: `Paket „Neue Texturen“ konnte nicht gebaut werden (${err instanceof Error ? err.message : String(err)}) - das Spiel startet ohne bzw. mit dem bisherigen Paket.`
    })
  }
}
