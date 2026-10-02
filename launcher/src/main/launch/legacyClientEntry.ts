import type { LaunchStage } from '../../shared/types'
import { libraryDestinationPath } from './classpath'
import { downloadAll, type DownloadTask } from './downloader'
import type { InstalledVersion } from './installer'

/** Mojang's own library server - LaunchWrapper is Mojang's, and ASM is hosted there for it. */
const MOJANG_LIBRARIES = 'https://libraries.minecraft.net/'

/** What our entry needs on the classpath next to the game: LaunchWrapper to start through, and ASM,
 * which Mixin (inside our own jar) rewrites classes with. Everything else both use (Guava, Gson,
 * log4j, jopt-simple) the game brings itself. */
const ENTRY_LIBRARIES: readonly { path: string; sha1: string }[] = [
  { path: 'net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar', sha1: '111e7bea9c968cdb3d06ef4632bf7ff0824d0f36' },
  { path: 'org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar', sha1: '4333508b8dd8ee72aa4e39afa713b3a74579b773' }
]

const LAUNCHWRAPPER_MAIN_CLASS = 'net.minecraft.launchwrapper.Launch'
/** `mod/<legacy version>/src/main/java/com/tntsallin1client/launch/ClientTweaker.java`. */
const TWEAK_CLASS = 'com.tntsallin1client.launch.ClientTweaker'

export type InstallProgressCallback = (
  stage: LaunchStage,
  completed: number,
  total: number,
  label?: string
) => void

/**
 * Our own way into a legacy version (before 1.14, no Fabric - see `shared/legacyVersions.ts`):
 * instead of a mod loader finding our mod in `mods/`, the game is started through Mojang's
 * LaunchWrapper with our jar on the classpath and its tweaker named as `--tweakClass`. The tweaker
 * switches Mixin on, from there it works like the Fabric mods. Only loads our own mod - there is
 * no mods folder and nothing for third-party mods.
 *
 * So far a feasibility test: only `mod/1.8.9/` exists, and it draws one line of text on the title
 * screen. A legacy version without a built jar just starts as plain Minecraft.
 */
export async function installLegacyClientEntry(
  vanilla: InstalledVersion,
  ownModJarPath: string,
  onProgress: InstallProgressCallback,
  signal?: AbortSignal
): Promise<InstalledVersion> {
  const tasks: DownloadTask[] = ENTRY_LIBRARIES.map((library) => ({
    url: `${MOJANG_LIBRARIES}${library.path}`,
    destination: libraryDestinationPath(library.path),
    sha1: library.sha1
  }))
  await downloadAll(tasks, 2, (completed, total, label) => onProgress('libraries', completed, total, label), signal)

  // 1.13 switched from one argument string to the `arguments` lists - the tweak class goes
  // wherever this version keeps its game arguments.
  const { detail } = vanilla
  const gameArguments = detail.arguments
    ? { arguments: { ...detail.arguments, game: [...detail.arguments.game, '--tweakClass', TWEAK_CLASS] } }
    : { minecraftArguments: `${detail.minecraftArguments ?? ''} --tweakClass ${TWEAK_CLASS}` }

  return {
    ...vanilla,
    detail: { ...detail, mainClass: LAUNCHWRAPPER_MAIN_CLASS, ...gameArguments },
    libraryPaths: [ownModJarPath, ...tasks.map((task) => task.destination), ...vanilla.libraryPaths]
  }
}
