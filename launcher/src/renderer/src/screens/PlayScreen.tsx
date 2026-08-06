import { useEffect, useState } from 'react'
import type {
  GameLogEvent,
  GameVersionSummary,
  LaunchProgressEvent,
  MinecraftProfile,
  UpdateCheckResult
} from '../../../shared/types'
import { isBundleCompatibleVersion, MINECRAFT_VERSION } from '../../../shared/types'
import { CreditsScreen } from './CreditsScreen'
import { ModsScreen } from './ModsScreen'

interface Props {
  profile: MinecraftProfile
  onLogout: () => void
}

/** Prefers the bundle-pinned version if it's in the list (should always be, it's a stable
 * release), otherwise falls back to the newest entry so the dropdown never starts empty. */
function pickDefaultVersion(list: GameVersionSummary[]): string {
  if (list.some((v) => v.id === MINECRAFT_VERSION)) return MINECRAFT_VERSION
  return list[0]?.id ?? MINECRAFT_VERSION
}

export function PlayScreen({ profile, onLogout }: Props) {
  const [busy, setBusy] = useState(false)
  const [progress, setProgress] = useState<LaunchProgressEvent | null>(null)
  const [logs, setLogs] = useState<GameLogEvent[]>([])
  const [showCredits, setShowCredits] = useState(false)
  const [showMods, setShowMods] = useState(false)

  const [versions, setVersions] = useState<GameVersionSummary[]>([])
  const [versionsError, setVersionsError] = useState<string | null>(null)
  const [showSnapshots, setShowSnapshots] = useState(false)
  const [selectedVersion, setSelectedVersion] = useState(MINECRAFT_VERSION)
  const [enabledBundledMods, setEnabledBundledMods] = useState<string[]>([])
  // Gates the save-effect below until the persisted settings have actually been applied - without
  // this, that effect's first run (on mount, still holding the plain useState defaults above)
  // would immediately overwrite whatever was saved from a previous session with those defaults.
  const [settingsLoaded, setSettingsLoaded] = useState(false)

  useEffect(() => {
    Promise.all([window.api.loadSettings(), window.api.listVersions()])
      .then(([settings, list]) => {
        setVersions(list)
        setShowSnapshots(settings.showSnapshots)
        setEnabledBundledMods(settings.enabledBundledMods)
        const visible = list.filter((v) => settings.showSnapshots || v.type === 'release')
        const persistedIsVisible = visible.some((v) => v.id === settings.selectedVersion)
        setSelectedVersion(persistedIsVisible ? settings.selectedVersion : pickDefaultVersion(visible))
        setSettingsLoaded(true)
      })
      .catch((err) => setVersionsError(err instanceof Error ? err.message : String(err)))
  }, [])

  useEffect(() => {
    if (!settingsLoaded) return
    void window.api.saveSettings({ selectedVersion, showSnapshots, enabledBundledMods })
  }, [settingsLoaded, selectedVersion, showSnapshots, enabledBundledMods])

  function handleToggleBundledMod(fileName: string, enabled: boolean): void {
    setEnabledBundledMods((prev) => (enabled ? [...prev, fileName] : prev.filter((f) => f !== fileName)))
  }

  const [updateInfo, setUpdateInfo] = useState<UpdateCheckResult | null>(null)
  const [updateDismissed, setUpdateDismissed] = useState(false)

  useEffect(() => {
    // Purely informational, so a failed check (offline, manifest unreachable) just means no
    // banner shows - never worth surfacing as an error to the user the way a failed version-list
    // fetch is, since nothing they'd want to do depends on it.
    window.api
      .checkForUpdate()
      .then(setUpdateInfo)
      .catch(() => undefined)
  }, [])

  const visibleVersions = versions.filter((v) => showSnapshots || v.type === 'release')

  useEffect(() => {
    if (visibleVersions.length === 0) return
    if (!visibleVersions.some((v) => v.id === selectedVersion)) {
      setSelectedVersion(pickDefaultVersion(visibleVersions))
    }
    // Deliberately keyed on the joined id list, not `visibleVersions`/`selectedVersion` directly -
    // only re-run when the visible set itself changes (snapshot toggle / initial load), not on
    // every selectedVersion change, which would fight the user's own dropdown pick.
  }, [visibleVersions.map((v) => v.id).join(',')])

  async function handlePlay(): Promise<void> {
    setBusy(true)
    setLogs([])
    setProgress(null)
    const unsubscribeProgress = window.api.onLaunchProgress(setProgress)
    const unsubscribeLog = window.api.onGameLog((event) => setLogs((prev) => [...prev.slice(-499), event]))
    try {
      await window.api.play(profile, selectedVersion)
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      setLogs((prev) => [...prev, { source: 'launcher', level: 'error', message }])
    } finally {
      unsubscribeProgress()
      unsubscribeLog()
      setBusy(false)
    }
  }

  if (showCredits) {
    return <CreditsScreen onClose={() => setShowCredits(false)} />
  }

  if (showMods) {
    return (
      <ModsScreen
        selectedVersion={selectedVersion}
        enabledBundledMods={enabledBundledMods}
        onToggleBundledMod={handleToggleBundledMod}
        onClose={() => setShowMods(false)}
      />
    )
  }

  return (
    <div className="play-screen">
      <header>
        <div>
          <strong>{profile.name}</strong>
          {profile.isMock && <span className="mock-badge">Dev-Mock-Profil</span>}
        </div>
        <div className="header-actions">
          <button className="link-button" onClick={() => setShowMods(true)}>
            Mods
          </button>
          <button className="link-button" onClick={() => setShowCredits(true)}>
            Credits
          </button>
          <button className="link-button" onClick={onLogout}>
            Abmelden
          </button>
        </div>
      </header>

      {updateInfo?.updateAvailable && !updateDismissed && (
        <div className="update-banner">
          <span>
            Update verfügbar: {updateInfo.latestVersion} (aktuell {updateInfo.currentVersion})
          </span>
          <div className="header-actions">
            {updateInfo.releaseNotesUrl && (
              <button className="link-button" onClick={() => void window.api.openExternal(updateInfo.releaseNotesUrl!)}>
                Änderungen ansehen
              </button>
            )}
            <button className="link-button" onClick={() => setUpdateDismissed(true)}>
              Ausblenden
            </button>
          </div>
        </div>
      )}

      <div className="version-picker">
        <label htmlFor="version-select">Minecraft-Version</label>
        <select
          id="version-select"
          value={selectedVersion}
          onChange={(e) => setSelectedVersion(e.target.value)}
          disabled={busy || visibleVersions.length === 0}
        >
          {visibleVersions.length === 0 && <option value={selectedVersion}>{selectedVersion}</option>}
          {visibleVersions.map((v) => (
            <option key={v.id} value={v.id}>
              {v.id}
            </option>
          ))}
        </select>
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={showSnapshots}
            onChange={(e) => setShowSnapshots(e.target.checked)}
            disabled={busy}
          />
          Snapshots anzeigen
        </label>
        {versionsError && <span className="error">Versionsliste konnte nicht geladen werden: {versionsError}</span>}
        {!isBundleCompatibleVersion(selectedVersion) && (
          <span className="version-warning">
            Nur {MINECRAFT_VERSION} enthält die gebündelten Mods/Resourcepacks (Sodium, Lithium, eigener Client-Mod,
            …) — {selectedVersion} startet als reines Fabric+Vanilla ohne Mods.
          </span>
        )}
      </div>

      <button className="primary-button play-button" onClick={() => void handlePlay()} disabled={busy}>
        {busy ? 'Läuft…' : 'Play'}
      </button>

      {progress && (
        <div className="progress">
          <span>
            {progress.stage}
            {progress.label ? ` — ${progress.label}` : ''} ({progress.completed}/{progress.total})
          </span>
          <progress value={progress.completed} max={Math.max(progress.total, 1)} />
        </div>
      )}

      <pre className="log-panel">
        {logs.map((log, index) => (
          <div key={index} className={log.level === 'error' ? 'log-error' : 'log-info'}>
            {log.message}
          </div>
        ))}
      </pre>
    </div>
  )
}
