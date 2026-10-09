import { mkdir } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import type { LaunchStage } from '../../shared/types'
import { libraryDestinationPath } from './classpath'
import { downloadAll, type DownloadTask } from './downloader'
import { fetchFabricProfile, fetchLatestStableLoaderVersion } from './fabricMeta'
import type { InstalledVersion } from './installer'

function mavenCoordinateToPath(coordinate: string): string {
  const [group, artifact, version, classifier] = coordinate.split(':')
  const groupPath = group.replace(/\./g, '/')
  const fileName = classifier ? `${artifact}-${version}-${classifier}.jar` : `${artifact}-${version}.jar`
  return `${groupPath}/${artifact}/${version}/${fileName}`
}

export type InstallProgressCallback = (
  stage: LaunchStage,
  completed: number,
  total: number,
  label?: string
) => void

/** Layers Fabric Loader on top of an already-installed vanilla version: downloads the loader's
 * own libraries (loader, intermediary mappings, ASM, mixin, ...) and overrides mainClass so the
 * game boots through Fabric's KnotClient instead of net.minecraft.client.main.Main. The vanilla
 * client jar is untouched — Fabric patches classes at runtime via its own classloader, it doesn't
 * replace the jar. */
export async function installFabricLoader(
  vanilla: InstalledVersion,
  onProgress: InstallProgressCallback,
  signal?: AbortSignal
): Promise<InstalledVersion> {
  onProgress('fabric-meta', 0, 1, vanilla.detail.id)
  const loaderVersion = await fetchLatestStableLoaderVersion(vanilla.detail.id, signal)
  const profile = await fetchFabricProfile(vanilla.detail.id, loaderVersion, signal)
  onProgress('fabric-meta', 1, 1, profile.id)

  const tasks: DownloadTask[] = []
  const libraryPaths: string[] = []
  for (const lib of profile.libraries) {
    const relativePath = mavenCoordinateToPath(lib.name)
    const destination = libraryDestinationPath(relativePath)
    tasks.push({ url: `${lib.url}${relativePath}`, destination, sha1: lib.sha1 })
    libraryPaths.push(destination)
  }
  await downloadAll(tasks, 8, (completed, total, label) => onProgress('fabric-libraries', completed, total, label), signal)

  // Fabric Loader reads mods from <gameDir>/mods at startup. Created unconditionally here so the
  // directory exists even on a checkout without launcher/mods-bundle populated yet; the actual
  // jars (own mod + Sodium/Lithium + Fabric API) get copied in by bundleSync.syncBundledContent()
  // right after this — a full in-launcher mod manager (add/remove/toggle via UI) is still Phase 6.
  await mkdir(join(vanilla.instanceDir, 'game', 'mods'), { recursive: true })

  return {
    ...vanilla,
    detail: {
      ...vanilla.detail,
      id: profile.id,
      mainClass: profile.mainClass,
      arguments: vanilla.detail.arguments
        ? {
            ...vanilla.detail.arguments,
            jvm: [...vanilla.detail.arguments.jvm, ...(profile.arguments?.jvm ?? [])]
          }
        : undefined
    },
    libraryPaths: [...withoutArtifactsOf(vanilla.libraryPaths, libraryPaths), ...libraryPaths]
  }
}

/** A library's folder without its version: `<libraries>/org/ow2/asm/asm`. */
function artifactDir(libraryPath: string): string {
  return dirname(dirname(libraryPath))
}

/** The game's own libraries minus those Fabric brings in a version of its own. 1.21.9 and 1.21.10
 * ship ASM themselves (9.6, next to Fabric's newer one), and Fabric Loader refuses to start with
 * two copies on the classpath: "duplicate ASM classes found". */
function withoutArtifactsOf(vanillaPaths: string[], fabricPaths: string[]): string[] {
  const fabricArtifacts = new Set(fabricPaths.map(artifactDir))
  return vanillaPaths.filter((path) => !fabricArtifacts.has(artifactDir(path)))
}
