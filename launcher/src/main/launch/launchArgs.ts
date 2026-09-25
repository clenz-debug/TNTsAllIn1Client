import { join } from 'node:path'
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

export function buildLaunchArgs(context: LaunchContext): string[] {
  const { detail, instanceDir, assetsDir, classpath, profile, maxMemoryMb, quickPlayMultiplayer } = context
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
    version_type: 'release',
    natives_directory: join(instanceDir, 'natives'),
    launcher_name: 'TNTsAllIn1ClientLauncher',
    launcher_version: '0.1.0',
    classpath,
    quickPlayMultiplayer: quickPlayMultiplayer ?? ''
  }
  const features = { is_quick_play_multiplayer: !!quickPlayMultiplayer }

  const jvmArgs = flattenArguments(detail.arguments?.jvm, vars)
  const gameArgs = flattenArguments(detail.arguments?.game, vars, features)
  // Versions before Quick Play (1.20) take the older --server/--port pair instead.
  const hasQuickPlay = JSON.stringify(detail.arguments?.game ?? []).includes('quickPlayMultiplayer')
  if (quickPlayMultiplayer && !hasQuickPlay) {
    const [host, port] = quickPlayMultiplayer.split(':')
    gameArgs.push('--server', host, '--port', port || '25565')
  }

  if (jvmArgs.length === 0) {
    // Fallback for the (unexpected, for 1.21.11) case of a version JSON without a modern
    // `arguments.jvm` block.
    jvmArgs.push('-cp', classpath)
  }

  const memoryArgs = maxMemoryMb != null ? [`-Xmx${maxMemoryMb}M`] : []

  return [...memoryArgs, ...jvmArgs, detail.mainClass, ...gameArgs]
}
