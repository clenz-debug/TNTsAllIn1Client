import { app } from 'electron'
import { copyFile, mkdir, readdir, readFile, rm, stat } from 'node:fs/promises'
import { join } from 'node:path'
import type { LaunchStage } from '../../shared/types'
import { isAlwaysEnabledBundledMod } from './modsManager'
import { bundledModsDir, bundledResourcepacksDir, bundledResourcesRoot, ownModDir } from './resourcePaths'

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
 * Copies whichever jar in `libsDir` is newest into `destModsDir`, overwriting whatever's already
 * there under that name. In dev mode `libsDir` is `mod/build/libs/` directly - our own mod jar
 * changes constantly during development, unlike the third-party jars in `mods-bundle/`, so keeping
 * a manually-updated copy of it there reliably goes stale (that's exactly what happened once: a
 * 4 KB Phase-1 stub sat in `mods-bundle/` for days while `mod/build/libs/` moved on through all of
 * Phase 5, so every launcher-based test ran an almost-empty mod with none of the actual features).
 * Pulling straight from the build output every launch makes that impossible - whatever
 * `gradlew build` last produced is what runs, no separate copy step to forget.
 *
 * Packaged builds have no `mod/` sibling project at all - `libsDir` there is instead
 * `resources/own-mod/`, a one-time snapshot electron-builder copies in at package time (see
 * `electron-builder.yml`'s `extraResources`) from whatever `mod/build/libs/` held at that moment.
 * Same "newest wins" logic still applies even though there's normally only one candidate there -
 * keeps this one function correct for both cases without an `isPackaged` branch inside it.
 *
 * Either way, excludes `-sources.jar`/`-dev.jar` (Loom's unremapped intermediary-names jar, not
 * safe to run standalone) so only the real, remapped runtime jar is ever picked.
 */
async function syncOwnModJar(libsDir: string, destModsDir: string): Promise<void> {
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

/** Reads `mod/gradle.properties`' `minecraft_version` value directly (not via Gradle - this only
 * needs the one property, spinning up Gradle just to read it would be very slow) - lets dev mode
 * tell whether `mod/build/libs/`'s current contents were actually built for the version being
 * launched right now, or are just left over from whichever version the local `mod/` checkout last
 * targeted. Returns `null` if the file is missing/unparseable (no local `mod/` checkout at all). */
async function readModGradleMinecraftVersion(resourcesRoot: string): Promise<string | null> {
  try {
    const raw = await readFile(join(resourcesRoot, '..', 'mod', 'gradle.properties'), 'utf-8')
    const match = raw.match(/^minecraft_version=(.+)$/m)
    return match ? match[1].trim() : null
  } catch {
    return null
  }
}

/**
 * Dev-mode own-mod-jar source for one Minecraft version, multi-version support follow-up: `mod/`
 * only ever holds one live build at a time (`gradle.properties`' `minecraft_version` decides which),
 * so testing more than one version locally needs *some* place to keep the others' jars around.
 * Reuses the exact same `own-mod/<versionId>/` folder packaged builds already use (see `ownModDir`)
 * as an opt-in snapshot spot - stash a built jar there by hand (same manual step the release runbook
 * already has you do for a real release) for any version you're not actively iterating on right now.
 *
 * Falls back to the live `mod/build/libs/` pull only when no snapshot exists for `versionId` *and*
 * `mod/gradle.properties`' `minecraft_version` currently matches `versionId` too - so the version
 * you're actively developing against still auto-picks up every fresh `gradlew build` without a
 * manual copy step (same anti-staleness reasoning `syncOwnModJar`'s own doc comment already gives
 * for why dev mode reads live in the first place), but a *different*, not-yet-snapshotted version
 * doesn't silently get whatever mismatched jar `mod/build/libs/` happens to hold right now - that
 * would just get rejected by Fabric Loader anyway (confirmed live during this session's own
 * multi-version testing), just with a much less obvious cause than "no snapshot for this version
 * yet, own mod skipped" (same no-own-mod-mod behavior `syncOwnModJar` already has for a fresh
 * checkout that was never built at all).
 */
async function resolveDevOwnModLibsDir(resourcesRoot: string, versionId: string): Promise<string> {
  const snapshotDir = ownModDir(versionId)
  const hasSnapshot = await readdir(snapshotDir)
    .then((entries) => entries.some((name) => name.endsWith('.jar')))
    .catch(() => false)
  if (hasSnapshot) return snapshotDir

  const liveTargetVersion = await readModGradleMinecraftVersion(resourcesRoot)
  return liveTargetVersion === versionId ? join(resourcesRoot, '..', 'mod', 'build', 'libs') : snapshotDir
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
 * Path resolution goes through {@link bundledResourcesRoot} - dev-mode a sibling of `launcher/`,
 * packaged mode `electron-builder`'s `extraResources` directory (Phase 9, see `resourcePaths.ts`'s
 * own doc comment for the full story - this used to only work in the unpackaged dev setup).
 *
 * `bundleCompatible` (Phase 6a, now backed by the dynamic per-version manifest check rather than a
 * single hardcoded version - see `isVersionBundleCompatible` in `bundleCompat.ts`) gates the whole
 * sync: every jar/resourcepack synced here for a given `versionId` is built specifically against
 * that Minecraft version, read from that version's own `mods-bundle/<versionId>/` etc. subfolder
 * (see `resourcePaths.ts`). Fabric Loader hard-rejects a mod whose `fabric.mod.json` declares a
 * Minecraft-version range that doesn't include the game version actually being launched, so syncing
 * the wrong version's jars into an instance wouldn't silently degrade the experience — it would
 * break the launch outright. Skipping here instead means a non-bundle-compatible version launches
 * as plain vanilla-through-Fabric, no mods, which the caller surfaces to the user via the returned
 * flag.
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
  versionId: string,
  enabledBundledMods: string[] = []
): Promise<{ skipped: boolean }> {
  onProgress('bundles', 0, 1)
  if (!bundleCompatible) {
    onProgress('bundles', 1, 1)
    return { skipped: true }
  }

  const resourcesRoot = bundledResourcesRoot()
  const gameDir = join(instanceDir, 'game')
  const destModsDir = join(gameDir, 'mods')

  const modsBundleDir = bundledModsDir(versionId)
  const allBundledMods = await listBundleFiles(modsBundleDir)
  const enabledSet = new Set(enabledBundledMods)
  const disabledMods = new Set(
    allBundledMods.filter((file) => !enabledSet.has(file) && !isAlwaysEnabledBundledMod(file))
  )

  // Packaged builds ship a frozen own-mod-jar snapshot under resources/own-mod/<versionId>/ (see
  // electron-builder.yml) instead of a live sibling mod/build/libs/ - there is no mod/ project at
  // all once the launcher is actually installed on someone else's machine.
  const ownModLibsDir = app.isPackaged ? ownModDir(versionId) : await resolveDevOwnModLibsDir(resourcesRoot, versionId)

  await syncBundleDir(modsBundleDir, destModsDir, disabledMods)
  await syncBundleDir(bundledResourcepacksDir(versionId), join(gameDir, 'resourcepacks'))
  await syncOwnModJar(ownModLibsDir, destModsDir)
  onProgress('bundles', 1, 1)
  return { skipped: false }
}
