import { join } from 'node:path'
import { localizedError } from '../../shared/errorMessages'
import type { LaunchStage } from '../../shared/types'
import { dataRoot } from '../dataRoot'
import { librariesForCurrentOs, libraryDestinationPath } from './classpath'
import { downloadAll, type DownloadTask } from './downloader'
import { fetchVersionDetail, type VersionDetail } from './versionManifest'

export interface InstalledVersion {
  detail: VersionDetail
  instanceDir: string
  /** Shared across every instance/version (see `sharedAssetsDir` below) - not `assets_root`'s old
   * per-instance path (removed for the same duplication reason as `libraries/`, see
   * `classpath.ts#libraryDestinationPath`'s doc comment). */
  assetsDir: string
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
  return join(dataRoot(), 'instances', instanceId)
}

/** Shared, version-keyed home for the vanilla client jar - siblings across every instance targeting
 * the same version download/reuse the exact same file instead of each keeping its own copy (own
 * user request: instances were multiplying multi-GB downloads for identical content). */
function sharedVersionDir(versionId: string): string {
  return join(dataRoot(), 'versions', versionId)
}

/** Where {@link installVersion} puts (and later launches use) the vanilla client jar for a given
 * version - exported so other code that needs to read something out of an already-downloaded jar
 * (e.g. `main/skin/defaultTemplate.ts` extracting the built-in Steve/Alex textures) resolves the
 * exact same path instead of re-deriving the `versions/<id>/<id>.jar` convention by hand. */
export function sharedClientJarPath(versionId: string): string {
  return join(sharedVersionDir(versionId), `${versionId}.jar`)
}

/** Shared home for the Mojang asset store (`indexes/`+`objects/`, typically 1 GB+) - same
 * deduplication reasoning as {@link sharedVersionDir}, just not version-keyed since asset objects
 * are content-addressed by hash already and safely reused across versions too. */
function sharedAssetsDir(): string {
  return join(dataRoot(), 'assets')
}

interface AssetIndex {
  objects: Record<string, { hash: string }>
}

async function fetchAssetIndex(url: string, signal?: AbortSignal): Promise<AssetIndex> {
  const response = await fetch(url, { signal })
  if (!response.ok) {
    throw localizedError('launch.assetIndexFetchFailed', { status: response.status })
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
  instanceId: string,
  signal?: AbortSignal
): Promise<InstalledVersion> {
  onProgress('manifest', 0, 1, versionId)
  const detail = await fetchVersionDetail(versionId, signal)
  onProgress('manifest', 1, 1, versionId)

  const dir = instanceDir(instanceId)
  const clientJarPath = sharedClientJarPath(detail.id)

  await downloadAll(
    [{ url: detail.downloads.client.url, destination: clientJarPath, sha1: detail.downloads.client.sha1 }],
    1,
    (completed, total) => onProgress('client-jar', completed, total, detail.id),
    signal
  )

  const libraries = librariesForCurrentOs(detail.libraries)
  const libraryTasks: DownloadTask[] = []
  const libraryPaths: string[] = []
  for (const lib of libraries) {
    const artifact = lib.downloads?.artifact
    if (!artifact) continue
    const destination = libraryDestinationPath(artifact.path)
    libraryTasks.push({ url: artifact.url, destination, sha1: artifact.sha1 })
    libraryPaths.push(destination)
  }
  await downloadAll(libraryTasks, 8, (completed, total, label) => onProgress('libraries', completed, total, label), signal)

  const assetsDir = sharedAssetsDir()
  const assetIndex = await fetchAssetIndex(detail.assetIndex.url, signal)
  const assetIndexDestination = join(assetsDir, 'indexes', `${detail.assetIndex.id}.json`)
  await downloadAll(
    [{ url: detail.assetIndex.url, destination: assetIndexDestination, sha1: detail.assetIndex.sha1 }],
    1,
    () => undefined,
    signal
  )

  const objectsDir = join(assetsDir, 'objects')
  const assetTasks: DownloadTask[] = Object.values(assetIndex.objects).map((object) => ({
    url: `https://resources.download.minecraft.net/${object.hash.slice(0, 2)}/${object.hash}`,
    destination: join(objectsDir, object.hash.slice(0, 2), object.hash),
    sha1: object.hash
  }))
  await downloadAll(assetTasks, 16, (completed, total, label) => onProgress('assets', completed, total, label), signal)

  return { detail, instanceDir: dir, assetsDir, clientJarPath, libraryPaths }
}
