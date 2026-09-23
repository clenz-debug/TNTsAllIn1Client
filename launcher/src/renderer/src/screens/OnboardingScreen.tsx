import { useState } from 'react'
import { AppearanceEditor } from '../AppearanceEditor'
import { Dropdown } from '../Dropdown'
import { useTranslations } from '../i18n/LanguageContext'
import { Logo } from '../Logo'
import type { Language, ThemeColors } from '../../../shared/types'

interface Props {
  /** Current in-memory language - still the plain `'de'` default until this screen's own language
   * step runs, or whatever was already saved for a returning-but-not-yet-onboarded install. Also
   * used to preselect the language dropdown. */
  language: Language
  /** Bubbles up to `App.tsx`'s own `language` state *immediately* on the language step's "Weiter" -
   * not deferred to `onComplete` - so the colors step right after can render via the normal
   * `useTranslations()`/`t.*` system in the just-picked language instead of staying bilingual too. */
  onLanguageChange: (language: Language) => void
  themeColors: ThemeColors | null
  onThemeColorsChange: (value: ThemeColors | null) => void
  /** Called once, after the colors step (whether skipped or actually customized) - `App.tsx`
   * persists `language`/`themeColors`/`onboardingCompleted` together at that point and this screen
   * unmounts, falling through to the normal Login/PlayScreen flow ("dann kommt man weiter zum
   * Bereich wo man sich anmelden muss", own user request) - no separate login step lives inside
   * onboarding itself, `LoginScreen` already *is* that step once `onboardingCompleted` is true and
   * there's no profile yet. */
  onComplete: () => void
}

type Step = 'welcome' | 'language' | 'colors'

/**
 * Own wishlist item ("client setup beim ersten start") - shown once, before the very first login,
 * gated by `LauncherSettings.onboardingCompleted` (see that field's own doc comment). Three steps
 * so far (welcome, language, colors) - more slot into the {@link Step} union and the switch below
 * as they get specified.
 *
 * <p>The welcome and language steps deliberately do *not* use `useTranslations()`/`t.*` like every
 * other screen - they run before the language is even chosen, so unlike anywhere else in the app
 * they have to show *both* languages at once rather than picking one. Own user-reviewed copy
 * (German checked and revised together, English translated to match) is inlined directly below
 * instead of going through `de.ts`/`en.ts`, which are built around exactly the opposite assumption
 * (one active language, picked ahead of time). The colors step runs *after* the language step's own
 * `onLanguageChange` already fired, so it switches to the normal single-language `t.*` system like
 * every other post-onboarding screen.
 */
export function OnboardingScreen({ language, onLanguageChange, themeColors, onThemeColorsChange, onComplete }: Props) {
  const [step, setStep] = useState<Step>('welcome')
  const [selectedLanguage, setSelectedLanguage] = useState<Language>(language)

  if (step === 'welcome') {
    return (
      <div className="login-screen">
        <Logo className="login-logo" />
        <h1>TNT&apos;s All-In-1 Client</h1>
        <div className="onboarding-text">
          <p>
            Willkommen bei TNT&apos;s All-In-1 Client, einem Launcher, der es dir ermöglicht, viele Einstellungen vorzunehmen, die
            dein Spielerlebnis in Minecraft angenehmer machen. Bevor du den Client verwenden kannst, musst du noch ein paar
            Einstellungen vornehmen, damit sich der Client an dich und deine Vorlieben anpassen kann.
          </p>
          <p>
            Welcome to TNT&apos;s All-In-1 Client, a launcher that lets you configure a wide range of settings to make your
            Minecraft experience more enjoyable. Before you can use the client, you&apos;ll need to go through a few more
            settings so it can adapt to you and your preferences.
          </p>
        </div>
        <button className="primary-button" onClick={() => setStep('language')}>
          Weiter / Next
        </button>
      </div>
    )
  }

  if (step === 'language') {
    return (
      <div className="login-screen">
        <Logo className="login-logo" />
        <h1>TNT&apos;s All-In-1 Client</h1>
        <p className="subtitle">Sprache wählen / Choose your language</p>
        <Dropdown
          ariaLabel="Sprache / Language"
          value={selectedLanguage}
          onChange={(value) => setSelectedLanguage(value as Language)}
          options={[
            { value: 'de', label: 'Deutsch' },
            { value: 'en', label: 'English' }
          ]}
        />
        <button
          className="primary-button"
          onClick={() => {
            onLanguageChange(selectedLanguage)
            setStep('colors')
          }}
        >
          Weiter / Next
        </button>
      </div>
    )
  }

  return <OnboardingColorsStep themeColors={themeColors} onThemeColorsChange={onThemeColorsChange} onComplete={onComplete} />
}

/** Split out from the `step === 'colors'` branch above purely so `useTranslations()` can be called
 * at all - it reads `LanguageProvider`'s context, which by this point already reflects the language
 * step's `onLanguageChange` call, but a hook still can't be called conditionally inside the same
 * component body as the two bilingual steps above it. */
function OnboardingColorsStep({ themeColors, onThemeColorsChange, onComplete }: Pick<Props, 'themeColors' | 'onThemeColorsChange' | 'onComplete'>) {
  const t = useTranslations()
  return (
    <div className="login-screen">
      <Logo className="login-logo" />
      <h1>TNT&apos;s All-In-1 Client</h1>
      <p className="subtitle">{t.onboarding.colorsHeading}</p>
      <div className="onboarding-text">
        <p>{t.onboarding.colorsExplanation}</p>
      </div>
      <div className="onboarding-appearance">
        <AppearanceEditor themeColors={themeColors} onThemeColorsChange={onThemeColorsChange} />
      </div>
      <div className="header-actions onboarding-actions">
        <button className="secondary-button" onClick={onComplete}>
          {t.onboarding.skip}
        </button>
        <button className="primary-button" onClick={onComplete}>
          {t.onboarding.next}
        </button>
      </div>
    </div>
  )
}
