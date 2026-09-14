import { existsSync } from 'node:fs'
import type { Readable } from 'node:stream'
import { openPromise } from 'yauzl'
import type { SkinVariant } from '../../shared/types'
import { sharedClientJarPath } from '../launch/installer'
import { loadLauncherSettings } from '../launcherSettings'

/**
 * In-jar paths of Mojang's own built-in default skin textures (`DefaultPlayerSkin` in the client
 * code embeds these rather than serving them from a stable, fetchable CDN hash - there is no
 * network endpoint for "the current default Steve/Alex", only the ones baked into each client
 * jar). Verified against a real downloaded 1.21.11 client jar - re-check here first if a future
 * Minecraft version ever moves these.
 */
const TEMPLATE_ENTRY_PATH: Record<SkinVariant, string> = {
  classic: 'assets/minecraft/textures/entity/player/wide/steve.png',
  slim: 'assets/minecraft/textures/entity/player/slim/alex.png'
}

async function streamToBuffer(stream: Readable): Promise<Buffer> {
  const chunks: Buffer[] = []
  for await (const chunk of stream) {
    chunks.push(chunk as Buffer)
  }
  return Buffer.concat(chunks)
}

/** Prefers the currently selected instance's version (most likely to already be installed and
 * what the user is about to play), falls back to any other instance whose client jar happens to
 * exist on disk already. */
async function findInstalledVersionId(): Promise<string | null> {
  const settings = await loadLauncherSettings()
  const ordered = [...settings.instances].sort((a, b) => {
    if (a.id === settings.selectedInstanceId) return -1
    if (b.id === settings.selectedInstanceId) return 1
    return 0
  })
  return ordered.find((instance) => existsSync(sharedClientJarPath(instance.versionId)))?.versionId ?? null
}

/**
 * Extracts Mojang's own built-in Steve/Alex texture directly out of an already-downloaded vanilla
 * client jar, rather than bundling a copy of Mojang's copyrighted assets in this repo ourselves -
 * same "never redistribute Mojang's own files, only ever fetch/derive them from a legitimate
 * install" principle already followed for the mods-bundle/resourcepacks-bundle folders (see
 * Projekt_Roadmap.md's license section). This is the skin pixel editor's "start from a Steve/Alex
 * template" option (Phase 7 step 3).
 */
export async function loadDefaultSkinTemplate(variant: SkinVariant): Promise<Buffer> {
  const versionId = await findInstalledVersionId()
  if (!versionId) {
    throw new Error('Erst eine Instanz starten (Play-Klick), um die Steve/Alex-Vorlage laden zu können.')
  }

  const jarPath = sharedClientJarPath(versionId)
  const entryPath = TEMPLATE_ENTRY_PATH[variant]
  const zipfile = await openPromise(jarPath, { lazyEntries: true, autoClose: true })
  try {
    for await (const entry of zipfile.eachEntry()) {
      if (entry.fileName === entryPath) {
        const stream = await zipfile.openReadStreamPromise(entry)
        return await streamToBuffer(stream)
      }
    }
  } finally {
    zipfile.close()
  }
  throw new Error(
    `Konnte "${entryPath}" nicht in ${jarPath} finden - der Pfad hat sich vermutlich mit einer neueren Minecraft-Version geändert.`
  )
}
