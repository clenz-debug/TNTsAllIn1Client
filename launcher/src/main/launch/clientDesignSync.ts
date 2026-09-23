import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { DEFAULT_THEME_COLORS, type ClientDesign, type LauncherSettings } from '../../shared/types'
import { loadLauncherSettings, saveLauncherSettings } from '../launcherSettings'

/** Both files live in Fabric's config dir (`game/config/`), read by our mod's `ClientDesign` /
 * `ClientTheme` classes - same "launcher writes a small JSON next to the mod's own config" handoff
 * as `bundleSync.ts#writeBundledResourcepackList`. */
function designFile(gameDir: string): string {
  return join(gameDir, 'config', 'tntsallin1client-design.json')
}

function themeFile(gameDir: string): string {
  return join(gameDir, 'config', 'tntsallin1client-theme.json')
}

/**
 * Hands the game the launcher's current design choice and theme colors right before launch - the
 * mod's client design draws the title screen and mod menu in exactly these colors (own user
 * request: "launcher theme, das was der user eingestellt hat"). Theme is one-way (only ever changed
 * in the launcher), design goes both ways, see {@link readBackClientDesign}.
 */
export async function writeClientDesignFiles(gameDir: string, settings: LauncherSettings): Promise<void> {
  await mkdir(join(gameDir, 'config'), { recursive: true })
  await writeFile(designFile(gameDir), JSON.stringify({ design: settings.clientDesign }, null, 2))
  await writeFile(themeFile(gameDir), JSON.stringify(settings.themeColors ?? DEFAULT_THEME_COLORS, null, 2))
}

/** After the game exits: the title screen's logo button can switch the design in-game, so whatever
 * the mod last wrote becomes the launcher setting too (own user request: keep both in sync). Re-reads
 * settings fresh instead of using the pre-launch copy - the player may have changed other settings
 * in the launcher while the game was running. */
export async function readBackClientDesign(gameDir: string): Promise<void> {
  let design: ClientDesign
  try {
    const parsed = JSON.parse(await readFile(designFile(gameDir), 'utf-8')) as { design?: unknown }
    if (parsed.design !== 'minecraft' && parsed.design !== 'client') return
    design = parsed.design
  } catch {
    return
  }
  const settings = await loadLauncherSettings()
  if (settings.clientDesign !== design) {
    await saveLauncherSettings({ ...settings, clientDesign: design })
  }
}
