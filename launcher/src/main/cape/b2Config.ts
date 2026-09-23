import { localizedError } from '../../shared/errorMessages'

export interface B2Config {
  keyId: string
  applicationKey: string
  bucket: string
  /** S3-compatible endpoint, e.g. `https://s3.us-west-004.backblazeb2.com` - used for signed
   * PUT/DELETE. Distinct from `publicBaseUrl` (Backblaze's "Friendly URL"), which is what public,
   * unauthenticated GETs - and the URL baked into the mod's `fabric.mod.json` - actually use. */
  endpoint: string
  region: string
  publicBaseUrl: string
}

const REQUIRED_VARS = [
  'MAIN_VITE_B2_KEY_ID',
  'MAIN_VITE_B2_APPLICATION_KEY',
  'MAIN_VITE_B2_BUCKET',
  'MAIN_VITE_B2_ENDPOINT',
  'MAIN_VITE_B2_REGION',
  'MAIN_VITE_B2_PUBLIC_BASE_URL'
] as const

/**
 * Reads the Backblaze B2 credentials/bucket info `launcher/.env` provides at build time (see
 * `env.d.ts`, `.env.example`) - this project's first *own* service secret, unlike every other
 * credential here (Mojang/Microsoft tokens), which is per-user and never something we provision.
 * Throws a clear German error rather than letting a `putCapeObject` call fail with a confusing
 * network/auth error if `.env` was never filled in - same "surface it, don't crash silently"
 * philosophy the rest of this app already follows, just via a thrown error instead of a dedicated
 * UI mode, since a missing `.env` is a one-time dev/build setup gap, not a long-lived expected state.
 */
export function getB2Config(): B2Config {
  const env = import.meta.env
  const missing = REQUIRED_VARS.filter((name) => !env[name])
  if (missing.length > 0) {
    throw localizedError('cape.notConfigured', { missing: missing.join(', ') })
  }
  return {
    keyId: env.MAIN_VITE_B2_KEY_ID as string,
    applicationKey: env.MAIN_VITE_B2_APPLICATION_KEY as string,
    bucket: env.MAIN_VITE_B2_BUCKET as string,
    endpoint: env.MAIN_VITE_B2_ENDPOINT as string,
    region: env.MAIN_VITE_B2_REGION as string,
    publicBaseUrl: env.MAIN_VITE_B2_PUBLIC_BASE_URL as string
  }
}
