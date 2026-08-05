import { spawn } from 'node:child_process'
import { mkdir } from 'node:fs/promises'
import type { GameLogEvent } from '../../shared/types'

/** `javaBinaryPath` is always the exact binary `javaRuntime.ts`'s `ensureJavaRuntime` resolved
 * for the launched version's `javaVersion.component` - never a bare `'java'` relying on PATH.
 * That used to be the whole story (fine while only 1.21.11 was ever launched), but broke outright
 * once the Phase 6a version picker allowed other versions requiring a newer JVM than whatever's
 * on the system - see `javaRuntime.ts`'s doc comment for the full story. */
export async function launchGame(
  javaBinaryPath: string,
  args: string[],
  gameDirectory: string,
  onLog: (event: GameLogEvent) => void
): Promise<void> {
  await mkdir(gameDirectory, { recursive: true })

  await new Promise<void>((resolve, reject) => {
    const proc = spawn(javaBinaryPath, args, { cwd: gameDirectory })

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
