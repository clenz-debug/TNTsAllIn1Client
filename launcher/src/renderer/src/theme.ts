/** Settings screen's "Erscheinungsbild" section (own wishlist item, expanded on request from a
 * single derived accent color to seven independently pickable colors) - re-themes the app's
 * `--bg`, `--bg-panel`, `--green-1..4`, `--text` (and the always-derived `--text-dim`) CSS custom
 * properties from a full {@link ThemeColors} object instead of a full light/dark theme system. */

import { DEFAULT_THEME_COLORS, type ThemeColors } from '../../shared/types'

export function resolveThemeColors(colors: ThemeColors | null): ThemeColors {
  return colors ?? DEFAULT_THEME_COLORS
}

function hexToRgb(hex: string): [number, number, number] {
  const match = /^#?([0-9a-f]{6})$/i.exec(hex.trim())
  if (!match) return [0, 0, 0]
  const value = parseInt(match[1], 16)
  return [(value >> 16) & 255, (value >> 8) & 255, value & 255]
}

function mix(hexA: string, hexB: string, amount: number): string {
  const a = hexToRgb(hexA)
  const b = hexToRgb(hexB)
  const rgb = a.map((channel, i) => Math.round(channel + (b[i] - channel) * amount))
  return `#${rgb.map((c) => c.toString(16).padStart(2, '0')).join('')}`
}

/** `--text-dim`'s always-derived value - 40% of the way from the picked `text` color toward this
 * theme's own `background1`, the same relationship the hardcoded defaults already have (`#a9c4a9`
 * sits about that far between `#e9f5e9` and `#0d1f0d`). Not a separately configurable color - see
 * `ThemeColors`'s own doc comment for why. */
function deriveDimText(colors: ThemeColors): string {
  return mix(colors.text, colors.background1, 0.4)
}

/** The full set of CSS custom property values a given (possibly still only previewed, not yet
 * confirmed) theme produces - used both to actually theme the page ({@link applyThemeColors}) and,
 * scoped to a wrapper element's own `style` instead of `:root`, to render `ThemePreview` without
 * touching the real page's theme while a color is still just being tried out. */
export function themeColorsToCssVars(colors: ThemeColors): Record<string, string> {
  return {
    '--bg': colors.background1,
    '--bg-panel': colors.background2,
    '--green-1': colors.accent1,
    '--green-2': colors.accent2,
    '--green-3': colors.accent3,
    '--green-4': colors.accent4,
    '--text': colors.text,
    '--text-dim': deriveDimText(colors)
  }
}

const THEMED_CSS_PROPERTIES = ['--bg', '--bg-panel', '--green-1', '--green-2', '--green-3', '--green-4', '--text', '--text-dim']

/** Applies (`colors` non-null) or clears (`null`) the theme as inline overrides on `:root` -
 * clearing falls back to `global.css`'s own hardcoded defaults. Called once at startup (`App.tsx`)
 * and again immediately after every *confirmed* (not merely previewed) change in the Settings
 * screen - a color being tried out in `ThemePreview` never reaches here until "Übernehmen". */
export function applyThemeColors(colors: ThemeColors | null): void {
  const root = document.documentElement.style
  if (colors === null) {
    for (const property of THEMED_CSS_PROPERTIES) root.removeProperty(property)
    return
  }
  for (const [property, value] of Object.entries(themeColorsToCssVars(colors))) root.setProperty(property, value)
}

function srgbToLinear(channel: number): number {
  const c = channel / 255
  return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4)
}

function relativeLuminance(hex: string): number {
  const [r, g, b] = hexToRgb(hex)
  return 0.2126 * srgbToLinear(r) + 0.7152 * srgbToLinear(g) + 0.0722 * srgbToLinear(b)
}

/** WCAG 2.x contrast ratio (1:1 to 21:1) between two colors. */
export function contrastRatio(hexA: string, hexB: string): number {
  const a = relativeLuminance(hexA)
  const b = relativeLuminance(hexB)
  const lighter = Math.max(a, b)
  const darker = Math.min(a, b)
  return (lighter + 0.05) / (darker + 0.05)
}

/** The worst (lowest) contrast ratio between `text` and every color it's actually rendered on top
 * of elsewhere in the app - `background1`/`background2` (nearly everything) plus `accent1`/
 * `accent3`/`accent4` (the three accent shades `global.css` also uses as button backgrounds with
 * `--text` on top; `accent2` never hosts text, only ever a border, so it's excluded).
 *
 * Own wishlist item's actual safety mechanism is the pixel-accurate `ThemePreview` plus requiring
 * an explicit "Übernehmen" click - a human judging the real rendered result catches cases a single
 * formula would miss. This number is a non-blocking hint alongside that preview, not a gate: nothing
 * here stops a user from confirming a low-contrast combination if that preview still looks fine to
 * them (e.g. a large heading vs. small body text has very different real-world readability needs). */
export function worstTextContrast(colors: ThemeColors): number {
  const backgrounds = [colors.background1, colors.background2, colors.accent1, colors.accent3, colors.accent4]
  return Math.min(...backgrounds.map((bg) => contrastRatio(colors.text, bg)))
}
