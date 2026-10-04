import { join } from 'node:path'
import { pathToFileURL } from 'node:url'
import type { MinecraftProfile } from '../../shared/types'
import { matchesRules } from './classpath'
import type { ConditionalArgument, VersionDetail } from './versionManifest'

export interface LaunchContext {
  detail: VersionDetail
  instanceDir: string
  /** Shared `assets/` root (`installer.ts#InstalledVersion.assetsDir`) - not `instanceDir`-relative
   * any more, see that file's doc comment. */
  assetsDir: string
  classpath: string
  profile: MinecraftProfile
  /** `LauncherSettings.maxMemoryMb` - `null` passes no `-Xmx` at all (today's behavior, whatever
   * the JVM's own default heap is). */
  maxMemoryMb: number | null
  /** Friends "join" (Phase 8): start straight into this server (`host` or `host:port`). */
  quickPlayMultiplayer?: string
  /** The replacement log4j configuration for a version released before 1.18.1 (see `legacyLogging.ts`). */
  legacyLogConfigPath?: string
}

function resolvePlaceholders(value: string, vars: Record<string, string>): string {
  return value.replace(/\$\{(\w+)\}/g, (match, key: string) => vars[key] ?? match)
}

function flattenArguments(
  entries: Array<string | ConditionalArgument> | undefined,
  vars: Record<string, string>,
  features: Record<string, boolean> = {}
): string[] {
  if (!entries) return []
  const result: string[] = []
  for (const entry of entries) {
    if (typeof entry === 'string') {
      result.push(resolvePlaceholders(entry, vars))
      continue
    }
    if (!matchesRules(entry.rules, features)) continue
    const values = Array.isArray(entry.value) ? entry.value : [entry.value]
    for (const value of values) result.push(resolvePlaceholders(value, vars))
  }
  return result
}

/**
 * Versions before 1.13 list no JVM arguments in their version JSON - Mojang's launcher supplies
 * these itself. Without `java.library.path` LWJGL 2 doesn't find its DLLs (see
 * `nativesExtractor.ts`); the heap dump path is the same Intel driver workaround the modern version
 * JSONs carry for Windows.
 */
function legacyJvmArgs(vars: Record<string, string>): string[] {
  return [
    ...(process.platform === 'win32'
      ? ['-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump']
      : []),
    `-Djava.library.path=${vars.natives_directory}`,
    '-cp',
    vars.classpath
  ]
}

export function buildLaunchArgs(context: LaunchContext): string[] {
  const { detail, instanceDir, assetsDir, classpath, profile, maxMemoryMb, quickPlayMultiplayer, legacyLogConfigPath } = context
  const vars: Record<string, string> = {
    auth_player_name: profile.name,
    version_name: detail.id,
    game_directory: join(instanceDir, 'game'),
    assets_root: assetsDir,
    assets_index_name: detail.assetIndex.id,
    auth_uuid: profile.id,
    auth_access_token: profile.accessToken,
    clientid: '',
    auth_xuid: '',
    user_type: 'msa',
    // Only in the old `minecraftArguments` (1.7.6 - 1.12.2): the profile's properties as JSON.
    user_properties: '{}',
    version_type: 'release',
    natives_directory: join(instanceDir, 'natives'),
    launcher_name: 'TNTsAllIn1ClientLauncher',
    launcher_version: '0.1.0',
    classpath,
    quickPlayMultiplayer: quickPlayMultiplayer ?? ''
  }
  const features = { is_quick_play_multiplayer: !!quickPlayMultiplayer }

  const jvmArgs = flattenArguments(detail.arguments?.jvm, vars)
  // Placeholders are resolved per argument, after splitting - a game directory with a space in its
  // path stays one argument that way.
  const gameArgs = detail.arguments
    ? flattenArguments(detail.arguments.game, vars, features)
    : (detail.minecraftArguments ?? '')
        .split(' ')
        .filter((argument) => argument.length > 0)
        .map((argument) => resolvePlaceholders(argument, vars))
  // Versions before Quick Play (1.20) take the older --server/--port pair instead.
  const hasQuickPlay = JSON.stringify(detail.arguments?.game ?? []).includes('quickPlayMultiplayer')
  if (quickPlayMultiplayer && !hasQuickPlay) {
    const [host, port] = quickPlayMultiplayer.split(':')
    gameArgs.push('--server', host, '--port', port || '25565')
  }

  if (jvmArgs.length === 0) {
    jvmArgs.push(...legacyJvmArgs(vars))
  }
  // As a file: URI, not a plain path - log4j 2.0-beta9 otherwise first tries the Windows path as a
  // classpath resource and prints a stack trace ("This may be innocuous") on every start. It
  // URL-decodes the URI, which would turn a literal "+" in the path into a space.
  if (legacyLogConfigPath) {
    jvmArgs.unshift(`-Dlog4j.configurationFile=${pathToFileURL(legacyLogConfigPath).href.replace(/\+/g, '%2B')}`)
    // Mojang's own switch for 1.17 and 1.18 (log4j 2.10 and later know it) - on top of the filter, costs nothing elsewhere.
    jvmArgs.unshift('-Dlog4j2.formatMsgNoLookups=true')
  }

  const memoryArgs = maxMemoryMb != null ? [`-Xmx${maxMemoryMb}M`] : []

  return [...memoryArgs, ...jvmArgs, detail.mainClass, ...gameArgs]
}
