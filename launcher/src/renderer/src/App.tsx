import { useEffect, useState } from 'react'
import type { MinecraftProfile } from '../../shared/types'
import { LoginScreen } from './screens/LoginScreen'
import { PlayScreen } from './screens/PlayScreen'

export default function App() {
  const [profile, setProfile] = useState<MinecraftProfile | null>(null)
  const [restoring, setRestoring] = useState(true)

  useEffect(() => {
    window.api
      .restoreSession()
      .then(setProfile)
      .catch(() => setProfile(null))
      .finally(() => setRestoring(false))
  }, [])

  if (restoring) {
    return (
      <div className="centered">
        <p>Sitzung wird geprüft…</p>
      </div>
    )
  }

  return profile ? (
    <PlayScreen profile={profile} onLogout={() => setProfile(null)} />
  ) : (
    <LoginScreen onLoggedIn={setProfile} />
  )
}
