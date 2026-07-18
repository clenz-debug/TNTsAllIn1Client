import { app } from 'electron'
import { join } from 'node:path'
import { MINECRAFT_VERSION, type LaunchStage } from '../../shared/types'
import { librariesForCurrentOs, libraryDestinationPath } from './classpath'
import { downloadAll, type DownloadTask } from './downloader'
import { fetchVersionDetail, type VersionDetail } from './versionManifest'

export interface InstalledVersion {
  detail: VersionDetail
  instanceDir: string
  clientJarPath: string
  libraryPaths: string[]
}

export function instanceDir(): string {
  return join(app.getPath('userData'), 'instances', 'default')
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

export async function installVersion(onProgress: InstallProgressCallback): Promise<InstalledVersion> {
  onProgress('manifest', 0, 1, MINECRAFT_VERSION)
  const detail = await fetchVersionDetail(MINECRAFT_VERSION)
  onProgress('manifest', 1, 1, MINECRAFT_VERSION)

  const dir = instanceDir()
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
