import { readdir, stat, statfs } from 'node:fs/promises'
import { join } from 'node:path'
import { config } from './config.js'

/**
 * The machine belongs to someone else and runs other services - whatever players upload must never
 * be able to fill its disk. Two stops in front of everything that stores a file a player caused
 * (a cape, the copy of a reported cape): the disk keeps a reserve free, and each of our folders has
 * a ceiling of its own.
 */

/** Folder sizes are counted at most this often - an upload is rate-limited far below that anyway. */
const SIZE_CACHE_MS = 60 * 1000
const sizes = new Map<string, { bytes: number; counted: number }>()

async function folderBytes(dir: string): Promise<number> {
  const cached = sizes.get(dir)
  if (cached && Date.now() - cached.counted < SIZE_CACHE_MS) return cached.bytes
  let bytes = 0
  for (const name of await readdir(dir).catch(() => [] as string[])) {
    bytes += await stat(join(dir, name)).then(
      (info) => (info.isFile() ? info.size : 0),
      () => 0
    )
  }
  sizes.set(dir, { bytes, counted: Date.now() })
  return bytes
}

/**
 * Whether `bytes` more may be written into `dir`, which may hold `maxFolderBytes` at most. Counts a
 * file that replaces another one as new - erring towards refusing a little early.
 */
export async function hasRoomFor(dir: string, bytes: number, maxFolderBytes: number): Promise<boolean> {
  const disk = await statfs(dir).catch(() => null)
  // Space an unprivileged user may actually use
  if (disk && disk.bavail * disk.bsize - bytes < config.minFreeBytes) return false
  const used = await folderBytes(dir)
  if (used + bytes > maxFolderBytes) return false
  // Counted towards the cached size right away, so a burst within one cache period cannot overshoot
  sizes.set(dir, { bytes: used + bytes, counted: sizes.get(dir)?.counted ?? Date.now() })
  return true
}
