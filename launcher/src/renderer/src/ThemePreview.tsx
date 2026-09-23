import type { CSSProperties } from 'react'
import { useEffect, useRef } from 'react'
import type { ThemeColors } from '../../shared/types'
import { Dropdown } from './Dropdown'
import { useTranslations } from './i18n/LanguageContext'
import { Logo } from './Logo'
import { PlayerMenuButton } from './PlayerMenuButton'
import { themeColorsToCssVars } from './theme'

interface Props {
  colors: ThemeColors
  /** Reports this mockup's actual rendered height in px whenever it changes - lets a caller (the
   * Settings screen's big hero logo next to it) match that height exactly instead of guessing a
   * fixed px value. A plain CSS `height: 100%` doesn't work for this: the logo sits in a flex row
   * whose own height is only as tall as this mockup makes it, so there's no ancestor with a
   * definite height to resolve a percentage against - it resolved against something much larger
   * instead and blew up the logo to a huge size (own user report after trying that first). */
  onHeightChange?: (height: number) => void
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
 * accurately. The `Dropdown` and the player-name `PlayerMenuButton` are both left genuinely
 * interactive (not just a static mockup) - own bonus: opening either one right here shows the
 * previewed accent color's highlighted-row appearance too, not just the closed trigger.
 * `PlayerMenuButton`'s own logout action is a no-op here (`() => undefined`) - there's no real
 * session to end in a color preview - and its head icon renders blank (`headTextureDataUri={null}`)
 * since there's no real skin texture to show either.
 *
 * Uses the real `Logo` component (not a placeholder) - its `var(--...)` fills inherit this
 * wrapper's own overridden custom properties exactly like every other themed element here, so the
 * previewed logo recolors live too, not just the rest of the mockup.
 */
export function ThemePreview({ colors, onHeightChange }: Props) {
  const t = useTranslations()
  const vars = themeColorsToCssVars(colors) as CSSProperties
  const frameRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!onHeightChange) return
    const node = frameRef.current
    if (!node) return
    const observer = new ResizeObserver((entries) => {
      const entry = entries[0]
      if (entry) onHeightChange(entry.contentRect.height)
    })
    observer.observe(node)
    return () => observer.disconnect()
  }, [onHeightChange])

  return (
    <div className="theme-preview-frame" style={vars} ref={frameRef}>
      <div className="play-screen theme-preview-inner">
        <header>
          <div className="identity-row">
            <Logo className="theme-preview-logo" />
            <PlayerMenuButton
              name={t.themePreview.playerName}
              headTextureDataUri={null}
              logoutLabel={t.play.headerLogout}
              menuLabel={t.play.playerMenuLabel}
              onLogout={() => undefined}
            />
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
