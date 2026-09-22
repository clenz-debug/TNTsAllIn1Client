import { useEffect, useState } from 'react'
import { Dropdown } from '../Dropdown'
import { formatError } from '../formatError'
import { useTranslations } from '../i18n/LanguageContext'
import { ColorPicker } from '../skinEditor/ColorPicker'
import { ThemePreview } from '../ThemePreview'
import type { Language, StorageInfo, StorageMoveProgressEvent, SystemMemoryInfo, ThemeColors } from '../../../shared/types'
import { applyThemeColors, resolveThemeColors, worstTextContrast } from '../theme'

function formatBytes(bytes: number): string {
  const gb = bytes / 1024 ** 3
  return gb >= 1 ? `${gb.toFixed(1)} GB` : `${(bytes / 1024 ** 2).toFixed(0)} MB`
}

/** Fallback used only while `getSystemMemoryInfo()` hasn't resolved yet (or failed) - large enough
 * that the "Max RAM" number input isn't stuck at some tiny, clearly-wrong ceiling in the meantime. */
const FALLBACK_SYSTEM_MEMORY_MB = 16384

const MIN_MEMORY_MB = 512

/** Pre-filled the instant a user opts into manual RAM control (never applied silently while
 * "Automatisch" stays checked) - 4096 MB is a generous but not excessive default for a
 * Sodium/Lithium-optimized, lightly-modded client like this one (vanilla's own launcher itself
 * only ever defaults new installations to `-Xmx2G`). Only backed off below that on genuinely
 * low-RAM systems, so half the detected total is never exceeded and the OS keeps some headroom. */
const RECOMMENDED_MAX_MEMORY_MB = 4096

/** One row per {@link ThemeColors} field in the Settings screen's "Erscheinungsbild" list -
 * `key` is what gets edited/merged, `labelKey` picks the translated label out of
 * `t.settings.appearance.fields`. */
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
  showSnapshots: boolean
  onShowSnapshotsChange: (value: boolean) => void
  maxMemoryMb: number | null
  onMaxMemoryMbChange: (value: number | null) => void
  consoleInSeparateWindow: boolean
  onConsoleInSeparateWindowChange: (value: boolean) => void
  themeColors: ThemeColors | null
  onThemeColorsChange: (value: ThemeColors | null) => void
  language: Language
  onLanguageChange: (value: Language) => void
  onDataRootOverrideChange: (path: string) => void
  onClose: () => void
}

/**
 * General launcher settings (own wishlist item: "Einstellungsbereich im Launcher wo man wichtige
 * Sachen einstellen kann") - a home for launcher-wide options that either had no UI at all yet
 * (RAM, console window) or previously lived awkwardly inside the Instances screen despite not
 * being per-instance (Speicherort, Snapshots anzeigen - moved here, not duplicated).
 *
 * The first (and so far only) screen migrated to the `i18n` system (own wishlist item: "englisch
 * und deutsch, aber erweiterbar in der Zukunft") - every other screen is still hardcoded German
 * regardless of the `language` setting until it's migrated too.
 */
export function SettingsScreen({
  showSnapshots,
  onShowSnapshotsChange,
  maxMemoryMb,
  onMaxMemoryMbChange,
  consoleInSeparateWindow,
  onConsoleInSeparateWindowChange,
  themeColors,
  onThemeColorsChange,
  language,
  onLanguageChange,
  onDataRootOverrideChange,
  onClose
}: Props) {
  const t = useTranslations()

  const [systemMemory, setSystemMemory] = useState<SystemMemoryInfo | null>(null)
  useEffect(() => {
    window.api
      .getSystemMemoryInfo()
      .then(setSystemMemory)
      .catch(() => undefined)
  }, [])
  const totalMemoryMb = systemMemory?.totalMb ?? FALLBACK_SYSTEM_MEMORY_MB

  const [error, setError] = useState<string | null>(null)
  const [storageInfo, setStorageInfo] = useState<StorageInfo | null>(null)
  const [moveProgress, setMoveProgress] = useState<StorageMoveProgressEvent | null>(null)
  const [movingStorage, setMovingStorage] = useState(false)

  useEffect(() => {
    window.api
      .getStorageInfo()
      .then(setStorageInfo)
      .catch(() => undefined)
  }, [])

  async function handleChangeStorageLocation(): Promise<void> {
    setMovingStorage(true)
    setMoveProgress(null)
    const unsubscribe = window.api.onStorageMoveProgress(setMoveProgress)
    try {
      const result = await window.api.changeStorageLocation()
      if (result) {
        const info = await window.api.getStorageInfo()
        setStorageInfo(info)
        onDataRootOverrideChange(result.path)
      }
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      unsubscribe()
      setMovingStorage(false)
      setMoveProgress(null)
    }
  }

  const memoryAuto = maxMemoryMb === null

  function handleMemoryAutoChange(auto: boolean): void {
    const halfSystemMemory = Math.round(totalMemoryMb / 2 / 512) * 512
    const suggested = Math.min(RECOMMENDED_MAX_MEMORY_MB, halfSystemMemory)
    onMaxMemoryMbChange(auto ? null : Math.max(MIN_MEMORY_MB, suggested))
  }

  function commitMaxMemory(text: string): void {
    const value = Number(text)
    if (!Number.isFinite(value)) return
    onMaxMemoryMbChange(Math.min(totalMemoryMb, Math.max(MIN_MEMORY_MB, Math.round(value))))
  }

  const committedTheme = resolveThemeColors(themeColors)
  // Which field's editor is currently open (null = the plain list of swatches) plus that field's
  // own in-progress value - deliberately never fed into `applyThemeColors` until "Übernehmen":
  // the whole point of a preview/confirm flow ("der User muss manuell bestätigen") is that
  // clicking "Abbrechen" leaves the real, already-applied page exactly as it was, no revert logic
  // needed because nothing outside `ThemePreview`'s own scoped `style` ever saw the draft color.
  const [editingField, setEditingField] = useState<keyof ThemeColors | null>(null)
  const [draftColor, setDraftColor] = useState('#000000')

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

  function handleResetTheme(): void {
    const confirmed = window.confirm(t.settings.appearance.resetConfirm)
    if (!confirmed) return
    applyThemeColors(null)
    onThemeColorsChange(null)
  }

  const previewTheme: ThemeColors = editingField ? { ...committedTheme, [editingField]: draftColor } : committedTheme
  const contrast = worstTextContrast(previewTheme)
  const contrastLabel =
    contrast < 3 ? t.settings.appearance.contrastBad : contrast < 4.5 ? t.settings.appearance.contrastBorderline : t.settings.appearance.contrastGood

  return (
    <div className="instances-screen">
      <header>
        <strong>{t.settings.title}</strong>
        <button className="link-button" onClick={onClose}>
          {t.settings.back}
        </button>
      </header>

      {error && <span className="error">{error}</span>}

      <section className="instances-section">
        <h3>{t.settings.memory.heading}</h3>
        <label className="checkbox-label">
          <input type="checkbox" checked={memoryAuto} onChange={(e) => handleMemoryAutoChange(e.target.checked)} />
          {t.settings.memory.auto}
        </label>
        {!memoryAuto && (
          <div className="instance-create-form">
            <span className="instance-name-label">{t.settings.memory.maxLabel}</span>
            {/* `defaultValue` + commit-on-blur/Enter, not a controlled `value`/`onChange` - clamping
                on every keystroke (the first version of this field did that) snaps the field back
                mid-typing, so e.g. typing "7000" digit-by-digit could end up clamped to some other
                number entirely before the last digit is even typed. Same pattern as ColorPicker's
                own hex/RGB fields below, for the same reason. */}
            <input
              type="number"
              className="instance-name-input"
              min={MIN_MEMORY_MB}
              max={totalMemoryMb}
              step={512}
              defaultValue={maxMemoryMb ?? MIN_MEMORY_MB}
              key={maxMemoryMb ?? MIN_MEMORY_MB}
              onBlur={(e) => commitMaxMemory(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') commitMaxMemory(e.currentTarget.value)
              }}
            />
            <span className="instance-version">{t.settings.memory.unit}</span>
          </div>
        )}
        {systemMemory && (
          <span className="instance-version">{t.settings.memory.systemDetected(formatBytes(systemMemory.totalMb * 1024 ** 2))}</span>
        )}
      </section>

      <section className="instances-section">
        <h3>{t.settings.storage.heading}</h3>
        <div className="instance-info">
          <span>{storageInfo?.path ?? t.settings.storage.loading}</span>
          {storageInfo?.freeBytes != null && (
            <span className="instance-version">{t.settings.storage.free(formatBytes(storageInfo.freeBytes))}</span>
          )}
        </div>
        <button
          className="secondary-button"
          onClick={() => void handleChangeStorageLocation()}
          disabled={movingStorage}
        >
          {t.settings.storage.change}
        </button>
        {movingStorage && moveProgress && (
          <div className="progress">
            <span>
              {moveProgress.subfolder}
              {moveProgress.label ? ` — ${moveProgress.label}` : ''} ({moveProgress.completed}/{moveProgress.total})
            </span>
            <progress value={moveProgress.completed} max={Math.max(moveProgress.total, 1)} />
          </div>
        )}
      </section>

      <section className="instances-section">
        <h3>{t.settings.instances.heading}</h3>
        <label className="checkbox-label">
          <input type="checkbox" checked={showSnapshots} onChange={(e) => onShowSnapshotsChange(e.target.checked)} />
          {t.settings.instances.showSnapshots}
        </label>
      </section>

      <section className="instances-section">
        <h3>{t.settings.console.heading}</h3>
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={consoleInSeparateWindow}
            onChange={(e) => onConsoleInSeparateWindowChange(e.target.checked)}
          />
          {t.settings.console.separateWindow}
        </label>
      </section>

      <section className="instances-section">
        <h3>{t.settings.appearance.heading}</h3>
        <p className="version-warning">{t.settings.appearance.description}</p>

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
            <button className="secondary-button theme-reset-button" onClick={handleResetTheme} disabled={themeColors === null}>
              {t.settings.appearance.resetToDefault}
            </button>
          </>
        ) : (
          <>
            <h4>{t.settings.appearance.editField(t.settings.appearance.fields[THEME_FIELDS.find((field) => field.key === editingField)!.labelKey])}</h4>
            <ColorPicker color={draftColor} onChange={setDraftColor} />
            {/* Non-blocking hint, not a gate - the actual safety mechanism is the pixel-accurate
                preview below plus requiring an explicit "Übernehmen" click (own wishlist item: "da
                muss man aber dafür sorgen dass ... die Schrift sichtbar ist"). A human judging the
                real rendered result below catches cases this single formula would miss. */}
            <p className="version-warning">{t.settings.appearance.contrastHint(contrast.toFixed(1), contrastLabel)}</p>
            <ThemePreview colors={previewTheme} />
            <div className="header-actions">
              <button className="secondary-button" onClick={handleConfirmField}>
                {t.settings.appearance.apply}
              </button>
              <button className="link-button" onClick={() => setEditingField(null)}>
                {t.settings.appearance.cancel}
              </button>
            </div>
          </>
        )}
      </section>

      <section className="instances-section">
        <h3>{t.settings.language.heading}</h3>
        <Dropdown
          value={language}
          onChange={(value) => onLanguageChange(value as Language)}
          options={[
            { value: 'de', label: t.settings.language.german },
            { value: 'en', label: t.settings.language.english }
          ]}
        />
      </section>
    </div>
  )
}
