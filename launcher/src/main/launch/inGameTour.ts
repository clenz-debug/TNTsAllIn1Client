import { mkdir, rm, writeFile } from 'node:fs/promises'
import { join } from 'node:path'

/** Read (and deleted) by our mod's `InGameTour` on its next start - same config-folder handoff as
 * `clientDesignSync.ts`. */
function tourFile(gameDir: string): string {
  return join(gameDir, 'config', 'tntsallin1client-tour.json')
}

/**
 * The launcher tour's last step can hand over to a tour through the game's own menus (own user
 * request). Written right before that launch only; every other launch removes a leftover request
 * (e.g. from a game that crashed before the mod read it), so the in-game tour never starts unasked.
 */
export async function writeInGameTourRequest(gameDir: string, start: boolean): Promise<void> {
  if (start) {
    await mkdir(join(gameDir, 'config'), { recursive: true })
    await writeFile(tourFile(gameDir), JSON.stringify({ start: true }, null, 2))
  } else {
    await rm(tourFile(gameDir), { force: true })
  }
}
