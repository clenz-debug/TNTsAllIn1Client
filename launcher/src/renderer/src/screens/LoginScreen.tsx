import { useEffect, useState } from 'react'
import { formatError } from '../formatError'
import { useTranslations } from '../i18n/LanguageContext'
import { Logo } from '../Logo'
import type { AuthProgressEvent, MinecraftProfile } from '../../../shared/types'

interface Props {
  onLoggedIn: (profile: MinecraftProfile) => void
}

export function LoginScreen({ onLoggedIn }: Props) {
  const t = useTranslations()
  const [status, setStatus] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  /** Whoever logged out on this PC last - can still play offline, see `tokenCache.ts`. */
  const [offlineProfile, setOfflineProfile] = useState<MinecraftProfile | null>(null)

  useEffect(() => {
    void window.api
      .signedOutProfile()
      .then(setOfflineProfile)
      .catch(() => undefined)
  }, [])

  async function handleLogin(): Promise<void> {
    setBusy(true)
    setError(null)
    const unsubscribe = window.api.onAuthProgress((event: AuthProgressEvent) => setStatus(event.message))
    try {
      const profile = await window.api.login()
      onLoggedIn(profile)
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      unsubscribe()
      setBusy(false)
    }
  }

  return (
    <div className="login-screen">
      <Logo className="login-logo" />
      <h1>{t.login.title}</h1>
      <p className="subtitle">{t.login.subtitle}</p>

      <button className="primary-button" onClick={() => void handleLogin()} disabled={busy}>
        {t.login.loginButton}
      </button>
      {offlineProfile && (
        <button className="secondary-button" onClick={() => onLoggedIn(offlineProfile)} disabled={busy}>
          {t.login.playOffline({ name: offlineProfile.name })}
        </button>
      )}

      {status && <p className="status">{status}</p>}
      {error && <p className="error">{error}</p>}
    </div>
  )
}
