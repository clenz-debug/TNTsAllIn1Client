import { useEffect, useState } from 'react'
import type {
  GameLogEvent,
  GameVersionSummary,
  Instance,
  LaunchProgressEvent,
  MinecraftProfile,
  UpdateCheckResult
} from '../../../shared/types'
import { isBundleCompatibleVersion, MINECRAFT_VERSION } from '../../../shared/types'
import { CreditsScreen } from './CreditsScreen'
import { InstancesScreen } from './InstancesScreen'
import { ModsScreen } from './ModsScreen'
import { SkinScreen } from './SkinScreen'

interface Props {
  profile: MinecraftProfile
  onProfileUpdate: (profile: MinecraftProfile) => void
  onLogout: () => void
}

export function PlayScreen({ profile, onProfileUpdate, onLogout }: Props) {
  const [busy, setBusy] = useState(false)
  const [progress, setProgress] = useState<LaunchProgressEvent | null>(null)
  const [logs, setLogs] = useState<GameLogEvent[]>([])
  const [showCredits, setShowCredits] = useState(false)
  const [showMods, setShowMods] = useState(false)
  const [showSkin, setShowSkin] = useState(false)
  const [showInstances, setShowInstances] = useState(false)

  const [versions, setVersions] = useState<GameVersionSummary[]>([])
  const [versionsError, setVersionsError] = useState<string | null>(null)
  const [showSnapshots, setShowSnapshots] = useState(false)
  const [instances, setInstances] = useState<Instance[]>([])
  const [selectedInstanceId, setSelectedInstanceId] = useState<string | null>(null)
  // Gates the save-effect below until the persisted settings have actually been applied - without
  // this, that effect's first run (on mount, still holding the plain useState defaults above)
  // would immediately overwrite whatever was saved from a previous session with those defaults.
  const [settingsLoaded, setSettingsLoaded] = useState(false)

  const selectedInstance = instances.find((instance) => instance.id === selectedInstanceId) ?? null

  useEffect(() => {
    Promise.all([window.api.loadSettings(), window.api.listVersions()])
      .then(([settings, list]) => {
        setVersions(list)
        setShowSnapshots(settings.showSnapshots)
        setInstances(settings.instances)
        setSelectedInstanceId(settings.selectedInstanceId)
        setSettingsLoaded(true)
      })
      .catch((err) => setVersionsError(err instanceof Error ? err.message : String(err)))
  }, [])

  useEffect(() => {
    if (!settingsLoaded) return
    void window.api.saveSettings({ showSnapshots, instances, selectedInstanceId })
  }, [settingsLoaded, showSnapshots, instances, selectedInstanceId])

  function handleInstancesChange(newInstances: Instance[], newSelectedId: string | null): void {
    setInstances(newInstances)
    setSelectedInstanceId(newSelectedId)
  }

  function handleToggleBundledMod(fileName: string, enabled: boolean): void {
    if (!selectedInstance) return
    setInstances((prev) =>
      prev.map((instance) =>
        instance.id !== selectedInstance.id
          ? instance
          : {
              ...instance,
              enabledBundledMods: enabled
                ? [...instance.enabledBundledMods, fileName]
                : instance.enabledBundledMods.filter((f) => f !== fileName)
            }
      )
    )
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

  async function handlePlay(): Promise<void> {
    if (!selectedInstance) return
    setBusy(true)
    setLogs([])
    setProgress(null)
    const unsubscribeProgress = window.api.onLaunchProgress(setProgress)
    const unsubscribeLog = window.api.onGameLog((event) => setLogs((prev) => [...prev.slice(-499), event]))
    try {
      await window.api.play(profile, selectedInstance.id)
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

  if (showInstances) {
    return (
      <InstancesScreen
        instances={instances}
        selectedInstanceId={selectedInstanceId}
        versions={versions}
        versionsError={versionsError}
        showSnapshots={showSnapshots}
        onShowSnapshotsChange={setShowSnapshots}
        onInstancesChange={handleInstancesChange}
        onSelect={setSelectedInstanceId}
        onClose={() => setShowInstances(false)}
      />
    )
  }

  if (showMods && selectedInstance) {
    return (
      <ModsScreen
        instanceId={selectedInstance.id}
        versionId={selectedInstance.versionId}
        enabledBundledMods={selectedInstance.enabledBundledMods}
        onToggleBundledMod={handleToggleBundledMod}
        onClose={() => setShowMods(false)}
      />
    )
  }

  if (showSkin) {
    return <SkinScreen profile={profile} onProfileUpdate={onProfileUpdate} onClose={() => setShowSkin(false)} />
  }

  return (
    <div className="play-screen">
      <header>
        <div>
          <strong>{profile.name}</strong>
          {profile.isMock && <span className="mock-badge">Dev-Mock-Profil</span>}
        </div>
        <div className="header-actions">
          <button className="link-button" onClick={() => setShowSkin(true)}>
            Skin
          </button>
          <button className="link-button" onClick={() => setShowMods(true)} disabled={!selectedInstance}>
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
        <label htmlFor="instance-select">Instanz</label>
        <select
          id="instance-select"
          value={selectedInstanceId ?? ''}
          onChange={(e) => setSelectedInstanceId(e.target.value)}
          disabled={busy || instances.length === 0}
        >
          {instances.length === 0 && <option value="">Keine Instanz</option>}
          {instances.map((instance) => (
            <option key={instance.id} value={instance.id}>
              {instance.name} ({instance.versionId})
            </option>
          ))}
        </select>
        <button className="secondary-button" onClick={() => setShowInstances(true)} disabled={busy}>
          Instanzen verwalten…
        </button>
        {versionsError && <span className="error">Versionsliste konnte nicht geladen werden: {versionsError}</span>}
        {instances.length === 0 && (
          <span className="version-warning">Noch keine Instanz angelegt - über "Instanzen verwalten…" eine erstellen.</span>
        )}
        {selectedInstance && !isBundleCompatibleVersion(selectedInstance.versionId) && (
          <span className="version-warning">
            Nur {MINECRAFT_VERSION} enthält die gebündelten Mods/Resourcepacks (Sodium, Lithium, eigener Client-Mod,
            …) — {selectedInstance.versionId} startet als reines Fabric+Vanilla ohne Mods.
          </span>
        )}
      </div>

      <button className="primary-button play-button" onClick={() => void handlePlay()} disabled={busy || !selectedInstance}>
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
