import { app } from 'electron'
import { copyFile, mkdir, readdir, rm, stat } from 'node:fs/promises'
import { join } from 'node:path'
import type { LaunchStage } from '../../shared/types'
import { isAlwaysEnabledBundledMod } from './modsManager'

export type InstallProgressCallback = (
  stage: LaunchStage,
  completed: number,
  total: number,
  label?: string
) => void

async function listBundleFiles(bundleDir: string): Promise<string[]> {
  try {
    return await readdir(bundleDir)
  } catch {
    // launcher/mods-bundle and launcher/resourcepacks-bundle are git-ignored, dev-populated
    // folders (see Aktuelle_Phase.md) — nothing to sync on a checkout where they're empty/absent.
    return []
  }
}

/**
 * `excluded` (Phase 6c) is the one deliberate exception to "only ever adds/overwrites, never
 * deletes" below: a mod that's currently off in the Mods screen has to actually disappear from
 * `destinationDir`, not merely stop being re-copied - otherwise a jar synced in from an earlier,
 * still-enabled launch would keep sitting there and Fabric Loader would keep loading it regardless
 * of the toggle. Only ever removes filenames from `excluded` itself, nothing else the user or a
 * previous sync put there. Callers translate the opt-in `enabledBundledMods` setting into this
 * opt-out `excluded` set (see `syncBundledContent`) rather than this function knowing about
 * enable/disable semantics itself - it stays a generic "sync exactly these files" primitive,
 * reused unconditionally for `resourcepacks-bundle` too (no toggle concept there at all).
 */
async function syncBundleDir(bundleDir: string, destinationDir: string, excluded: Set<string> = new Set()): Promise<void> {
  const files = (await listBundleFiles(bundleDir)).filter((file) => !excluded.has(file))
  if (files.length > 0) {
    await mkdir(destinationDir, { recursive: true })
    await Promise.all(files.map((file) => copyFile(join(bundleDir, file), join(destinationDir, file))))
  }
  await Promise.all([...excluded].map((file) => rm(join(destinationDir, file), { force: true })))
}

/**
 * Copies whichever jar in `mod/build/libs/` is newest into `destModsDir`, overwriting whatever's
 * already there under that name. Our own mod jar changes constantly during development — unlike
 * the third-party jars in `mods-bundle/`, keeping a manually-updated copy of it there reliably
 * goes stale (that's exactly what happened: a 4 KB Phase-1 stub sat in `mods-bundle/` for days
 * while `mod/build/libs/` moved on through all of Phase 5, so every launcher-based test ran an
 * almost-empty mod with none of the actual features). Pulling straight from the build output
 * every launch makes that impossible - whatever `gradlew build` last produced is what runs, no
 * separate copy step to forget. Excludes `-sources.jar`/`-dev.jar` (Loom's unremapped
 * intermediary-names jar, not safe to run standalone) so only the real, remapped runtime jar is
 * ever picked.
 */
async function syncOwnModJar(repoRoot: string, destModsDir: string): Promise<void> {
  const libsDir = join(repoRoot, 'mod', 'build', 'libs')
  let entries: string[]
  try {
    entries = await readdir(libsDir)
  } catch {
    // Mod hasn't been built yet on this checkout (no `gradlew build` run) - nothing to pull in.
    return
  }

  const candidates = entries.filter(
    (name) => name.endsWith('.jar') && !name.includes('-sources') && !name.includes('-dev')
  )
  if (candidates.length === 0) return

  const withMtime = await Promise.all(
    candidates.map(async (name) => ({ name, mtimeMs: (await stat(join(libsDir, name))).mtimeMs }))
  )
  const newest = withMtime.reduce((a, b) => (b.mtimeMs > a.mtimeMs ? b : a))

  await mkdir(destModsDir, { recursive: true })
  await copyFile(join(libsDir, newest.name), join(destModsDir, newest.name))
}

/**
 * Copies the bundled third-party mod jars, the bundled resourcepack(s), and our own freshly
 * built mod jar into the instance's `game/mods` and `game/resourcepacks` folders on every launch.
 * Previously all of this was a one-off manual copy (see Phase 4/5p in Aktuelle_Phase.md) — meaning
 * a fresh or reset instance directory silently lost it, and (worse) a stale manual copy of our own
 * mod jar could sit there indefinitely without anyone noticing. Only ever adds/overwrites the
 * synced files, never deletes anything else already in those folders (see `syncBundleDir`'s own
 * doc comment for the one deliberate exception: mods currently toggled off), so anything the user
 * places there by hand survives a sync.
 *
 * Doesn't touch `options.txt` — a resourcepack still has to be enabled once in-game (Optionen ->
 * Ressourcenpakete), same as any resourcepack in vanilla Minecraft. This only guarantees the file
 * itself is always there to enable.
 *
 * Path resolution (`app.getAppPath()`, `mod/` as a sibling of `launcher/`) only holds for the
 * current unpackaged dev setup — needs revisiting once Phase 6 adds real electron-builder
 * packaging (`extraResources` or similar).
 *
 * `bundleCompatible` (Phase 6a) gates the whole sync (see `isBundleCompatibleVersion` in
 * shared/types.ts): every jar here (including our own mod) and every resourcepack is built
 * against `MINECRAFT_VERSION` specifically. Fabric Loader hard-rejects a mod whose
 * `fabric.mod.json` declares a Minecraft-version range that doesn't include the game version
 * actually being launched, so syncing them into a differently-versioned instance wouldn't
 * silently degrade the experience — it would break the launch outright. Skipping here instead
 * means a non-pinned version launches as plain vanilla-through-Fabric, no mods, which the caller
 * surfaces to the user via the returned flag.
 *
 * `enabledBundledMods` (Phase 6c, opt-in per later user request) lists which third-party mods the
 * user has actively turned on - defaults to none, so a fresh install starts with every *optional*
 * bundled mod off rather than everything silently active. Translated into `syncBundleDir`'s
 * opt-out `excluded` set here (every bundled filename NOT in the enabled list), with a few forced
 * exceptions that are never excluded regardless of the setting - see `modsManager.ts`'s
 * `isAlwaysEnabledBundledMod` for exactly which and why (Fabric API as a hard dependency, Sodium/
 * Lithium on explicit user request so performance doesn't regress by default either).
 */
export async function syncBundledContent(
  instanceDir: string,
  onProgress: InstallProgressCallback,
  bundleCompatible: boolean,
  enabledBundledMods: string[] = []
): Promise<{ skipped: boolean }> {
  onProgress('bundles', 0, 1)
  if (!bundleCompatible) {
    onProgress('bundles', 1, 1)
    return { skipped: true }
  }

  const appRoot = app.getAppPath()
  const repoRoot = join(appRoot, '..')
  const gameDir = join(instanceDir, 'game')
  const destModsDir = join(gameDir, 'mods')

  const modsBundleDir = join(appRoot, 'mods-bundle')
  const allBundledMods = await listBundleFiles(modsBundleDir)
  const enabledSet = new Set(enabledBundledMods)
  const disabledMods = new Set(
    allBundledMods.filter((file) => !enabledSet.has(file) && !isAlwaysEnabledBundledMod(file))
  )

  await syncBundleDir(modsBundleDir, destModsDir, disabledMods)
  await syncBundleDir(join(appRoot, 'resourcepacks-bundle'), join(gameDir, 'resourcepacks'))
  await syncOwnModJar(repoRoot, destModsDir)
  onProgress('bundles', 1, 1)
  return { skipped: false }
}
