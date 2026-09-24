import type { Instance } from './types'

/**
 * Optional bundled mods that start ON but can still be turned off per instance (own user request
 * for Cape Provider: custom capes are a real client feature now, so other players should see them
 * without first hunting for a switch - but it stays the user's choice). Keyed by filename prefix,
 * not full filename, so the choice survives a version bump of the jar.
 */
export const DEFAULT_ON_BUNDLED_PREFIXES = ['cape-provider-']

export function defaultOnPrefix(fileName: string): string | null {
  return DEFAULT_ON_BUNDLED_PREFIXES.find((prefix) => fileName.startsWith(prefix)) ?? null
}

/** Whether a *toggleable* bundled mod is on for this instance - default-on mods unless opted out
 * (`disabledBundledMods`), every other one only when opted in (`enabledBundledMods`). The always-on
 * ones (Fabric API, Sodium, ...) never reach this, see `main/launch/modsManager.ts`. */
export function isBundledModEnabled(
  instance: Pick<Instance, 'enabledBundledMods' | 'disabledBundledMods'>,
  fileName: string
): boolean {
  const prefix = defaultOnPrefix(fileName)
  if (prefix) return !(instance.disabledBundledMods ?? []).includes(prefix)
  return instance.enabledBundledMods.includes(fileName)
}
