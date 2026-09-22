import { useEffect, useState } from 'react'
import type { Language, MinecraftProfile } from '../../shared/types'
import { LanguageProvider, useTranslations } from './i18n/LanguageContext'
import { LoginScreen } from './screens/LoginScreen'
import { PlayScreen } from './screens/PlayScreen'
import { applyThemeColors } from './theme'

/** A real descendant of `LanguageProvider`, not inlined into `App` itself - `useTranslations()`
 * reads from React context, which only ever sees values from an *ancestor* provider, never one
 * the same component instance is about to render for its own children. */
function CheckingSession() {
  const t = useTranslations()
  return (
    <div className="centered">
      <p>{t.app.checkingSession}</p>
    </div>
  )
}

export default function App() {
  const [profile, setProfile] = useState<MinecraftProfile | null>(null)
  const [restoring, setRestoring] = useState(true)
  // Owned here (not inside PlayScreen) so `LanguageProvider` can wrap LoginScreen too, ready for
  // when it gets migrated - PlayScreen still owns *persisting* it, via the `onLanguageChange`
  // prop threaded down to it, since `saveSettings` takes the whole settings object and PlayScreen
  // already holds every other field of it.
  const [language, setLanguage] = useState<Language>('de')

  useEffect(() => {
    window.api
      .restoreSession()
      .then(setProfile)
      .catch(() => setProfile(null))
      .finally(() => setRestoring(false))
  }, [])

  // Applied/read once here (covers LoginScreen too, not just PlayScreen and its subscreens) - the
  // Settings screen re-applies/persists both live on every change itself, this just restores them
  // on startup.
  useEffect(() => {
    window.api
      .loadSettings()
      .then((settings) => {
        applyThemeColors(settings.themeColors)
        setLanguage(settings.language)
      })
      .catch(() => undefined)
  }, [])

  return (
    <LanguageProvider language={language}>
      {restoring ? (
        <CheckingSession />
      ) : profile ? (
        <PlayScreen
          profile={profile}
          onProfileUpdate={setProfile}
          onLogout={() => setProfile(null)}
          language={language}
          onLanguageChange={setLanguage}
        />
      ) : (
        <LoginScreen onLoggedIn={setProfile} />
      )}
    </LanguageProvider>
  )
}
