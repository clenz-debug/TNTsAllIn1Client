import { createHash } from 'node:crypto'
import { localizedError } from '../shared/errorMessages'

/** Downloads a URL and verifies its SHA-1 against an expected hash before returning the bytes -
 * shared by every "fetch a third-party binary and trust it" path in this app (Modrinth mod
 * installs, the mod-bundle auto-updater's pinned jars and own-mod jar), so the check can never
 * accidentally drift between call sites. `label` is only used in the thrown error message. */
export async function downloadAndVerifySha1(url: string, expectedSha1: string, label: string, signal?: AbortSignal): Promise<Buffer> {
  const response = await fetch(url, { signal })
  if (!response.ok) {
    throw localizedError('download.failed', { status: response.status, url })
  }
  const buffer = Buffer.from(await response.arrayBuffer())
  const actualSha1 = createHash('sha1').update(buffer).digest('hex')
  if (actualSha1 !== expectedSha1) {
    throw localizedError('download.sha1Mismatch', { label, expected: expectedSha1, actual: actualSha1 })
  }
  return buffer
}
