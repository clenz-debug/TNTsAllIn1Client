import { spawn } from 'node:child_process'
import { mkdir } from 'node:fs/promises'
import type { GameLogEvent } from '../../shared/types'

export async function checkJavaAvailable(): Promise<boolean> {
  return await new Promise((resolve) => {
    const proc = spawn('java', ['-version'])
    proc.on('error', () => resolve(false))
    proc.on('exit', (code) => resolve(code === 0))
  })
}

/** Parses e.g. `openjdk version "21.0.11" ...` -> 21, or the old `"1.8.0_301"` scheme -> 8.
 * Exported for tests; the real entry point is `getInstalledJavaMajorVersion`. */
export function parseJavaMajorVersion(versionOutput: string): number | null {
  const match = versionOutput.match(/version "(\d+)(?:\.(\d+))?/)
  if (!match) return null
  const first = Number(match[1])
  return first === 1 && match[2] ? Number(match[2]) : first
}

/**
 * The system `java` on PATH is the only JVM this launcher ever spawns (see `launchGame` below) -
 * there's no per-version runtime download/management like Mojang's own launcher does. That's fine
 * as long as the selected Minecraft version's required Java major version (`VersionDetail.javaVersion`)
 * is one this JVM actually satisfies; if it isn't, the JVM doesn't just run in a degraded way, it
 * refuses to start outright (e.g. `26.2` ships an unconditional `--sun-misc-unsafe-memory-access=allow`
 * JVM arg that only exists from JDK 24 onward - a JDK 21 print "Unrecognized option" and exits
 * before Minecraft's own code ever runs). Used by `ipc/handlers.ts` to catch that mismatch up
 * front with a readable message instead of surfacing the raw JVM crash.
 */
export async function getInstalledJavaMajorVersion(): Promise<number | null> {
  return await new Promise((resolve) => {
    const proc = spawn('java', ['-version'])
    let output = ''
    proc.stdout.on('data', (chunk: Buffer) => (output += chunk.toString()))
    proc.stderr.on('data', (chunk: Buffer) => (output += chunk.toString()))
    proc.on('error', () => resolve(null))
    proc.on('exit', () => resolve(parseJavaMajorVersion(output)))
  })
}

export async function launchGame(
  args: string[],
  gameDirectory: string,
  onLog: (event: GameLogEvent) => void
): Promise<void> {
  await mkdir(gameDirectory, { recursive: true })

  await new Promise<void>((resolve, reject) => {
    const proc = spawn('java', args, { cwd: gameDirectory })

    proc.stdout.on('data', (chunk: Buffer) => {
      onLog({ source: 'game', level: 'info', message: chunk.toString().trimEnd() })
    })
    proc.stderr.on('data', (chunk: Buffer) => {
      onLog({ source: 'game', level: 'error', message: chunk.toString().trimEnd() })
    })
    proc.on('error', (error) => reject(error))
    proc.on('exit', (code) => {
      onLog({
        source: 'launcher',
        level: code === 0 ? 'info' : 'error',
        message: `Minecraft-Prozess beendet mit Code ${code}.`
      })
      resolve()
    })
  })
}
