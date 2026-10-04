import { mkdir, rename, rm, writeFile } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { openPromise } from 'yauzl'
import { decodePng, encodePng, type RgbaImage } from '../pngCodec'
import { readPngDimensions } from '../pngUtils'
import type { NewTexturesRules } from './newTexturesRules'

const TEXTURES = 'assets/minecraft/textures/'
/** Resource pack format of 1.6.1 to 1.8.9. */
const PACK_FORMAT = 1
/** Says what the pack in an instance was built from - see `newTexturesPack.ts`. */
export const PACK_MARKER_FILE = 'tnt-new-textures.json'

export interface PackMarker {
  /** The Minecraft version the textures were taken from. */
  source: string
  /** {@link NewTexturesRules.revision} at the time. */
  revision: number
}

export interface BuildResult {
  /** Textures the pack replaces. */
  replaced: number
  /** Textures the rules name a replacement for that could not be used - gone, or another shape by now. */
  skipped: string[]
}

/** Every file of a jar whose name `wanted` accepts, read into memory. */
export async function readEntries(jarPath: string, wanted: (name: string) => boolean): Promise<Map<string, Buffer>> {
  const entries = new Map<string, Buffer>()
  const zipfile = await openPromise(jarPath, { lazyEntries: true, autoClose: true })
  try {
    for await (const entry of zipfile.eachEntry()) {
      if (entry.fileName.endsWith('/') || !wanted(entry.fileName)) continue
      const chunks: Buffer[] = []
      for await (const chunk of await zipfile.openReadStreamPromise(entry)) {
        chunks.push(chunk as Buffer)
      }
      entries.set(entry.fileName, Buffer.concat(chunks))
    }
  } finally {
    zipfile.close()
  }
  return entries
}

/** The `animation` part of a texture's `.mcmeta`, if it has one. Newer versions keep other things in that file too, which the old game has no use for. */
function animationOf(mcmeta: Buffer | undefined): unknown {
  if (!mcmeta) return undefined
  try {
    return (JSON.parse(mcmeta.toString('utf8')) as { animation?: unknown }).animation
  } catch {
    return undefined
  }
}

/** Pictures of one size put together in a grid, row by row. Null if they aren't all the same size. */
function arrange(cells: RgbaImage[], columns: number): RgbaImage | null {
  const { width, height } = cells[0]
  if (cells.some((cell) => cell.width !== width || cell.height !== height)) return null
  const rows = Math.ceil(cells.length / columns)
  const data = new Uint8Array(width * columns * height * rows * 4)
  cells.forEach((cell, index) => {
    const left = (index % columns) * width
    const top = Math.floor(index / columns) * height
    for (let y = 0; y < height; y++) {
      data.set(cell.data.subarray(y * width * 4, (y + 1) * width * 4), ((top + y) * width * columns + left) * 4)
    }
  })
  return { width: width * columns, height: height * rows, data }
}

/** Multiplies every pixel's color with `color` (0xRRGGBB), leaving its alpha. */
function tint(image: RgbaImage, color: number): RgbaImage {
  const factors = [(color >> 16) & 0xff, (color >> 8) & 0xff, color & 0xff]
  const data = new Uint8Array(image.data)
  for (let index = 0; index < data.length; index += 4) {
    for (let channel = 0; channel < 3; channel++) {
      data[index + channel] = Math.round((data[index + channel] * factors[channel]) / 255)
    }
  }
  return { ...image, data }
}

/**
 * Builds the "New Textures" resource pack for a legacy version: for every texture of the old game
 * that the rules know a current counterpart for, that counterpart under the old name. Both games'
 * files are the player's own (`oldJarPath`, `newJarPath` are client jars as Mojang ships them) -
 * nothing of the pack's pictures comes from us.
 *
 * Only textures the old game has are written, and only where the new picture still fits: same size
 * for what is laid out for a model, one square frame (or an animation) for blocks and items. What
 * doesn't fit is left out, so the game keeps its own texture there.
 *
 * The pack is a folder (the old game reads those like zip files), built next to its final place
 * and moved there at the end - a build that fails halfway leaves the previous pack as it was.
 */
export async function buildNewTexturesPack(
  oldJarPath: string,
  newJarPath: string,
  rules: NewTexturesRules,
  packDir: string,
  marker: PackMarker
): Promise<BuildResult> {
  const oldFiles = await readEntries(oldJarPath, (name) => name.startsWith(TEXTURES) && name.endsWith('.png'))
  const oldTextures = [...oldFiles.keys()].map((name) => name.slice(TEXTURES.length, -'.png'.length))

  /** The current texture standing in for an old one, by the rules alone. */
  const targetOf = (oldPath: string): string | undefined => {
    if (rules.renames[oldPath]) return rules.renames[oldPath]
    const slash = oldPath.lastIndexOf('/')
    const newFolder = rules.folders[oldPath.slice(0, slash)]
    return newFolder ? `${newFolder}${oldPath.slice(slash)}` : undefined
  }

  const wanted = new Set<string>()
  for (const oldPath of oldTextures) {
    const sources = rules.strips[oldPath] ?? rules.sheets[oldPath]?.cells ?? [targetOf(oldPath)]
    for (const source of sources) {
      if (source) {
        wanted.add(`${TEXTURES}${source}.png`)
        wanted.add(`${TEXTURES}${source}.png.mcmeta`)
      }
    }
  }
  const newFiles = await readEntries(newJarPath, (name) => wanted.has(name))
  const newTexture = (path: string): Buffer | undefined => newFiles.get(`${TEXTURES}${path}.png`)

  const output = new Map<string, Buffer>()
  const skipped: string[] = []
  let replaced = 0
  for (const oldPath of oldTextures) {
    const oldSize = readPngDimensions(oldFiles.get(`${TEXTURES}${oldPath}.png`) as Buffer)
    const destination = `${TEXTURES}${oldPath}.png`

    const pieces = rules.strips[oldPath] ?? rules.sheets[oldPath]?.cells
    if (pieces) {
      // One column for a strip of frames.
      const columns = rules.sheets[oldPath]?.columns ?? 1
      const buffers = pieces.map(newTexture)
      let image: RgbaImage | null = null
      if (buffers.every((buffer) => buffer !== undefined)) {
        try {
          image = arrange(buffers.map((buffer) => decodePng(buffer as Buffer)), columns)
        } catch {
          image = null
        }
      }
      // The old game expects exactly the picture it has: the same number of frames, the same grid.
      if (!image || !oldSize || image.width !== oldSize.width || image.height !== oldSize.height) {
        skipped.push(oldPath)
        continue
      }
      output.set(destination, encodePng(image))
      if (rules.strips[oldPath]) {
        output.set(`${destination}.mcmeta`, Buffer.from(JSON.stringify({ animation: {} }), 'utf8'))
      }
      replaced++
      continue
    }

    const target = targetOf(oldPath)
    if (!target) continue
    const picture = newTexture(target)
    const size = picture ? readPngDimensions(picture) : null
    if (!picture || !size || !oldSize) {
      skipped.push(oldPath)
      continue
    }
    const animation = animationOf(newFiles.get(`${TEXTURES}${target}.png.mcmeta`))
    const fits = rules.sameSizeOnly.some((prefix) => oldPath.startsWith(prefix))
      ? size.width === oldSize.width && size.height === oldSize.height
      : animation !== undefined || size.width === size.height
    if (!fits) {
      skipped.push(oldPath)
      continue
    }

    // Always written anew as plain RGBA: current textures are stored in whatever PNG variant is
    // smallest (gray, palette), and the old game's image reader shows some of those too bright.
    try {
      const image = decodePng(picture)
      const color = rules.tinted[oldPath]
      output.set(destination, encodePng(color === undefined ? image : tint(image, color)))
    } catch {
      skipped.push(oldPath)
      continue
    }
    if (animation !== undefined) {
      output.set(`${destination}.mcmeta`, Buffer.from(JSON.stringify({ animation }), 'utf8'))
    }
    replaced++
  }

  const packMeta = {
    pack: {
      pack_format: PACK_FORMAT,
      description: `Minecraft ${marker.source} textures - built by TNT's All-In-1 Client from your own game files`
    }
  }
  output.set('pack.mcmeta', Buffer.from(JSON.stringify(packMeta, null, 2), 'utf8'))
  output.set(PACK_MARKER_FILE, Buffer.from(JSON.stringify(marker), 'utf8'))

  const buildDir = `${packDir}.building`
  await rm(buildDir, { recursive: true, force: true })
  for (const [name, content] of output) {
    const file = join(buildDir, name)
    await mkdir(dirname(file), { recursive: true })
    await writeFile(file, content)
  }
  await rm(packDir, { recursive: true, force: true })
  await rename(buildDir, packDir)
  return { replaced, skipped }
}
