import type { CSSProperties } from 'react'
import type { ThemeColors } from '../../shared/types'
import { Dropdown } from './Dropdown'
import { useTranslations } from './i18n/LanguageContext'
import { Logo } from './Logo'
import { themeColorsToCssVars } from './theme'

interface Props {
  colors: ThemeColors
}

/**
 * Faithful mockup of the Play screen's "Startbereich" (own wishlist item: show exactly this
 * before any color change is confirmed) - reuses the exact same CSS classes `PlayScreen` itself
 * renders with (`play-screen`, `header`, `link-button`, `primary-button play-button`, `log-panel`,
 * ...) inside a wrapper that overrides the theme's CSS custom properties only for this subtree
 * (via `style`, not `document.documentElement`), so it never touches the real page's own theme
 * while a color is still just being tried out in `SettingsScreen`'s editor.
 *
 * No `disabled` attributes on the mockup's buttons - `.primary-button:disabled` dims to 60%
 * opacity in `global.css`, which would misrepresent exactly the color this preview exists to show
 * accurately. Nothing here has an `onClick` besides the `Dropdown` itself, so the rest is already
 * inert without it. The `Dropdown` is left genuinely interactive (not just a static mockup) - own
 * bonus: it lets a user opening it right here see the previewed accent color's highlighted-row
 * appearance too, not just the closed trigger.
 *
 * Uses the real `Logo` component (not a placeholder) - its `var(--...)` fills inherit this
 * wrapper's own overridden custom properties exactly like every other themed element here, so the
 * previewed logo recolors live too, not just the rest of the mockup.
 */
export function ThemePreview({ colors }: Props) {
  const t = useTranslations()
  const vars = themeColorsToCssVars(colors) as CSSProperties

  return (
    <div className="theme-preview-frame" style={vars}>
      <div className="play-screen theme-preview-inner">
        <header>
          <div className="identity-row">
            <Logo className="theme-preview-logo" />
            <strong>{t.themePreview.playerName}</strong>
          </div>
          <div className="header-actions">
            <button className="link-button">{t.play.headerSkin}</button>
            <button className="link-button">{t.play.headerSettings}</button>
          </div>
        </header>

        <div className="version-picker">
          <label>{t.play.instanceLabel}</label>
          <Dropdown value="example" onChange={() => undefined} options={[{ value: 'example', label: t.themePreview.exampleInstance }]} />
          <button className="secondary-button">{t.play.mods}</button>
        </div>

        <button className="primary-button play-button">{t.play.play}</button>

        <pre className="log-panel theme-preview-log">
          <div className="log-info">{t.themePreview.sampleLogInfo}</div>
          <div className="log-error">{t.themePreview.sampleLogError}</div>
        </pre>
      </div>
    </div>
  )
}
