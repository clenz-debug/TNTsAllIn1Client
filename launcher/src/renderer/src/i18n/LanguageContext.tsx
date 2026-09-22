import { createContext, useContext } from 'react'
import type { ReactNode } from 'react'
import type { Language } from '../../../shared/types'
import { de } from './de'
import { en } from './en'

const translations = { de, en }

const LanguageContext = createContext<Language>('de')

interface Props {
  language: Language
  children: ReactNode
}

/** Wraps the whole app (see `App.tsx`) so `language` changes made in the Settings screen re-render
 * every already-migrated screen with the new strings immediately, no reload needed - same "just a
 * React state change" mechanism `theme.ts`'s live color preview already relies on. */
export function LanguageProvider({ language, children }: Props) {
  return <LanguageContext.Provider value={language}>{children}</LanguageContext.Provider>
}

/** Returns the current language's translation dictionary - e.g. `const t = useTranslations();
 * t.settings.back`. Only keys that exist in `de.ts` exist at all (`en.ts` is typed against it), so
 * a typo here is a compile error, not a blank label at runtime. */
export function useTranslations() {
  const language = useContext(LanguageContext)
  return translations[language]
}

/** The raw language code, for the rare case that needs it directly instead of a translated string
 * - e.g. `SkinEditorScreen.tsx`'s default skin name picks `Intl`'s locale (`de-DE`/`en-US`) for its
 * date formatting from this, not from any dictionary key. */
export function useLanguage(): Language {
  return useContext(LanguageContext)
}
