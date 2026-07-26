import { app } from 'electron'
import { copyFile, mkdir, readdir } from 'node:fs/promises'
import { join } from 'node:path'
import type { LaunchStage } from '../../shared/types'

export type InstallProgressCallback = (
  stage: LaunchStage,
  completed: number,
  total: number,
  label?: string
) => void

async function listBundleFiles(bundleDir: string): Promise<string[]> {
  try {
    return await readdir(bundleDir)
  } catch {
    // launcher/mods-bundle and launcher/resourcepacks-bundle are git-ignored, dev-populated
    // folders (see Aktuelle_Phase.md) — nothing to sync on a checkout where they're empty/absent.
    return []
  }
}

async function syncBundleDir(bundleDir: string, destinationDir: string): Promise<void> {
  const files = await listBundleFiles(bundleDir)
  if (files.length === 0) return

  await mkdir(destinationDir, { recursive: true })
  await Promise.all(files.map((file) => copyFile(join(bundleDir, file), join(destinationDir, file))))
}

/**
 * Copies the bundled mod jars and the bundled resourcepack(s) into the instance's `game/mods`
 * and `game/resourcepacks` folders on every launch. Previously both were a one-off manual copy
 * (see Phase 4/5p in Aktuelle_Phase.md) — meaning a fresh or reset instance directory silently
 * lost them, which is exactly what happened here. Only ever adds/overwrites the bundle's own
 * files, never deletes anything else already in those folders, so anything the user places there
 * by hand survives a sync.
 *
 * Doesn't touch `options.txt` — a resourcepack still has to be enabled once in-game (Optionen ->
 * Ressourcenpakete), same as any resourcepack in vanilla Minecraft. This only guarantees the file
 * itself is always there to enable.
 */
export async function syncBundledContent(
  instanceDir: string,
  onProgress: InstallProgressCallback
): Promise<void> {
  onProgress('bundles', 0, 1)
  const appRoot = app.getAppPath()
  const gameDir = join(instanceDir, 'game')

  await syncBundleDir(join(appRoot, 'mods-bundle'), join(gameDir, 'mods'))
  await syncBundleDir(join(appRoot, 'resourcepacks-bundle'), join(gameDir, 'resourcepacks'))
  onProgress('bundles', 1, 1)
}
