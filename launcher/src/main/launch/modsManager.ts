import type { BrowserWindow } from 'electron'
import { dialog } from 'electron'
import { basename, join } from 'node:path'
import { copyFile, mkdir, readdir, rename, rm } from 'node:fs/promises'
import type { Readable } from 'node:stream'
import { openPromise } from 'yauzl'
import type { CustomModEntry, ModNotice } from '../../shared/types'
import { loadLauncherSettings } from '../launcherSettings'
import { instanceDir } from './installer'
import { bundledModsDir } from './resourcePaths'

/** Suffix a custom mod's filename gets while disabled - keeps the file (and Modrinth-project
 * identity, since hashing doesn't care about the extension) around without Fabric Loader's `*.jar`
 * discovery picking it up, instead of only offering delete-and-redownload-later. Bundled mods don't
 * need this: they're toggled via `Instance.enabledBundledMods` since their master copy lives outside
 * the instance entirely (`mods-bundle/`) - a custom mod's jar in `game/mods` *is* the only copy. */
const DISABLED_SUFFIX = '.disabled'

function disabledFileName(fileName: string): string {
  return `${fileName}${DISABLED_SUFFIX}`
}

/** Turns a raw `game/mods` directory entry into a `CustomModEntry`, or `null` for anything that's
 * neither an active nor a disabled jar (e.g. a stray non-mod file someone dropped in there). */
function toCustomModEntry(rawName: string): Omit<CustomModEntry, 'notice'> | null {
  if (rawName.endsWith(DISABLED_SUFFIX) && rawName.slice(0, -DISABLED_SUFFIX.length).endsWith('.jar')) {
    return { fileName: rawName.slice(0, -DISABLED_SUFFIX.length), enabled: false }
  }
  if (rawName.endsWith('.jar')) {
    return { fileName: rawName, enabled: true }
  }
  return null
}

/** Fabric mod ids behind each {@link ModNotice}. Essential's jar in the mods folder is
 * `essential-container` (the real `essential` mod gets downloaded by it at startup), OptiFabric is
 * what makes OptiFine load on Fabric - Sodium's own `fabric.mod.json` declares it as `breaks`. */
const NOTICE_MOD_IDS: Record<string, ModNotice> = {
  'essential-container': 'essential',
  essential: 'essential',
  optifabric: 'optifine'
}

async function streamToString(stream: Readable): Promise<string> {
  const chunks: Buffer[] = []
  for await (const chunk of stream) {
    chunks.push(chunk as Buffer)
  }
  return Buffer.concat(chunks).toString('utf8')
}

/** Which {@link ModNotice} a jar deserves, if any - by its `fabric.mod.json` id, or for a plain
 * OptiFine jar (no `fabric.mod.json` at all) by its `net/optifine/` classes. Unreadable jars and
 * broken JSON just get no notice; this is only a hint, never a reason to fail listing the mods. */
async function detectModNotice(jarPath: string): Promise<ModNotice | null> {
  try {
    const zipfile = await openPromise(jarPath, { lazyEntries: true, autoClose: true })
    try {
      for await (const entry of zipfile.eachEntry()) {
        if (entry.fileName === 'fabric.mod.json') {
          const json = JSON.parse(await streamToString(await zipfile.openReadStreamPromise(entry))) as { id?: unknown }
          return typeof json.id === 'string' ? (NOTICE_MOD_IDS[json.id] ?? null) : null
        }
        if (entry.fileName.startsWith('net/optifine/')) {
          return 'optifine'
        }
      }
    } finally {
      zipfile.close()
    }
  } catch {
    // Not a readable jar - no notice.
  }
  return null
}

/** Matches `rootProject.name` in `mod/settings.gradle.kts` - every jar `bundleSync.syncOwnModJar`
 * produces starts with this, regardless of version (`tntsallin1client-0.1.0.jar` etc.). Used to
 * exclude our own mod from the "custom mods" list below - it's not something the user adds or
 * removes here, it's what makes this "our" client to begin with. */
export const OWN_MOD_PREFIX = 'tntsallin1client-'

/**
 * Bundled jar filename prefixes that are never optional via the Mods screen's toggle - always
 * synced regardless of `enabledBundledMods`, for a few different reasons:
 *  - `fabric-api-`: a hard dependency every other bundled mod declares in its own
 *    `fabric.mod.json`. If it were off while e.g. Sodium was on, Fabric Loader would reject the
 *    whole launch on Sodium's unmet dependency instead of degrading gracefully.
 *  - `sodium-fabric-`/`lithium-fabric-`: explicit user request - pure performance, no visible
 *    behavior change, and losing "good performance even on weak hardware" by default would work
 *    against this project's whole point (see Projekt_Roadmap.md's stated goal).
 *  - `continuity-`/`skinlayers3d-fabric-`: also explicit user request, different reasoning - both
 *    already have their own dedicated on/off switch in the ingame mod menu (`ClientMenuScreen`'s
 *    "Connected Textures"/"3D Skin Layers" rows), which is the actually meaningful control surface
 *    for them. Gating them a second time behind the launcher's toggle first would just be a
 *    redundant, easy-to-forget extra step before the ingame toggle even becomes reachable.
 *  - `e4mc-`: world invitations to friends (Phase 8b) depend on it. Our mod keeps its public tunnel
 *    switched off except while inviting (`E4mcControl.java`), so a normal "Open to LAN" stays local.
 *
 * Every other bundled jar (today only Cape Provider) shows up as a switch in the Mods screen -
 * off by default unless listed in `shared/bundledMods.ts`'s default-on prefixes, which Cape
 * Provider is (own user request: on by default, but still the user's choice).
 */
const ALWAYS_ENABLED_PREFIXES = [
  'fabric-api-',
  'sodium-fabric-',
  'lithium-fabric-',
  'continuity-',
  'skinlayers3d-fabric-',
  'e4mc-'
]

export function isAlwaysEnabledBundledMod(fileName: string): boolean {
  return ALWAYS_ENABLED_PREFIXES.some((prefix) => fileName.startsWith(prefix))
}

async function listJarsIn(dir: string): Promise<string[]> {
  try {
    return (await readdir(dir)).filter((name) => name.endsWith('.jar'))
  } catch {
    return []
  }
}

/** Every third-party jar in `launcher/mods-bundle/<versionId>/`, Fabric API included - the
 * full-fidelity list used to recognize "this filename is a bundled mod" (see `listCustomMods` below
 * and `bundleSync.ts`), read fresh every time rather than cached, since it only changes when someone
 * edits the (dev-populated, git-ignored) folder itself or an update gets applied. Not what the Mods
 * screen shows as toggleable - see {@link listToggleableBundledMods} for that. */
export async function listBundledMods(versionId: string): Promise<string[]> {
  return listJarsIn(bundledModsDir(versionId))
}

/** The subset of {@link listBundledMods} the Mods screen actually offers a checkbox for - the
 * always-enabled ones are deliberately left out, see {@link isAlwaysEnabledBundledMod}. */
export async function listToggleableBundledMods(versionId: string): Promise<string[]> {
  const all = await listBundledMods(versionId)
  return all.filter((name) => !isAlwaysEnabledBundledMod(name))
}

/** Whatever's sitting in an instance's `game/mods` folder that isn't a bundled mod and isn't our
 * own jar - i.e. mods the user added themselves via `addCustomMods`, active or disabled alike (see
 * `DISABLED_SUFFIX`). Tied to a specific instance (not just a version) since the whole point of
 * instances is that two of them can target the same Minecraft version with a different mod set.
 * Resolves the instance's own `versionId` internally (same lookup pattern `ipc/handlers.ts`'s
 * `LaunchPlay` handler already uses) so callers/IPC/preload don't need to pass one through just for
 * this exclusion check - an instance that's gone missing from settings falls back to an empty
 * bundled-mods exclusion set rather than throwing. */
export async function listCustomMods(instanceId: string): Promise<CustomModEntry[]> {
  const modsDir = join(instanceDir(instanceId), 'game', 'mods')
  const settings = await loadLauncherSettings()
  const instance = settings.instances.find((candidate) => candidate.id === instanceId)
  const bundled = new Set(instance ? await listBundledMods(instance.versionId) : [])
  const rawEntries = await readdir(modsDir).catch(() => [])
  const entries = rawEntries
    .map(toCustomModEntry)
    .filter((entry): entry is Omit<CustomModEntry, 'notice'> => entry !== null)
    .filter((entry) => !bundled.has(entry.fileName) && !entry.fileName.startsWith(OWN_MOD_PREFIX))
  return Promise.all(
    entries.map(async (entry) => ({
      ...entry,
      notice: await detectModNotice(join(modsDir, entry.enabled ? entry.fileName : disabledFileName(entry.fileName)))
    }))
  )
}

/**
 * Opens a native "choose file(s)" dialog (not literal drag & drop - see Aktuelle_Phase.md for why)
 * scoped to `.jar` files, copies whatever was picked into the given instance's `game/mods`, and
 * returns the refreshed custom-mods list. A cancelled dialog is not an error, just returns the
 * unchanged list.
 */
export async function addCustomMods(instanceId: string, parentWindow: BrowserWindow | null): Promise<CustomModEntry[]> {
  const dialogOptions: Electron.OpenDialogOptions = {
    title: 'Fabric-Mod-Jar(s) auswählen',
    properties: ['openFile', 'multiSelections'],
    filters: [{ name: 'Fabric Mod Jar', extensions: ['jar'] }]
  }
  const result = parentWindow
    ? await dialog.showOpenDialog(parentWindow, dialogOptions)
    : await dialog.showOpenDialog(dialogOptions)
  if (!result.canceled && result.filePaths.length > 0) {
    const modsDir = join(instanceDir(instanceId), 'game', 'mods')
    await mkdir(modsDir, { recursive: true })
    await Promise.all(result.filePaths.map((filePath) => copyFile(filePath, join(modsDir, basename(filePath)))))
  }
  return listCustomMods(instanceId)
}

/** Removes a custom mod's jar regardless of whether it's currently enabled or disabled - `fileName`
 * is always the plain `.jar` name (see `CustomModEntry`), so both possible on-disk names are cleared;
 * exactly one of them ever actually exists, `force: true` just makes the other a no-op. */
export async function removeCustomMod(instanceId: string, fileName: string): Promise<CustomModEntry[]> {
  const modsDir = join(instanceDir(instanceId), 'game', 'mods')
  await Promise.all([
    rm(join(modsDir, fileName), { force: true }),
    rm(join(modsDir, disabledFileName(fileName)), { force: true })
  ])
  return listCustomMods(instanceId)
}

/** Toggles a custom mod on/off by renaming it in place (see `DISABLED_SUFFIX`) - own user request,
 * a middle ground between "always loaded" and "gone for good" that `removeCustomMod` alone offered.
 * A disabled mod stops showing up in `resolveProjectVersionsInDir` (`modrinthApi.ts`) too, since that
 * only ever scans `*.jar` - exactly right, since an inert mod can't actually conflict with anything. */
export async function setCustomModEnabled(instanceId: string, fileName: string, enabled: boolean): Promise<CustomModEntry[]> {
  const modsDir = join(instanceDir(instanceId), 'game', 'mods')
  const activePath = join(modsDir, fileName)
  const disabledPath = join(modsDir, disabledFileName(fileName))
  try {
    await rename(enabled ? disabledPath : activePath, enabled ? activePath : disabledPath)
  } catch (err) {
    if ((err as NodeJS.ErrnoException).code !== 'ENOENT') throw err
  }
  return listCustomMods(instanceId)
}
