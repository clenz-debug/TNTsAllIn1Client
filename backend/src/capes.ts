import { mkdir, rename, rm, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { config } from './config.js'

/**
 * Files are named by the *dashed* UUID (`223d5156-5c7b-...png`): Cape Provider's `§idNoHyphen`
 * placeholder is unusable - its template filler replaces `§id` first, which also matches inside
 * `§idNoHyphen` and leaves `<dashed-uuid>NoHyphen` behind (bytecode-verified in 5.4.3), so the mods'
 * URL template uses `§id` instead. Mojang returns the id without dashes; this adds them.
 */
function dashedUuid(uuid: string): string {
  const hex = uuid.replace(/-/g, '').toLowerCase()
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

function capePath(uuid: string): string {
  return join(config.capesDir, `${dashedUuid(uuid)}.png`)
}

export function capeUrl(uuid: string): string {
  return `${config.publicBaseUrl}/${dashedUuid(uuid)}.png`
}

/** Written to a temp file first, then renamed - Apache never serves a half-written PNG. */
export async function saveCape(uuid: string, png: Buffer): Promise<void> {
  await mkdir(config.capesDir, { recursive: true })
  const tempPath = join(config.capesDir, `.${dashedUuid(uuid)}.${process.pid}.tmp`)
  await writeFile(tempPath, png, { mode: 0o644 })
  await rename(tempPath, capePath(uuid))
}

export async function deleteCape(uuid: string): Promise<void> {
  await rm(capePath(uuid), { force: true })
}
