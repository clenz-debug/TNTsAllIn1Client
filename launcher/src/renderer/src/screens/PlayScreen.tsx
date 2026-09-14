import { useEffect, useState } from 'react'
import type {
  GameLogEvent,
  GameVersionSummary,
  Instance,
  LaunchProgressEvent,
  MinecraftProfile,
  SkinLibraryEntry,
  UpdateStatus
} from '../../../shared/types'
import { isBundleCompatibleVersion, MINECRAFT_VERSION } from '../../../shared/types'
import { CreditsScreen } from './CreditsScreen'
import { InstancesScreen } from './InstancesScreen'
import { ModsScreen } from './ModsScreen'
import { SkinEditorScreen } from './SkinEditorScreen'
import { SkinScreen } from './SkinScreen'

/** `'new'` opens the editor blank (template/own-PNG chooser); a `SkinLibraryEntry` opens it
 * pre-loaded via that entry's "Bearbeiten" button; `null` means the editor isn't open. */
type SkinEditorRequest = 'new' | SkinLibraryEntry | null

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
  const [skinEditorRequest, setSkinEditorRequest] = useState<SkinEditorRequest>(null)
  const [showInstances, setShowInstances] = useState(false)

  const [versions, setVersions] = useState<GameVersionSummary[]>([])
  const [versionsError, setVersionsError] = useState<string | null>(null)
  const [showSnapshots, setShowSnapshots] = useState(false)
  const [instances, setInstances] = useState<Instance[]>([])
  const [selectedInstanceId, setSelectedInstanceId] = useState<string | null>(null)
  // Never read or changed by this screen itself (only `storageManager.ts#changeStorageLocation`
  // in the main process sets it, via its own direct `saveLauncherSettings` call) - kept here purely
  // so this screen's own save-effect below round-trips it unchanged instead of wiping it back to
  // `null` every time showSnapshots/instances/selectedInstanceId change.
  const [dataRootOverride, setDataRootOverride] = useState<string | null>(null)
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
        setDataRootOverride(settings.dataRootOverride)
        setSettingsLoaded(true)
      })
      .catch((err) => setVersionsError(err instanceof Error ? err.message : String(err)))
  }, [])

  useEffect(() => {
    if (!settingsLoaded) return
    void window.api.saveSettings({ showSnapshots, instances, selectedInstanceId, dataRootOverride })
  }, [settingsLoaded, showSnapshots, instances, selectedInstanceId, dataRootOverride])

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

  const [updateStatus, setUpdateStatus] = useState<UpdateStatus | null>(null)
  const [updateDismissed, setUpdateDismissed] = useState(false)

  useEffect(() => {
    // Long-lived subscription, not a one-shot check - main only pushes when electron-updater's own
    // state actually changes (see autoUpdate.ts). No-ops entirely in dev builds (electron-updater
    // requires a packaged app), so this simply never fires outside a real installed launcher.
    return window.api.onUpdateStatus((status) => {
      setUpdateStatus(status)
      setUpdateDismissed(false)
    })
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
        onDataRootOverrideChange={setDataRootOverride}
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

  if (skinEditorRequest !== null) {
    return (
      <SkinEditorScreen
        editingLibraryEntry={skinEditorRequest === 'new' ? undefined : skinEditorRequest}
        onClose={() => setSkinEditorRequest(null)}
      />
    )
  }

  if (showSkin) {
    return (
      <SkinScreen
        profile={profile}
        onProfileUpdate={onProfileUpdate}
        onClose={() => setShowSkin(false)}
        onOpenEditor={(entry) => setSkinEditorRequest(entry ?? 'new')}
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

      {/* 'checking'/'not-available'/'error' deliberately show no banner - same "purely informational,
          a failed check is never worth surfacing" reasoning the old Phase 6d check already had.
          That matters concretely right now: this repo has no GitHub Release yet, so every real
          check errors out until the first one is published - showing that as a visible error every
          single launch would just be noise, not a genuine problem to react to. */}
      {updateStatus && !updateDismissed && (updateStatus.state === 'available' || updateStatus.state === 'downloading' || updateStatus.state === 'downloaded') && (
        <div className="update-banner">
          <span>
            {updateStatus.state === 'available' && `Update gefunden (Version ${updateStatus.version}) - wird heruntergeladen…`}
            {updateStatus.state === 'downloading' && `Update wird heruntergeladen… (${updateStatus.percent ?? 0}%)`}
            {updateStatus.state === 'downloaded' &&
              `Update heruntergeladen (Version ${updateStatus.version}) - bereit zum Installieren.`}
          </span>
          <div className="header-actions">
            {updateStatus.state === 'downloaded' && (
              <button className="link-button" onClick={() => void window.api.installUpdateNow()}>
                Jetzt neu starten
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
