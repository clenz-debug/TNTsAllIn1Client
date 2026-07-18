import { useState } from 'react'
import type { AuthProgressEvent, MinecraftProfile } from '../../../shared/types'

interface Props {
  onLoggedIn: (profile: MinecraftProfile) => void
}

export function LoginScreen({ onLoggedIn }: Props) {
  const [status, setStatus] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function handleLogin(): Promise<void> {
    setBusy(true)
    setError(null)
    const unsubscribe = window.api.onAuthProgress((event: AuthProgressEvent) => setStatus(event.message))
    try {
      const profile = await window.api.login()
      onLoggedIn(profile)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      unsubscribe()
      setBusy(false)
    }
  }

  async function handleMockLogin(): Promise<void> {
    setBusy(true)
    setError(null)
    try {
      const profile = await window.api.loginMock()
      onLoggedIn(profile)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-screen">
      <h1>TNT&apos;s All-In-1 Client</h1>
      <p className="subtitle">Mit deinem Microsoft-Account anmelden, um zu spielen.</p>

      <button className="primary-button" onClick={() => void handleLogin()} disabled={busy}>
        Mit Microsoft anmelden
      </button>

      {import.meta.env.DEV && (
        <button className="secondary-button" onClick={() => void handleMockLogin()} disabled={busy}>
          Skip Login (Dev-Mock — Mojang-API noch nicht freigeschaltet)
        </button>
      )}

      {status && <p className="status">{status}</p>}
      {error && <p className="error">{error}</p>}
    </div>
  )
}
