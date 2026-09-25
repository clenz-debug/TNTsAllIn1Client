import { useEffect, useState } from 'react'
import { AppearanceEditor } from '../AppearanceEditor'
import { Dropdown } from '../Dropdown'
import { formatBytes } from '../formatBytes'
import { formatError } from '../formatError'
import { useTranslations } from '../i18n/LanguageContext'
import type { ClientDesign, Language, StorageInfo, StorageMoveProgressEvent, SystemMemoryInfo, ThemeColors } from '../../../shared/types'

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
  clientDesign: ClientDesign
  onClientDesignChange: (value: ClientDesign) => void
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
  clientDesign,
  onClientDesignChange,
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

  // "About": the launcher's own version - the installer's file name no longer shows it (own user request).
  const [appVersion, setAppVersion] = useState<string | null>(null)
  useEffect(() => {
    void window.api.getAppVersion().then(setAppVersion)
  }, [])

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
          <input type="checkbox" className="toggle-switch" checked={memoryAuto} onChange={(e) => handleMemoryAutoChange(e.target.checked)} />
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
          <input type="checkbox" className="toggle-switch" checked={showSnapshots} onChange={(e) => onShowSnapshotsChange(e.target.checked)} />
          {t.settings.instances.showSnapshots}
        </label>
      </section>

      <section className="instances-section">
        <h3>{t.settings.console.heading}</h3>
        <label className="checkbox-label">
          <input
            type="checkbox"
            className="toggle-switch"
            checked={consoleInSeparateWindow}
            onChange={(e) => onConsoleInSeparateWindowChange(e.target.checked)}
          />
          {t.settings.console.separateWindow}
        </label>
      </section>

      <section className="instances-section">
        <h3>{t.settings.appearance.heading}</h3>
        <p className="version-warning">{t.settings.appearance.description}</p>
        <AppearanceEditor themeColors={themeColors} onThemeColorsChange={onThemeColorsChange} />
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

      <section className="instances-section">
        <h3>{t.settings.clientDesign.heading}</h3>
        <p className="version-warning">{t.settings.clientDesign.description}</p>
        <Dropdown
          value={clientDesign}
          onChange={(value) => onClientDesignChange(value as ClientDesign)}
          options={[
            { value: 'minecraft', label: t.settings.clientDesign.minecraft },
            { value: 'client', label: t.settings.clientDesign.client }
          ]}
        />
      </section>

      <section className="instances-section">
        <h3>{t.settings.about.heading}</h3>
        <p className="version-warning">{appVersion ? t.settings.about.version(appVersion) : t.common.loading}</p>
      </section>
    </div>
  )
}
