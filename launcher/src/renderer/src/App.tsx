import { useEffect, useState } from 'react'
import type { ClientDesign, Language, MinecraftProfile, ThemeColors } from '../../shared/types'
import { LanguageProvider, useTranslations } from './i18n/LanguageContext'
import { LoginScreen } from './screens/LoginScreen'
import { OnboardingScreen } from './screens/OnboardingScreen'
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
  // Same "owned here, PlayScreen persists it" split as `language` above - only actually read/written
  // outside PlayScreen by `OnboardingScreen`'s colors step, which needs it live so `AppearanceEditor`
  // (rendered from both places) always edits/previews the one real in-memory value.
  const [themeColors, setThemeColors] = useState<ThemeColors | null>(null)
  const [clientDesign, setClientDesign] = useState<ClientDesign>('minecraft')
  // Starts `false` (same default as `DEFAULT_LAUNCHER_SETTINGS`) - stays that way, showing
  // `OnboardingScreen`, until the settings-load effect below resolves either way. `settingsLoaded`
  // gates rendering on it below so a brand-new install never flashes Login/PlayScreen first.
  const [onboardingCompleted, setOnboardingCompleted] = useState(false)
  const [settingsLoaded, setSettingsLoaded] = useState(false)

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
        setThemeColors(settings.themeColors)
        setOnboardingCompleted(settings.onboardingCompleted)
      })
      .catch(() => undefined)
      .finally(() => setSettingsLoaded(true))
  }, [])

  // Own wishlist item ("client setup beim ersten start") - fires once, after the colors step
  // (skipped or not); `language`/`themeColors` are already the current, live values by then, both
  // updated as-you-go during the language/colors steps (`onLanguageChange`/`onThemeColorsChange`
  // below), not passed in here. Re-reads settings rather than trusting only what this component
  // still has in memory: `saveSettings` overwrites the whole file (see
  // `main/launcherSettings.ts#saveLauncherSettings`) and PlayScreen owns several other fields of it
  // this component never loads at all, so merging onto a guaranteed-fresh read avoids clobbering
  // those with stale/absent values.
  async function handleOnboardingComplete(): Promise<void> {
    const settings = await window.api.loadSettings()
    await window.api.saveSettings({ ...settings, language, themeColors, clientDesign, onboardingCompleted: true })
    setOnboardingCompleted(true)
  }

  return (
    <LanguageProvider language={language}>
      {restoring || !settingsLoaded ? (
        <CheckingSession />
      ) : !onboardingCompleted ? (
        <OnboardingScreen
          language={language}
          onLanguageChange={setLanguage}
          themeColors={themeColors}
          onThemeColorsChange={setThemeColors}
          clientDesign={clientDesign}
          onClientDesignChange={setClientDesign}
          onComplete={() => void handleOnboardingComplete()}
        />
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
