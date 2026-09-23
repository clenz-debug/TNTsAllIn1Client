import type { CSSProperties } from 'react'
import { useState } from 'react'
import { ConfirmDialog } from './ConfirmDialog'
import { useTranslations } from './i18n/LanguageContext'
import { Logo } from './Logo'
import { ColorPicker } from './skinEditor/ColorPicker'
import { ThemePreview } from './ThemePreview'
import type { ThemeColors } from '../../shared/types'
import { applyThemeColors, resolveThemeColors, themeColorsToCssVars, worstTextContrast } from './theme'

/** One row per {@link ThemeColors} field - `key` is what gets edited/merged, `labelKey` picks the
 * translated label out of `t.settings.appearance.fields`. */
const THEME_FIELDS: Array<{ key: keyof ThemeColors; labelKey: keyof ReturnType<typeof useTranslations>['settings']['appearance']['fields'] }> = [
  { key: 'background1', labelKey: 'background1' },
  { key: 'background2', labelKey: 'background2' },
  { key: 'accent1', labelKey: 'accent1' },
  { key: 'accent2', labelKey: 'accent2' },
  { key: 'accent3', labelKey: 'accent3' },
  { key: 'accent4', labelKey: 'accent4' },
  { key: 'text', labelKey: 'text' }
]

interface Props {
  themeColors: ThemeColors | null
  onThemeColorsChange: (value: ThemeColors | null) => void
}

/**
 * Own follow-up extraction (own user request: onboarding's colors step wants "der Preview wie in
 * den Einstellungen") - the interactive part of the Settings screen's "Erscheinungsbild" section
 * (per-field swatch list; the color picker + live preview + contrast badge for whichever field is
 * being edited; reset-to-default with confirmation), pulled out of `SettingsScreen.tsx` so both it
 * and `OnboardingScreen`'s colors step render the exact same already-tested logic instead of two
 * copies that could drift apart. Deliberately doesn't include the section's own heading/description
 * text - those differ by context (Settings' own vs. onboarding's explanation) and stay with
 * whichever screen renders this.
 */
export function AppearanceEditor({ themeColors, onThemeColorsChange }: Props) {
  const t = useTranslations()
  const committedTheme = resolveThemeColors(themeColors)
  // Which field's editor is currently open (null = the plain list of swatches) plus that field's
  // own in-progress value - deliberately never fed into `applyThemeColors` until "Übernehmen":
  // the whole point of a preview/confirm flow ("der User muss manuell bestätigen") is that
  // clicking "Abbrechen" leaves the real, already-applied page exactly as it was, no revert logic
  // needed because nothing outside `ThemePreview`'s own scoped `style` ever saw the draft color.
  const [editingField, setEditingField] = useState<keyof ThemeColors | null>(null)
  const [draftColor, setDraftColor] = useState('#000000')
  // The mockup's own actual rendered height, reported live by ThemePreview - lets the big hero logo
  // next to it match that height exactly (own user request), see ThemePreview.tsx#onHeightChange.
  const [previewHeight, setPreviewHeight] = useState<number | null>(null)
  // Own themed replacement for window.confirm() (ConfirmDialog).
  const [confirmingReset, setConfirmingReset] = useState(false)

  function startEditingField(field: keyof ThemeColors): void {
    setEditingField(field)
    setDraftColor(committedTheme[field])
  }

  function handleConfirmField(): void {
    if (!editingField) return
    const updated: ThemeColors = { ...committedTheme, [editingField]: draftColor }
    applyThemeColors(updated)
    onThemeColorsChange(updated)
    setEditingField(null)
  }

  function handleConfirmResetTheme(): void {
    applyThemeColors(null)
    onThemeColorsChange(null)
    setConfirmingReset(false)
  }

  const previewTheme: ThemeColors = editingField ? { ...committedTheme, [editingField]: draftColor } : committedTheme
  const contrast = worstTextContrast(previewTheme)
  const contrastLevel = contrast < 3 ? 'bad' : contrast < 4.5 ? 'borderline' : 'good'
  const contrastLabel =
    contrast < 3 ? t.settings.appearance.contrastBad : contrast < 4.5 ? t.settings.appearance.contrastBorderline : t.settings.appearance.contrastGood
  const previewVars = themeColorsToCssVars(previewTheme) as CSSProperties

  return (
    <>
      {editingField === null ? (
        <>
          <ul className="instances-list">
            {THEME_FIELDS.map((field) => (
              <li key={field.key} className="instances-row">
                <div className="instance-info">
                  <span className="theme-color-swatch" style={{ background: committedTheme[field.key] }} />
                  <strong>{t.settings.appearance.fields[field.labelKey]}</strong>
                  <span className="instance-version">{committedTheme[field.key]}</span>
                </div>
                <button className="link-button" onClick={() => startEditingField(field.key)}>
                  {t.settings.appearance.change}
                </button>
              </li>
            ))}
          </ul>
          <button className="secondary-button theme-reset-button" onClick={() => setConfirmingReset(true)} disabled={themeColors === null}>
            {t.settings.appearance.resetToDefault}
          </button>
        </>
      ) : (
        <>
          <div className="theme-field-heading-row">
            <h4>{t.settings.appearance.editField(t.settings.appearance.fields[THEME_FIELDS.find((field) => field.key === editingField)!.labelKey])}</h4>
            {/* Non-blocking hint, not a gate - the actual safety mechanism is the pixel-accurate
                preview below plus requiring an explicit "Übernehmen" click (own wishlist item: "da
                muss man aber dafür sorgen dass ... die Schrift sichtbar ist"). A human judging the
                real rendered result below catches cases this single formula would miss. The full
                "worst affected background/accent" explanation lives in the title tooltip instead
                of a permanently-visible sentence (own user report: used to be an unlabeled wall of
                text floating between the picker and the preview). */}
            <span className="contrast-badge" title={t.settings.appearance.contrastExplain}>
              <span>
                {t.settings.appearance.contrastLabel}: {contrast.toFixed(1)}:1
              </span>
              <span className="contrast-badge-verdict" data-level={contrastLevel}>
                {contrastLabel}
              </span>
            </span>
          </div>
          <ColorPicker color={draftColor} onChange={setDraftColor} />

          <div className="theme-preview-intro">
            <h4>{t.settings.appearance.previewHeading}</h4>
            <p className="version-warning">{t.settings.appearance.previewDescription}</p>
          </div>
          <div className="theme-preview-row">
            <ThemePreview colors={previewTheme} onHeightChange={setPreviewHeight} />
            <Logo className="theme-preview-hero-logo" style={previewHeight !== null ? { ...previewVars, height: previewHeight } : previewVars} />
          </div>
          <div className="header-actions theme-edit-actions">
            <button className="secondary-button" onClick={handleConfirmField}>
              {t.settings.appearance.apply}
            </button>
            <button className="link-button" onClick={() => setEditingField(null)}>
              {t.settings.appearance.cancel}
            </button>
          </div>
        </>
      )}

      {confirmingReset && (
        <ConfirmDialog
          message={t.settings.appearance.resetConfirm}
          confirmLabel={t.settings.appearance.resetToDefault}
          cancelLabel={t.common.cancel}
          onConfirm={handleConfirmResetTheme}
          onCancel={() => setConfirmingReset(false)}
        />
      )}
    </>
  )
}
