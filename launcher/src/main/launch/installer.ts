import { app } from 'electron'
import { join } from 'node:path'
import type { LaunchStage } from '../../shared/types'
import { librariesForCurrentOs, libraryDestinationPath } from './classpath'
import { downloadAll, type DownloadTask } from './downloader'
import { fetchVersionDetail, type VersionDetail } from './versionManifest'

export interface InstalledVersion {
  detail: VersionDetail
  instanceDir: string
  clientJarPath: string
  libraryPaths: string[]
}

/** One directory per real, user-created {@link Instance} (`instances/<instanceId>/`) - each
 * instance keeps its own saves/options/mods/resourcepacks, never shared with another instance even
 * if both target the same Minecraft version (that's the whole point of the instance system, see
 * `shared/types.ts`'s `Instance` doc comment). Before the instance system existed (Phase 6a-6e)
 * this was keyed by version id directly, one shared folder per version - `launcherSettings.ts`'s
 * migration turns any such leftover folder into a real instance whose id equals the old folder
 * name, so this function itself didn't need to change shape, only what its argument conceptually
 * means. */
export function instanceDir(instanceId: string): string {
  return join(app.getPath('userData'), 'instances', instanceId)
}

interface AssetIndex {
  objects: Record<string, { hash: string }>
}

async function fetchAssetIndex(url: string): Promise<AssetIndex> {
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`Failed to fetch asset index: ${response.status}`)
  }
  return (await response.json()) as AssetIndex
}

export type InstallProgressCallback = (
  stage: LaunchStage,
  completed: number,
  total: number,
  label?: string
) => void

export async function installVersion(
  onProgress: InstallProgressCallback,
  versionId: string,
  instanceId: string
): Promise<InstalledVersion> {
  onProgress('manifest', 0, 1, versionId)
  const detail = await fetchVersionDetail(versionId)
  onProgress('manifest', 1, 1, versionId)

  const dir = instanceDir(instanceId)
  const clientJarPath = join(dir, 'versions', detail.id, `${detail.id}.jar`)

  await downloadAll(
    [{ url: detail.downloads.client.url, destination: clientJarPath, sha1: detail.downloads.client.sha1 }],
    1,
    (completed, total) => onProgress('client-jar', completed, total, detail.id)
  )

  const libraries = librariesForCurrentOs(detail.libraries)
  const libraryTasks: DownloadTask[] = []
  const libraryPaths: string[] = []
  for (const lib of libraries) {
    const artifact = lib.downloads?.artifact
    if (!artifact) continue
    const destination = libraryDestinationPath(dir, artifact.path)
    libraryTasks.push({ url: artifact.url, destination, sha1: artifact.sha1 })
    libraryPaths.push(destination)
  }
  await downloadAll(libraryTasks, 8, (completed, total, label) => onProgress('libraries', completed, total, label))

  const assetIndex = await fetchAssetIndex(detail.assetIndex.url)
  const assetIndexDestination = join(dir, 'assets', 'indexes', `${detail.assetIndex.id}.json`)
  await downloadAll(
    [{ url: detail.assetIndex.url, destination: assetIndexDestination, sha1: detail.assetIndex.sha1 }],
    1,
    () => undefined
  )

  const objectsDir = join(dir, 'assets', 'objects')
  const assetTasks: DownloadTask[] = Object.values(assetIndex.objects).map((object) => ({
    url: `https://resources.download.minecraft.net/${object.hash.slice(0, 2)}/${object.hash}`,
    destination: join(objectsDir, object.hash.slice(0, 2), object.hash),
    sha1: object.hash
  }))
  await downloadAll(assetTasks, 16, (completed, total, label) => onProgress('assets', completed, total, label))

  return { detail, instanceDir: dir, clientJarPath, libraryPaths }
}
