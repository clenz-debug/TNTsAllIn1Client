import { mkdir, readFile, rename, rm, writeFile } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import type { GameLogEvent } from '../../shared/types'
import { decodePng, encodePng, type RgbaImage } from '../pngCodec'
import type { InstallProgressCallback } from './installer'
import { readEntries } from './newTexturesBuilder'

/**
 * "TNT Dark Mode" for the legacy versions: dark inventories, buttons and hotbar, the same look as
 * the dark mode pack of the Fabric versions (`resourcepacks/dark-mode/build.py`) - same palettes,
 * same rule that only neutral greys change, so everything with a color of its own keeps reading the
 * way it does in the game.
 *
 * Built here, in the instance, from the player's own client jar, like the "New Textures" pack
 * (`newTexturesPack.ts`): the launcher's bundle sync does not run for versions without a mod loader,
 * and this way none of Mojang's pictures travel with the launcher. Our mod switches the pack on and
 * off (`DarkMode` in `mod/1.8.9`) and draws the screens' labels in {@link LABEL_COLOR} meanwhile -
 * the game's own dark grey for them is nothing a resource pack can change.
 *
 * The old game keeps what the newer ones have as single sprites in a few big sheets (buttons and
 * hotbar in `widgets.png`, a furnace's flame and arrow beside its panel), so the rules here work on
 * parts of a picture where `build.py` works on whole files.
 */

/** The pack's folder name in an instance's `resourcepacks/` - `DarkMode.PACK_NAME` in `mod/1.8.9`. */
export const LEGACY_DARK_MODE_PACK_NAME = 'TNT Dark Mode'
const PACK_MARKER_FILE = 'tnt-dark-mode.json'
/** Resource pack format of 1.6.1 to 1.8.9. */
const PACK_FORMAT = 1
const TEXTURES = 'assets/minecraft/textures/'

/** The color the mod draws container labels in while the pack is active (the game's own: #404040). */
const LABEL_COLOR = '#D0D0D0'
/** How far apart a pixel's red, green and blue may be to still count as neutral grey. */
const MAX_CHROMA = 10

type Palette = [number, number][]
type LevelMap = (level: number) => number

// The palettes of build.py - keep the two in step.
// Panels: 0x00 outlines, 0x37 a slot's shadow edge, 0x55 a panel's shadow edge, 0x8B slots, 0xC6 panels, 0xFF highlights
const PALETTE: Palette = [[0x00, 0x00], [0x37, 0x0e], [0x55, 0x16], [0x8b, 0x1a], [0xc6, 0x2b], [0xff, 0x4a]]
// Buttons and sliders sit on the world rather than on a panel; the white frame of a hovered button stays white.
const WIDGET_PALETTE: Palette = [[0x00, 0x00], [0x2c, 0x1a], [0x56, 0x2a], [0x6f, 0x3a], [0xaa, 0x5c], [0xff, 0xff]]
// The hotbar: the buttons' greys without their jump to white at the top.
const HOTBAR_PALETTE: Palette = [...WIDGET_PALETTE.slice(0, -1), [0xff, 0x7a]]
// Empty-slot pictures are darker than the slot in the game; on a dark slot they have to be lighter than it.
const SILHOUETTE_LIGHTEST = 0x58
const SILHOUETTE_DARKEST = 0x3a

/** A palette as a table: index = the game's grey level, value = the dark grey level, straight lines in between. */
function curve(palette: Palette): LevelMap {
  const table: number[] = []
  for (let level = 0; level < 256; level++) {
    for (let index = 0; index + 1 < palette.length; index++) {
      const [fromLow, toLow] = palette[index]
      const [fromHigh, toHigh] = palette[index + 1]
      if (level >= fromLow && level <= fromHigh) {
        table.push(Math.round(toLow + ((toHigh - toLow) * (level - fromLow)) / (fromHigh - fromLow)))
        break
      }
    }
  }
  return (level) => table[level]
}

const PANEL = curve(PALETTE)
const WIDGET = curve(WIDGET_PALETTE)
const HOTBAR = curve(HOTBAR_PALETTE)
const SILHOUETTE: LevelMap = (level) => Math.round(SILHOUETTE_LIGHTEST - ((SILHOUETTE_LIGHTEST - SILHOUETTE_DARKEST) * level) / 255)

interface Region {
  /** Left, top, right, bottom - the last two exclusive. */
  box: [number, number, number, number]
  /** Null leaves the region as it is. */
  map: LevelMap | null
  /** For a region that is tinted rather than grey: every pixel is darkened by as much as a grey of its brightness would be, keeping its tint. */
  tinted?: boolean
}

interface PictureRule {
  /** Matched against the path below `textures/`, without `.png`; a trailing `*` matches any rest. */
  path: string
  map: LevelMap
  /** Parts of the picture treated differently; the first one a pixel lies in counts. */
  regions?: Region[]
  /** Single colors (0xRRGGBB) swapped as they are - for a bright patch that is no neutral grey. */
  swaps?: Record<number, number>
}

/** First match wins. */
const RULES_1_8_9: PictureRule[] = [
  // The flame and the progress arrow lie right of the panel and are meant to stand out - white stays white.
  { path: 'gui/container/furnace', map: PANEL, regions: [{ box: [176, 0, 256, 256], map: null }] },
  // The same for the bubbles and the brewing arrow.
  { path: 'gui/container/brewing_stand', map: PANEL, regions: [{ box: [176, 0, 256, 256], map: null }] },
  // Below the panel and the effect box: the effect icons, little pictures drawn partly in greys.
  { path: 'gui/container/inventory', map: PANEL, regions: [{ box: [0, 198, 256, 256], map: null }] },
  // The "destroy item" slot has a reddish fill that would stay as bright as it is - a dark red, as in build.py.
  { path: 'gui/container/creative_inventory/tab_inventory', map: PANEL, swaps: { 0xab7f7f: 0x4a2424 } },
  { path: 'gui/container/*', map: PANEL },
  { path: 'gui/achievement/achievement_background', map: PANEL },
  {
    path: 'gui/widgets',
    map: WIDGET,
    regions: [
      // The white frame around the selected hotbar slot.
      { box: [0, 22, 24, 46], map: null },
      // The hotbar's frame; its half see-through inside is no opaque grey and stays anyway.
      { box: [0, 0, 182, 22], map: HOTBAR },
      // A button under the cursor is bluish in this version.
      { box: [0, 86, 200, 106], map: WIDGET, tinted: true }
    ]
  },
  { path: 'gui/spectator_widgets', map: HOTBAR, regions: [{ box: [0, 22, 24, 46], map: null }] },
  // The helmet, chestplate, ... outlines in the empty armor slots.
  { path: 'items/empty_armor_slot_*', map: SILHOUETTE }
]

interface LegacyDarkModeRules {
  /** Raised whenever the rules or palettes change, so existing packs get rebuilt. */
  revision: number
  pictures: PictureRule[]
}

const RULES: Record<string, LegacyDarkModeRules> = { '1.8.9': { revision: 1, pictures: RULES_1_8_9 } }

function ruleFor(rules: PictureRule[], path: string): PictureRule | undefined {
  return rules.find((rule) => (rule.path.endsWith('*') ? path.startsWith(rule.path.slice(0, -1)) : path === rule.path))
}

/** The picture with its greys darkened by the rule - null if no pixel changed. */
function recolor(image: RgbaImage, rule: PictureRule): RgbaImage | null {
  const data = new Uint8Array(image.data)
  let changed = false
  for (let y = 0; y < image.height; y++) {
    for (let x = 0; x < image.width; x++) {
      const index = (y * image.width + x) * 4
      if (data[index + 3] !== 255) continue
      const region = rule.regions?.find(({ box }) => x >= box[0] && y >= box[1] && x < box[2] && y < box[3])
      const map = region ? region.map : rule.map
      if (!map) continue
      const r = data[index]
      const g = data[index + 1]
      const b = data[index + 2]
      const swap = rule.swaps?.[(r << 16) | (g << 8) | b]
      if (swap !== undefined) {
        data.set([(swap >> 16) & 0xff, (swap >> 8) & 0xff, swap & 0xff], index)
        changed = true
        continue
      }
      const level = Math.round((r + g + b) / 3)
      let next: [number, number, number]
      if (region?.tinted) {
        const factor = level === 0 ? 1 : map(level) / level
        next = [Math.round(r * factor), Math.round(g * factor), Math.round(b * factor)]
      } else {
        if (Math.max(r, g, b) - Math.min(r, g, b) > MAX_CHROMA) continue
        const dark = map(level)
        next = [dark, dark, dark]
      }
      if (next[0] !== r || next[1] !== g || next[2] !== b) {
        data.set(next, index)
        changed = true
      }
    }
  }
  return changed ? { ...image, data } : null
}

/** pack.png: a dark panel with a 3x3 grid of slots, in the pack's own palette - the same picture build.py makes. */
function packIcon(): Buffer {
  const size = 64
  const data = new Uint8Array(size * size * 4)
  const set = (x: number, y: number, level: number): void => data.set([level, level, level, 255], (y * size + x) * 4)
  for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) set(x, y, PANEL(0xc6))
  for (let i = 0; i < size; i++) {
    for (const edge of [0, 1]) {
      set(i, edge, PANEL(0xff))
      set(edge, i, PANEL(0xff))
      set(i, size - 1 - edge, PANEL(0x55))
      set(size - 1 - edge, i, PANEL(0x55))
    }
  }
  for (let row = 0; row < 3; row++) {
    for (let column = 0; column < 3; column++) {
      for (let y = 0; y < 16; y++) {
        for (let x = 0; x < 16; x++) {
          const level = x === 0 || y === 0 ? PANEL(0x37) : x === 15 || y === 15 ? PANEL(0xff) : PANEL(0x8b)
          set(8 + column * 16 + x, 8 + row * 16 + y, level)
        }
      }
    }
  }
  return encodePng({ width: size, height: size, data })
}

/** Builds the pack folder next to its final place and moves it there at the end. Returns how many pictures it recolors. */
async function buildPack(clientJarPath: string, rules: LegacyDarkModeRules, packDir: string): Promise<number> {
  const pictures = await readEntries(
    clientJarPath,
    (name) => name.startsWith(TEXTURES) && name.endsWith('.png') && ruleFor(rules.pictures, name.slice(TEXTURES.length, -'.png'.length)) !== undefined
  )
  const output = new Map<string, Buffer>()
  for (const [name, content] of pictures) {
    const rule = ruleFor(rules.pictures, name.slice(TEXTURES.length, -'.png'.length)) as PictureRule
    const recolored = recolor(decodePng(content), rule)
    if (recolored) output.set(name, encodePng(recolored))
  }
  const recoloredCount = output.size

  const packMeta = { pack: { pack_format: PACK_FORMAT, description: "§6TNT Dark Mode\n§7Dunkle Inventare für TNT's All-In-1 Client" } }
  output.set('pack.mcmeta', Buffer.from(JSON.stringify(packMeta, null, 2), 'utf8'))
  output.set('pack.png', packIcon())
  output.set('assets/tntsallin1client/dark_mode.json', Buffer.from(JSON.stringify({ label_color: LABEL_COLOR }, null, 2), 'utf8'))
  output.set(PACK_MARKER_FILE, Buffer.from(JSON.stringify({ revision: rules.revision }), 'utf8'))

  const buildDir = `${packDir}.building`
  await rm(buildDir, { recursive: true, force: true })
  for (const [name, content] of output) {
    const file = join(buildDir, name)
    await mkdir(dirname(file), { recursive: true })
    await writeFile(file, content)
  }
  await rm(packDir, { recursive: true, force: true })
  await rename(buildDir, packDir)
  return recoloredCount
}

/**
 * Makes sure a legacy instance has the "TNT Dark Mode" resource pack. Called on every start of a
 * legacy version our mod exists for; does nothing once the pack is there and up to date.
 *
 * Never fails a launch - with a file it can't read, the game just starts without the pack (or with
 * the one it had) and the reason goes to the log.
 */
export async function ensureLegacyDarkModePack(
  gameDir: string,
  legacyVersionId: string,
  legacyClientJarPath: string,
  onProgress: InstallProgressCallback,
  log: (event: GameLogEvent) => void
): Promise<void> {
  const rules = RULES[legacyVersionId]
  if (!rules) return

  const packDir = join(gameDir, 'resourcepacks', LEGACY_DARK_MODE_PACK_NAME)
  try {
    try {
      const marker = JSON.parse(await readFile(join(packDir, PACK_MARKER_FILE), 'utf8')) as { revision?: number }
      if (marker.revision === rules.revision) return
    } catch {
      // No pack yet, or not one of ours - build it.
    }
    onProgress('bundles', 0, 1, LEGACY_DARK_MODE_PACK_NAME)
    const recolored = await buildPack(legacyClientJarPath, rules, packDir)
    onProgress('bundles', 1, 1, LEGACY_DARK_MODE_PACK_NAME)
    log({ source: 'launcher', level: 'info', message: `Paket „TNT Dark Mode“ gebaut: ${recolored} Bilder abgedunkelt.` })
  } catch (err) {
    log({
      source: 'launcher',
      level: 'error',
      message: `Paket „TNT Dark Mode“ konnte nicht gebaut werden (${err instanceof Error ? err.message : String(err)}) - das Spiel startet ohne bzw. mit dem bisherigen Paket.`
    })
  }
}

/** For building the pack outside the launcher (a preview while tuning the rules). */
export async function buildLegacyDarkModePackForPreview(versionId: string, clientJarPath: string, packDir: string): Promise<number> {
  return buildPack(clientJarPath, RULES[versionId], packDir)
}
