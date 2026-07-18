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
