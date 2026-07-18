import { join } from 'node:path'
import type { MinecraftProfile } from '../../shared/types'
import { matchesRules } from './classpath'
import type { ConditionalArgument, VersionDetail } from './versionManifest'

export interface LaunchContext {
  detail: VersionDetail
  instanceDir: string
  classpath: string
  profile: MinecraftProfile
}

function resolvePlaceholders(value: string, vars: Record<string, string>): string {
  return value.replace(/\$\{(\w+)\}/g, (match, key: string) => vars[key] ?? match)
}

function flattenArguments(
  entries: Array<string | ConditionalArgument> | undefined,
  vars: Record<string, string>
): string[] {
  if (!entries) return []
  const result: string[] = []
  for (const entry of entries) {
    if (typeof entry === 'string') {
      result.push(resolvePlaceholders(entry, vars))
      continue
    }
    if (!matchesRules(entry.rules)) continue
    const values = Array.isArray(entry.value) ? entry.value : [entry.value]
    for (const value of values) result.push(resolvePlaceholders(value, vars))
  }
  return result
}

export function buildLaunchArgs(context: LaunchContext): string[] {
  const { detail, instanceDir, classpath, profile } = context
  const vars: Record<string, string> = {
    auth_player_name: profile.name,
    version_name: detail.id,
    game_directory: join(instanceDir, 'game'),
    assets_root: join(instanceDir, 'assets'),
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
    classpath
  }

  const jvmArgs = flattenArguments(detail.arguments?.jvm, vars)
  const gameArgs = flattenArguments(detail.arguments?.game, vars)

  if (jvmArgs.length === 0) {
    // Fallback for the (unexpected, for 1.21.11) case of a version JSON without a modern
    // `arguments.jvm` block.
    jvmArgs.push('-cp', classpath)
  }

  return [...jvmArgs, detail.mainClass, ...gameArgs]
}
