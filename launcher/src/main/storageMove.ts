import { copyFile, lstat, mkdir, readdir, readlink, rename, rm, stat, symlink } from 'node:fs/promises'
import { dirname, join, relative } from 'node:path'

/** Same-size check rather than a full re-hash: every file being moved here was already SHA-1
 * verified once by `launch/downloader.ts#downloadFile` when it was first downloaded, so re-hashing
 * a second time on every move would be redundant I/O, not extra safety - same trust model
 * `downloader.ts#alreadyValid` already relies on for files without a handy hash. */
async function isSameFile(destination: string, sourceSize: number): Promise<boolean> {
  try {
    return (await stat(destination)).size === sourceSize
  } catch {
    return false
  }
}

/** Moves a single file from `source` to `destination`, or - if an equally-sized file already sits
 * at `destination` (another instance already "brought" it, or a previous, interrupted run already
 * moved it) - just deletes `source` instead of overwriting. Tries an atomic same-volume `rename()`
 * first; falls back to "copy to a `.part` sibling, rename it into place, then delete the source"
 * on `EXDEV` (moving across drives, guaranteed for `changeStorageLocation`), same write discipline
 * `launch/downloader.ts#downloadFile` already uses for network writes. */
export async function moveOrSkip(source: string, destination: string): Promise<void> {
  const sourceStat = await stat(source).catch(() => null)
  if (!sourceStat) return // already moved by an earlier, interrupted run

  if (await isSameFile(destination, sourceStat.size)) {
    await rm(source, { force: true })
    return
  }

  await mkdir(dirname(destination), { recursive: true })
  try {
    await rename(source, destination)
    return
  } catch (err) {
    if ((err as NodeJS.ErrnoException).code !== 'EXDEV') throw err
  }

  const tmpPath = `${destination}.part`
  await copyFile(source, tmpPath)
  await rm(destination, { force: true })
  await rename(tmpPath, destination)
  await rm(source, { force: true })
}

/** Symlinks (e.g. the ones `auth/../launch/javaRuntime.ts#ensureJavaRuntime` creates for some
 * runtime files on macOS/Linux) need their own handling - `stat`/`copyFile` would follow them
 * through to the link target's content instead of moving the link itself. Recreates the link at
 * the destination (skip if one's already there) via `readlink`/`symlink`, then removes the
 * original. */
async function moveSymlink(source: string, destination: string): Promise<void> {
  const alreadyThere = await lstat(destination)
    .then(() => true)
    .catch(() => false)
  if (!alreadyThere) {
    await mkdir(dirname(destination), { recursive: true })
    await symlink(await readlink(source), destination)
  }
  await rm(source, { force: true })
}

interface TreeEntry {
  path: string
  isSymlink: boolean
}

async function listEntriesRecursive(dir: string): Promise<TreeEntry[]> {
  const dirents = await readdir(dir, { withFileTypes: true }).catch(() => [])
  const entries: TreeEntry[] = []
  for (const dirent of dirents) {
    const full = join(dir, dirent.name)
    if (dirent.isSymbolicLink()) entries.push({ path: full, isSymlink: true })
    else if (dirent.isDirectory()) entries.push(...(await listEntriesRecursive(full)))
    else if (dirent.isFile()) entries.push({ path: full, isSymlink: false })
  }
  return entries
}

/**
 * Moves every file/symlink under `sourceDir` to the identically-relative path under
 * `destinationDir`, then removes whatever's left of `sourceDir` (empty directory husks only, by
 * construction - every entry was either moved or already present at the destination). Used by both
 * `launch/storageConsolidation.ts` (per-instance legacy-folder cleanup) and
 * `launch/storageManager.ts#changeStorageLocation` (whole-root relocation). A no-op (besides the
 * final `rm`) if `sourceDir` doesn't exist or is already empty.
 */
export async function moveTree(
  sourceDir: string,
  destinationDir: string,
  concurrency: number,
  onProgress?: (completed: number, total: number, label: string) => void
): Promise<void> {
  const entries = await listEntriesRecursive(sourceDir)
  let completed = 0
  let nextIndex = 0

  async function worker(): Promise<void> {
    while (nextIndex < entries.length) {
      const entry = entries[nextIndex++]
      const relativePath = relative(sourceDir, entry.path)
      if (entry.isSymlink) await moveSymlink(entry.path, join(destinationDir, relativePath))
      else await moveOrSkip(entry.path, join(destinationDir, relativePath))
      completed++
      onProgress?.(completed, entries.length, relativePath)
    }
  }

  await Promise.all(Array.from({ length: Math.min(concurrency, entries.length) }, () => worker()))
  await rm(sourceDir, { recursive: true, force: true })
}
