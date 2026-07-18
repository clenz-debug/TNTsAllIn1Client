import { createHash, randomBytes } from 'node:crypto'
import { createServer } from 'node:http'
import { shell } from 'electron'
import { MS_CLIENT_ID, MS_TENANT } from '../config'

const AUTHORIZE_ENDPOINT = `https://login.microsoftonline.com/${MS_TENANT}/oauth2/v2.0/authorize`
const TOKEN_ENDPOINT = `https://login.microsoftonline.com/${MS_TENANT}/oauth2/v2.0/token`
const SCOPE = 'XboxLive.signin offline_access'

export interface MsTokenResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

function base64Url(input: Buffer): string {
  return input.toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function createPkcePair(): { verifier: string; challenge: string } {
  const verifier = base64Url(randomBytes(32))
  const challenge = base64Url(createHash('sha256').update(verifier).digest())
  return { verifier, challenge }
}

interface AuthorizationResult {
  code: string
  verifier: string
  redirectUri: string
}

/** Opens the system browser for Microsoft login and waits on a loopback HTTP server for the
 * redirect. The Azure app is registered with the bare `http://localhost` redirect URI (no path,
 * no port) under "Mobile and desktop applications" — Microsoft's identity platform special-cases
 * exactly that value to match any port at runtime, but only without a path suffix, so the
 * redirect_uri we send must be `http://localhost:{port}` with nothing after the port. The server
 * only ever serves this one login attempt, so it treats any request as the callback rather than
 * matching on path. */
async function getAuthorizationCode(): Promise<AuthorizationResult> {
  const { verifier, challenge } = createPkcePair()
  const state = base64Url(randomBytes(16))

  return await new Promise<AuthorizationResult>((resolve, reject) => {
    let redirectUri = ''

    const server = createServer((req, res) => {
      const url = new URL(req.url ?? '/', 'http://localhost')
      const code = url.searchParams.get('code')
      const error = url.searchParams.get('error')

      // Stray requests (e.g. the browser fetching /favicon.ico for the redirected-to page) have
      // neither param — ignore them instead of treating them as a failed callback, so they can't
      // race with and pre-empt the real one.
      if (code === null && error === null) {
        res.writeHead(404).end()
        return
      }

      const returnedState = url.searchParams.get('state')
      const ok = !error && code !== null && returnedState === state

      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' })
      res.end(
        ok
          ? '<html><body>Login erfolgreich. Dieses Fenster kann geschlossen werden.</body></html>'
          : '<html><body>Login fehlgeschlagen. Dieses Fenster kann geschlossen werden.</body></html>'
      )

      clearTimeout(timeout)
      server.close()
      if (ok && code) {
        resolve({ code, verifier, redirectUri })
      } else {
        reject(new Error(error ?? 'Missing or mismatched authorization code/state.'))
      }
    })

    const timeout = setTimeout(() => {
      server.close()
      reject(new Error('Microsoft login timed out after 5 minutes.'))
    }, 5 * 60 * 1000)

    server.on('error', (err) => {
      clearTimeout(timeout)
      reject(err)
    })

    server.listen(0, '127.0.0.1', () => {
      const address = server.address()
      if (address === null || typeof address === 'string') {
        reject(new Error('Failed to start loopback server.'))
        return
      }
      redirectUri = `http://localhost:${address.port}`

      const authorizeUrl = new URL(AUTHORIZE_ENDPOINT)
      authorizeUrl.searchParams.set('client_id', MS_CLIENT_ID)
      authorizeUrl.searchParams.set('response_type', 'code')
      authorizeUrl.searchParams.set('redirect_uri', redirectUri)
      authorizeUrl.searchParams.set('response_mode', 'query')
      authorizeUrl.searchParams.set('scope', SCOPE)
      authorizeUrl.searchParams.set('state', state)
      authorizeUrl.searchParams.set('code_challenge', challenge)
      authorizeUrl.searchParams.set('code_challenge_method', 'S256')
      authorizeUrl.searchParams.set('prompt', 'select_account')

      void shell.openExternal(authorizeUrl.toString())
    })
  })
}

async function requestToken(params: Record<string, string>): Promise<MsTokenResult> {
  const response = await fetch(TOKEN_ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams(params)
  })
  if (!response.ok) {
    throw new Error(`Microsoft token exchange failed: ${response.status} ${await response.text()}`)
  }
  const data = (await response.json()) as {
    access_token: string
    refresh_token: string
    expires_in: number
  }
  return { accessToken: data.access_token, refreshToken: data.refresh_token, expiresIn: data.expires_in }
}

export async function refreshMsToken(refreshToken: string): Promise<MsTokenResult> {
  return await requestToken({
    grant_type: 'refresh_token',
    refresh_token: refreshToken,
    client_id: MS_CLIENT_ID
  })
}

export async function loginWithMicrosoft(): Promise<MsTokenResult> {
  const { code, verifier, redirectUri } = await getAuthorizationCode()
  return await requestToken({
    grant_type: 'authorization_code',
    code,
    redirect_uri: redirectUri,
    code_verifier: verifier,
    client_id: MS_CLIENT_ID
  })
}
