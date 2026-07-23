import { mkdir } from 'node:fs/promises'
import { join } from 'node:path'
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
  onProgress: InstallProgressCallback
): Promise<InstalledVersion> {
  onProgress('fabric-meta', 0, 1, vanilla.detail.id)
  const loaderVersion = await fetchLatestStableLoaderVersion(vanilla.detail.id)
  const profile = await fetchFabricProfile(vanilla.detail.id, loaderVersion)
  onProgress('fabric-meta', 1, 1, profile.id)

  const tasks: DownloadTask[] = []
  const libraryPaths: string[] = []
  for (const lib of profile.libraries) {
    const relativePath = mavenCoordinateToPath(lib.name)
    const destination = libraryDestinationPath(vanilla.instanceDir, relativePath)
    tasks.push({ url: `${lib.url}${relativePath}`, destination, sha1: lib.sha1 })
    libraryPaths.push(destination)
  }
  await downloadAll(tasks, 8, (completed, total, label) => onProgress('fabric-libraries', completed, total, label))

  // Fabric Loader reads mods from <gameDir>/mods at startup. For now that folder is filled by
  // hand (own mod jar + Sodium/Lithium + Fabric API) — see Aktuelle_Phase.md; an in-launcher mod
  // manager is Phase 6.
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
    libraryPaths: [...vanilla.libraryPaths, ...libraryPaths]
  }
}
