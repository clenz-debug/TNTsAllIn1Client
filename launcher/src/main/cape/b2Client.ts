import { AwsClient } from 'aws4fetch'
import type { B2Config } from './b2Config'

/** Path-style addressing (`<endpoint>/<bucket>/<key>`) rather than virtual-hosted-style
 * (`<bucket>.<endpoint>`) - avoids any assumption about the bucket name being DNS-/wildcard-cert-
 * safe; Backblaze's S3-compatible API documents both, path-style always works. */
function objectUrl(config: B2Config, objectKey: string): string {
  return `${config.endpoint}/${config.bucket}/${objectKey}`
}

function signer(config: B2Config): AwsClient {
  return new AwsClient({ accessKeyId: config.keyId, secretAccessKey: config.applicationKey, service: 's3', region: config.region })
}

/** Uploads (or overwrites) one cape PNG - `objectKey` is always `<uuid>.png`, see
 * `capeStorage.ts`. aws4fetch signs the request (SigV4) and performs the actual `fetch` in one
 * call; B2's S3-compatible API needs nothing beyond that plus a `Content-Type`. */
export async function putCapeObject(config: B2Config, objectKey: string, pngBuffer: Buffer): Promise<void> {
  const response = await signer(config).fetch(objectUrl(config, objectKey), {
    method: 'PUT',
    body: pngBuffer,
    headers: { 'Content-Type': 'image/png' }
  })
  if (!response.ok) {
    throw new Error(`Cape-Upload zu B2 fehlgeschlagen (${response.status}): ${await response.text()}`)
  }
}

/** A 404 counts as success here - "delete my cape" and "there was never one" should both leave
 * the caller (capeStorage.ts#deleteCustomCape) with nothing left to do, not a thrown error. */
export async function deleteCapeObject(config: B2Config, objectKey: string): Promise<void> {
  const response = await signer(config).fetch(objectUrl(config, objectKey), { method: 'DELETE' })
  if (!response.ok && response.status !== 404) {
    throw new Error(`Cape-Löschen bei B2 fehlgeschlagen (${response.status}): ${await response.text()}`)
  }
}
