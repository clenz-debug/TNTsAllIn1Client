import { createHash } from 'node:crypto'

/** Downloads a URL and verifies its SHA-1 against an expected hash before returning the bytes -
 * shared by every "fetch a third-party binary and trust it" path in this app (Modrinth mod
 * installs, the mod-bundle auto-updater's pinned jars and own-mod jar), so the check can never
 * accidentally drift between call sites. `label` is only used in the thrown error message. */
export async function downloadAndVerifySha1(url: string, expectedSha1: string, label: string): Promise<Buffer> {
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`Download fehlgeschlagen (${response.status}): ${url}`)
  }
  const buffer = Buffer.from(await response.arrayBuffer())
  const actualSha1 = createHash('sha1').update(buffer).digest('hex')
  if (actualSha1 !== expectedSha1) {
    throw new Error(`SHA-1 stimmt nicht überein für ${label}: erwartet ${expectedSha1}, erhalten ${actualSha1}`)
  }
  return buffer
}
