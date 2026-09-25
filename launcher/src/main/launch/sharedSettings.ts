import { app } from 'electron'
import { copyFile, mkdir, readFile, writeFile } from 'node:fs/promises'
import { join } from 'node:path'

/** Exported (not just used internally) so `clientImport.ts` can copy an external client's
 * `options.txt` straight into the same shared cache every instance already reads from/writes to -
 * see that module's own doc comment for why importing into just the new instance's own folder
 * would be silently overwritten on its first launch. */
export function sharedOptionsPath(): string {
  return join(app.getPath('userData'), 'shared-settings', 'options.txt')
}

function sharedServersPath(): string {
  return join(app.getPath('userData'), 'shared-settings', 'servers.dat')
}

/**
 * `options.txt` (graphics/controls/sound/... - everything under Minecraft's own Options menu)
 * lives one level below `instanceDir`, which has been per-version since Phase 6a - switching the
 * version dropdown means a completely different `game/` folder, so without this a player's
 * settings would silently reset every time they tried a different version. Kept as a single
 * version-independent file instead of duplicating the per-version-instance approach, since these
 * are personal preferences the player wants to follow them everywhere, not something that's ever
 * meaningfully different per version (unlike mods/resourcepacks, which genuinely are).
 *
 * Scope deliberately narrow: just vanilla's `options.txt`. Not mod-specific config files (Sodium's
 * own options, etc.) - those only apply to 1.21.11 anyway since that's the only bundle-compatible
 * version. `servers.dat` (multiplayer list) used to be excluded here too for the same "narrow
 * scope" reason, until a later, explicit user request to also share it - see
 * {@link applySharedServers}/{@link saveSharedServers} below, same pattern.
 *
 * Exception: which resource packs are switched on stays per instance ({@link PER_INSTANCE_OPTIONS}),
 * see {@link keepInstanceOptions}.
 */
export async function applySharedOptions(gameDir: string): Promise<void> {
  try {
    await mkdir(gameDir, { recursive: true })
    const shared = await readFile(sharedOptionsPath(), 'utf8')
    const own = await readFile(join(gameDir, 'options.txt'), 'utf8').catch(() => '')
    await writeFile(join(gameDir, 'options.txt'), keepInstanceOptions(shared, own), 'utf8')
  } catch {
    // No shared options yet (very first launch ever) - the instance just keeps Minecraft's own
    // built-in defaults, and whatever it writes becomes the shared baseline once this session ends.
  }
}

/** The option name of one `options.txt` line (`key_key.jump:key.keyboard.space` -> `key_key.jump`). */
function optionKey(line: string): string {
  const colon = line.indexOf(':')
  return colon < 0 ? line : line.slice(0, colon)
}

/**
 * Options that belong to the instance, not the player: the active resource packs. The pack files
 * live per instance and are often named per version (`Default-Dark-Mode-26.2-...` vs.
 * `...-1.21.11-...`), so a shared list switched a pack off in every instance that has a
 * differently named file - and that instance then wrote the shortened list back for all others
 * (own user report: dark mode gone after playing another instance).
 */
const PER_INSTANCE_OPTIONS = new Set(['resourcePacks', 'incompatibleResourcePacks'])

/** The shared options, but with the instance's own values for {@link PER_INSTANCE_OPTIONS} where it
 * has them - a brand-new instance starts from the shared (last used) pack selection. */
export function keepInstanceOptions(shared: string, own: string): string {
  const ownValues = new Map(
    own
      .split(/\r?\n/)
      .filter((line) => PER_INSTANCE_OPTIONS.has(optionKey(line)))
      .map((line) => [optionKey(line), line] as const)
  )
  const lines = shared
    .split(/\r?\n/)
    .filter((line) => line.length > 0)
    .map((line) => ownValues.get(optionKey(line)) ?? line)
  const present = new Set(lines.map(optionKey))
  for (const [key, line] of ownValues) {
    if (!present.has(key)) lines.push(line)
  }
  return lines.join('\n') + '\n'
}

/**
 * Minecraft only writes options it knows right now - a mod's key bindings vanish from the file as
 * soon as an instance without that mod saves it. Merging keeps every line of the previous shared
 * file whose option the just-finished game didn't write, so e.g. Essential's or Simple Voice
 * Chat's keys survive playing another instance (found when Essential's keys, unbound once by our
 * mod, came back on their defaults).
 */
export function mergeOptions(written: string, previous: string): string {
  const writtenLines = written.split(/\r?\n/).filter((line) => line.length > 0)
  const writtenKeys = new Set(writtenLines.map(optionKey))
  const kept = previous
    .split(/\r?\n/)
    .filter((line) => line.length > 0 && !writtenKeys.has(optionKey(line)))
  return [...writtenLines, ...kept].join('\n') + '\n'
}

/** Copies whatever the just-finished session wrote back out to the shared location, so the next
 * launch - any version - picks up anything the player changed in-game. Options only another
 * instance's mods know are kept, see {@link mergeOptions}. */
export async function saveSharedOptions(gameDir: string): Promise<void> {
  try {
    const destination = sharedOptionsPath()
    await mkdir(join(app.getPath('userData'), 'shared-settings'), { recursive: true })
    const written = await readFile(join(gameDir, 'options.txt'), 'utf8')
    const previous = await readFile(destination, 'utf8').catch(() => '')
    await writeFile(destination, mergeOptions(written, previous), 'utf8')
  } catch {
    // No options.txt to copy back (e.g. the game never got far enough to write one) - nothing to do.
  }
}

/**
 * Own user request: switching instance/version meant a previously added multiplayer server had to
 * be re-entered every time, since `servers.dat` lives right next to `options.txt` inside each
 * instance's own `game/` folder. Same version-independent shared-file approach as
 * {@link applySharedOptions} - a player's server list is a personal thing they want everywhere,
 * same reasoning as their video/sound/control settings.
 */
export async function applySharedServers(gameDir: string): Promise<void> {
  try {
    await mkdir(gameDir, { recursive: true })
    await copyFile(sharedServersPath(), join(gameDir, 'servers.dat'))
  } catch {
    // No shared server list yet (very first launch ever, or no server ever added) - nothing to
    // apply, the instance just starts with an empty multiplayer list.
  }
}

/** Copies whatever the just-finished session wrote back out to the shared location, so any server
 * added/removed/reordered in this session is there next time too - any instance, any version. */
export async function saveSharedServers(gameDir: string): Promise<void> {
  try {
    const destination = sharedServersPath()
    await mkdir(join(app.getPath('userData'), 'shared-settings'), { recursive: true })
    await copyFile(join(gameDir, 'servers.dat'), destination)
  } catch {
    // No servers.dat to copy back (e.g. the game never got far enough to write one, or the player
    // never opened the multiplayer screen) - nothing to do.
  }
}
