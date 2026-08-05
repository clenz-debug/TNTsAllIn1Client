import { app } from 'electron'
import { chmod, mkdir, readFile, symlink, writeFile } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import type { LaunchStage } from '../../shared/types'
import { downloadAll, type DownloadTask } from './downloader'

/** Fixed, well-known manifest URL Mojang's own launcher uses to look up Java runtimes - not tied
 * to any Minecraft version, just occasionally rotated by Mojang itself (see minecraft.wiki /
 * community "Piston Meta" notes if this ever starts 404ing). */
const RUNTIME_MANIFEST_URL =
  'https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json'

interface RuntimeManifestRef {
  manifest: { url: string }
  version: { name: string }
}

type RuntimeManifest = Record<string, Record<string, RuntimeManifestRef[]>>

interface RuntimeFileEntry {
  type: 'file' | 'directory' | 'link'
  executable?: boolean
  target?: string
  downloads?: { raw: { sha1: string; url: string } }
}

interface RuntimeFilesManifest {
  files: Record<string, RuntimeFileEntry>
}

export type InstallProgressCallback = (
  stage: LaunchStage,
  completed: number,
  total: number,
  label?: string
) => void

/** Mojang's own os/arch keys for this manifest - narrower than what Minecraft itself runs on (no
 * Linux ARM, no 32-bit Mac - `all.json` simply has no entry for those). */
function runtimeOsKey(): string {
  const { platform, arch } = process
  if (platform === 'win32') {
    if (arch === 'arm64') return 'windows-arm64'
    if (arch === 'ia32') return 'windows-x86'
    return 'windows-x64'
  }
  if (platform === 'darwin') {
    return arch === 'arm64' ? 'mac-os-arm64' : 'mac-os'
  }
  if (platform === 'linux') {
    return arch === 'ia32' ? 'linux-i386' : 'linux'
  }
  throw new Error(`No Mojang Java runtime available for ${platform}/${arch}.`)
}

/** Relative path to the `java` executable inside an unpacked runtime - verified against the
 * actual file manifests for windows-x64/linux/mac-os (macOS wraps everything in a
 * `jre.bundle/Contents/Home` bundle, Windows/Linux don't). Only windows-x64 is live-tested (dev
 * machine here is Windows) - mirrors the "[?]" cross-platform status in Ideen_für_den_client.md. */
function javaBinaryRelativePath(osKey: string): string {
  if (osKey.startsWith('windows')) return 'bin/java.exe'
  if (osKey.startsWith('mac-os')) return 'jre.bundle/Contents/Home/bin/java'
  return 'bin/java'
}

function runtimeDir(component: string): string {
  return join(app.getPath('userData'), 'java-runtimes', component)
}

/**
 * Downloads and unpacks the exact Java runtime Mojang's own launcher would use for a given
 * `VersionDetail.javaVersion.component` (e.g. `java-runtime-delta`), and returns the path to its
 * `java`/`java.exe` binary. Shared across every instance/version that happens to need the same
 * component - a runtime is a download cache, not per-instance state, same reasoning `libraries/`
 * already gets inside a single instance, just one level further up since multiple *instances*
 * can share one runtime too.
 *
 * Exists because the launcher previously always spawned whatever `java` was on the system PATH
 * (fine as long as only 1.21.11 was ever launched, since Phase 0 specifically set up JDK 21 for
 * that) - once the Phase 6a version picker made other Minecraft versions selectable, that broke
 * outright for any version needing a newer Java than JDK 21 (see Aktuelle_Phase.md's "erster
 * Live-Test" bugfix note): the JVM refuses to even start on a JVM arg it doesn't recognize,
 * there's no graceful degradation. Matching Mojang's own per-version runtime exactly - the same
 * thing their own launcher does - removes that whole class of failure instead of only detecting
 * it.
 */
export async function ensureJavaRuntime(component: string, onProgress: InstallProgressCallback): Promise<string> {
  onProgress('java-runtime', 0, 1, component)
  const osKey = runtimeOsKey()
  const dir = runtimeDir(component)
  const javaBinaryPath = join(dir, ...javaBinaryRelativePath(osKey).split('/'))
  const versionMarkerPath = join(dir, '.version')

  const manifestResponse = await fetch(RUNTIME_MANIFEST_URL)
  if (!manifestResponse.ok) {
    throw new Error(`Failed to fetch Java runtime manifest: ${manifestResponse.status}`)
  }
  const manifest = (await manifestResponse.json()) as RuntimeManifest
  const ref = manifest[osKey]?.[component]?.[0]
  if (!ref) {
    throw new Error(`No Java runtime "${component}" available for ${osKey}.`)
  }

  const installedVersion = await readFile(versionMarkerPath, 'utf8').catch(() => null)
  if (installedVersion?.trim() === ref.version.name) {
    onProgress('java-runtime', 1, 1, ref.version.name)
    return javaBinaryPath
  }

  const filesResponse = await fetch(ref.manifest.url)
  if (!filesResponse.ok) {
    throw new Error(`Failed to fetch Java runtime file list: ${filesResponse.status}`)
  }
  const filesManifest = (await filesResponse.json()) as RuntimeFilesManifest
  const entries = Object.entries(filesManifest.files)

  const directories = entries.filter(([, e]) => e.type === 'directory')
  const files = entries.filter(([, e]) => e.type === 'file')
  const links = entries.filter(([, e]) => e.type === 'link')

  await mkdir(dir, { recursive: true })
  for (const [relPath] of directories) {
    await mkdir(join(dir, relPath), { recursive: true })
  }

  const tasks: DownloadTask[] = files.map(([relPath, entry]) => ({
    url: entry.downloads!.raw.url,
    destination: join(dir, relPath),
    sha1: entry.downloads!.raw.sha1
  }))
  await downloadAll(tasks, 8, (completed, total, label) => onProgress('java-runtime', completed, total, label))

  // Windows has no POSIX executable bit to set - the `.exe` extension alone makes it runnable.
  if (process.platform !== 'win32') {
    for (const [relPath, entry] of files) {
      if (entry.executable) await chmod(join(dir, relPath), 0o755)
    }
  }

  for (const [relPath, entry] of links) {
    const linkPath = join(dir, relPath)
    await mkdir(dirname(linkPath), { recursive: true })
    await symlink(entry.target!, linkPath).catch((err: NodeJS.ErrnoException) => {
      if (err.code !== 'EEXIST') throw err
    })
  }

  await writeFile(versionMarkerPath, ref.version.name, 'utf8')
  onProgress('java-runtime', 1, 1, ref.version.name)
  return javaBinaryPath
}
