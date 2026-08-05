import { app } from 'electron'
import { copyFile, mkdir } from 'node:fs/promises'
import { join } from 'node:path'

function sharedOptionsPath(): string {
  return join(app.getPath('userData'), 'shared-settings', 'options.txt')
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
 * Scope deliberately narrow: just vanilla's `options.txt`. Not `servers.dat` (multiplayer list -
 * arguably also worth sharing, but the user's ask was specifically "Einstellungen"/settings) and
 * not mod-specific config files (Sodium's own options, etc.) - those only apply to 1.21.11 anyway
 * since that's the only bundle-compatible version.
 */
export async function applySharedOptions(gameDir: string): Promise<void> {
  try {
    await mkdir(gameDir, { recursive: true })
    await copyFile(sharedOptionsPath(), join(gameDir, 'options.txt'))
  } catch {
    // No shared options yet (very first launch ever) - the instance just keeps Minecraft's own
    // built-in defaults, and whatever it writes becomes the shared baseline once this session ends.
  }
}

/** Copies whatever the just-finished session wrote back out to the shared location, so the next
 * launch - any version - picks up anything the player changed in-game. */
export async function saveSharedOptions(gameDir: string): Promise<void> {
  try {
    const destination = sharedOptionsPath()
    await mkdir(join(app.getPath('userData'), 'shared-settings'), { recursive: true })
    await copyFile(join(gameDir, 'options.txt'), destination)
  } catch {
    // No options.txt to copy back (e.g. the game never got far enough to write one) - nothing to do.
  }
}
