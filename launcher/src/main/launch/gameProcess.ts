import { spawn } from 'node:child_process'
import { mkdir } from 'node:fs/promises'
import type { GameLogEvent } from '../../shared/types'

/** Substrings (matched case-insensitively against each raw output line) that only show up when
 * Fabric Loader's own mod resolver rejects the mod set - own user request, follow-up to
 * `modrinthApi.ts#installModrinthMod`'s install-time `incompatible` check, which only catches
 * conflicts Modrinth's own dependency metadata declares. Two real mods can still conflict in ways
 * neither side bothered to declare on Modrinth (or via a mechanism Modrinth's metadata can't
 * express at all, e.g. both mixin-patching the same method), and that failure only ever surfaces
 * here, at actual launch, in Fabric Loader's own log output - never as a launcher-side check.
 * Deliberately just a keyword match on Loader's own wording rather than a full parse of its
 * exception format, which differs across Loader versions and isn't worth chasing exactly - quoting
 * whichever of Loader's own lines mention "incompatib*" verbatim (see `matchedLines` below) already
 * gives the actual mod names without needing to know Loader's exact current message shape. */
const INCOMPATIBLE_MOD_MARKERS = ['incompatible mod', 'is incompatible with', 'modresolutionexception']

/** `javaBinaryPath` is always the exact binary `javaRuntime.ts`'s `ensureJavaRuntime` resolved
 * for the launched version's `javaVersion.component` - never a bare `'java'` relying on PATH.
 * That used to be the whole story (fine while only 1.21.11 was ever launched), but broke outright
 * once the Phase 6a version picker allowed other versions requiring a newer JVM than whatever's
 * on the system - see `javaRuntime.ts`'s doc comment for the full story. */
export async function launchGame(
  javaBinaryPath: string,
  args: string[],
  gameDirectory: string,
  onLog: (event: GameLogEvent) => void,
  signal?: AbortSignal
): Promise<void> {
  await mkdir(gameDirectory, { recursive: true })

  await new Promise<void>((resolve, reject) => {
    // `signal` lets `main/ipc/handlers.ts`'s Cancel button kill an already-running game the same
    // way it aborts an in-flight download - Node's own `spawn` support for this (since v15.14) means
    // no separate "keep the ChildProcess handle around to kill it later" bookkeeping is needed here.
    const proc = spawn(javaBinaryPath, args, { cwd: gameDirectory, signal })
    // Every raw line Fabric Loader itself flagged as incompatibility-related, collected as the
    // process runs (not re-scanned from a full buffer at the end) so a very chatty run doesn't
    // need its whole output held twice in memory just for this check.
    const matchedLines: string[] = []

    const scanForIncompatibility = (text: string): void => {
      for (const line of text.split(/\r?\n/)) {
        const lower = line.toLowerCase()
        if (INCOMPATIBLE_MOD_MARKERS.some((marker) => lower.includes(marker))) {
          matchedLines.push(line.trim())
        }
      }
    }

    proc.stdout.on('data', (chunk: Buffer) => {
      const text = chunk.toString().trimEnd()
      onLog({ source: 'game', level: 'info', message: text })
      scanForIncompatibility(text)
    })
    proc.stderr.on('data', (chunk: Buffer) => {
      const text = chunk.toString().trimEnd()
      onLog({ source: 'game', level: 'error', message: text })
      scanForIncompatibility(text)
    })
    proc.on('error', (error) => reject(error))
    proc.on('exit', (code) => {
      if (code !== 0 && matchedLines.length > 0) {
        onLog({
          source: 'launcher',
          level: 'error',
          message: [
            'Minecraft konnte nicht gestartet werden: Fabric Loader hat inkompatible Mods erkannt.',
            ...[...new Set(matchedLines)].slice(0, 8)
          ].join('\n')
        })
      }
      onLog({
        source: 'launcher',
        level: code === 0 ? 'info' : 'error',
        message: `Minecraft-Prozess beendet mit Code ${code}.`
      })
      resolve()
    })
  })
}
