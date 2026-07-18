export interface XstsResult {
  xstsToken: string
  userHash: string
}

interface XboxLiveTokenResponse {
  Token: string
  DisplayClaims: { xui: Array<{ uhs: string }> }
}

async function authenticateXboxLive(msAccessToken: string): Promise<string> {
  const response = await fetch('https://user.auth.xboxlive.com/user/authenticate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({
      Properties: {
        AuthMethod: 'RPS',
        SiteName: 'user.auth.xboxlive.com',
        RpsTicket: `d=${msAccessToken}`
      },
      RelyingParty: 'http://auth.xboxlive.com',
      TokenType: 'JWT'
    })
  })
  if (!response.ok) {
    throw new Error(`Xbox Live authentication failed: ${response.status} ${await response.text()}`)
  }
  const data = (await response.json()) as XboxLiveTokenResponse
  return data.Token
}

async function authorizeXsts(xblToken: string): Promise<XstsResult> {
  const response = await fetch('https://xsts.auth.xboxlive.com/xsts/authorize', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({
      Properties: { SandboxId: 'RETAIL', UserTokens: [xblToken] },
      RelyingParty: 'rp://api.minecraftservices.com/',
      TokenType: 'JWT'
    })
  })
  const data = (await response.json()) as XboxLiveTokenResponse & { XErr?: number }
  if (!response.ok) {
    // Well-known XSTS error codes, see minecraft.wiki "Microsoft Authentication".
    if (data.XErr === 2148916233) {
      throw new Error(
        'Dieses Microsoft-Konto hat kein Xbox-Live-Profil. Auf xbox.com eines anlegen und erneut versuchen.'
      )
    }
    if (data.XErr === 2148916238) {
      throw new Error('Dieser Account ist ein Kinderkonto ohne Zustimmung eines Erziehungsberechtigten für Xbox Live.')
    }
    throw new Error(`XSTS authorization failed: ${response.status} ${JSON.stringify(data)}`)
  }
  return { xstsToken: data.Token, userHash: data.DisplayClaims.xui[0].uhs }
}

export async function loginToXboxLive(msAccessToken: string): Promise<XstsResult> {
  const xblToken = await authenticateXboxLive(msAccessToken)
  return await authorizeXsts(xblToken)
}
