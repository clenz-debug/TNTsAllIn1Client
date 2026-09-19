import { useEffect, useState } from 'react'
import { formatError } from '../formatError'
import type {
  GameLogEvent,
  GameVersionSummary,
  Instance,
  LaunchProgressEvent,
  LauncherSettings,
  MinecraftProfile,
  ModBundleUpdateInfo,
  SkinLibraryEntry,
  UpdateStatus
} from '../../../shared/types'
import { isBundleCompatibleVersion } from '../../../shared/types'
import { CreditsScreen } from './CreditsScreen'
import { InstancesScreen } from './InstancesScreen'
import { ModsScreen } from './ModsScreen'
import { SkinEditorScreen } from './SkinEditorScreen'
import { SkinScreen } from './SkinScreen'
import { WorldsScreen } from './WorldsScreen'

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
  const [showWorlds, setShowWorlds] = useState(false)
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
  // Same "mirror it, never edit it here" reasoning as `dataRootOverride` above - written directly
  // by `main/launch/modBundleUpdater.ts#applyModBundleUpdate` (see `handleApplyModBundleUpdate`
  // below, which re-syncs these two right after a successful apply) rather than through this
  // screen's own state, so the save-effect needs its own up-to-date copy to round-trip instead of
  // clobbering a just-applied update back to stale data on the next unrelated save.
  const [appliedModBundleVersions, setAppliedModBundleVersions] = useState<LauncherSettings['appliedModBundleVersions']>({})
  const [appliedOwnModVersions, setAppliedOwnModVersions] = useState<LauncherSettings['appliedOwnModVersions']>({})
  const [appliedResourcepackVersions, setAppliedResourcepackVersions] = useState<LauncherSettings['appliedResourcepackVersions']>({})
  // Gates the save-effect below until the persisted settings have actually been applied - without
  // this, that effect's first run (on mount, still holding the plain useState defaults above)
  // would immediately overwrite whatever was saved from a previous session with those defaults.
  const [settingsLoaded, setSettingsLoaded] = useState(false)
  // Which Minecraft versions currently have bundle content available - dynamic, manifest-driven
  // (multi-version support follow-up) instead of a single compiled-in version id. Loaded once
  // alongside settings/versions below; a version added to the manifest while the launcher is
  // already running only shows up after the next restart (see `bundleCompat.ts`'s own doc comment).
  const [bundleCompatibleVersions, setBundleCompatibleVersions] = useState<string[]>([])

  const selectedInstance = instances.find((instance) => instance.id === selectedInstanceId) ?? null

  useEffect(() => {
    Promise.all([window.api.loadSettings(), window.api.listVersions(), window.api.listBundleCompatibleVersions()])
      .then(([settings, list, bundleVersions]) => {
        setVersions(list)
        setShowSnapshots(settings.showSnapshots)
        setInstances(settings.instances)
        setSelectedInstanceId(settings.selectedInstanceId)
        setDataRootOverride(settings.dataRootOverride)
        setAppliedModBundleVersions(settings.appliedModBundleVersions)
        setAppliedOwnModVersions(settings.appliedOwnModVersions)
        setAppliedResourcepackVersions(settings.appliedResourcepackVersions)
        setBundleCompatibleVersions(bundleVersions)
        setSettingsLoaded(true)
      })
      .catch((err) => setVersionsError(formatError(err)))
  }, [])

  useEffect(() => {
    if (!settingsLoaded) return
    void window.api.saveSettings({
      showSnapshots,
      instances,
      selectedInstanceId,
      dataRootOverride,
      appliedModBundleVersions,
      appliedOwnModVersions,
      appliedResourcepackVersions
    })
  }, [
    settingsLoaded,
    showSnapshots,
    instances,
    selectedInstanceId,
    dataRootOverride,
    appliedModBundleVersions,
    appliedOwnModVersions,
    appliedResourcepackVersions
  ])

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

  const [modBundleUpdate, setModBundleUpdate] = useState<ModBundleUpdateInfo | null>(null)
  const [modBundleUpdateError, setModBundleUpdateError] = useState<string | null>(null)
  const [applyingModBundleUpdate, setApplyingModBundleUpdate] = useState(false)

  useEffect(() => {
    if (!selectedInstance) {
      setModBundleUpdate(null)
      return
    }
    // Same "a failed check is never worth surfacing" philosophy as the launcher's own update check
    // just above - silently no-ops if the manifest isn't reachable yet (e.g. no GitHub release/repo
    // pushed yet), rather than showing an error banner on every single launch. Re-runs whenever the
    // selected instance's version changes, not just once on mount - each version has its own
    // independent bundle to check now.
    window.api
      .checkModBundleUpdate(selectedInstance.versionId)
      .then((info) =>
        setModBundleUpdate(
          info.outdatedMods.length > 0 || info.outdatedResourcepacks.length > 0 || info.ownModUpdateAvailable ? info : null
        )
      )
      .catch(() => undefined)
  }, [selectedInstance?.versionId])

  async function handleApplyModBundleUpdate(): Promise<void> {
    if (!selectedInstance) return
    setApplyingModBundleUpdate(true)
    setModBundleUpdateError(null)
    try {
      const updated = await window.api.applyModBundleUpdate(selectedInstance.versionId)
      // Sync this screen's own mirrors immediately - see their declaration above for why: without
      // this, the next unrelated save-effect run would round-trip the stale pre-apply copy and
      // clobber what was just written back to disk.
      setAppliedModBundleVersions(updated.appliedModBundleVersions)
      setAppliedOwnModVersions(updated.appliedOwnModVersions)
      setAppliedResourcepackVersions(updated.appliedResourcepackVersions)
      setModBundleUpdate(null)
    } catch (err) {
      setModBundleUpdateError(formatError(err))
    } finally {
      setApplyingModBundleUpdate(false)
    }
  }

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
      const message = formatError(err)
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
        bundleCompatibleVersions={bundleCompatibleVersions}
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
        bundleCompatibleVersions={bundleCompatibleVersions}
        onToggleBundledMod={handleToggleBundledMod}
        onClose={() => setShowMods(false)}
      />
    )
  }

  if (showWorlds && selectedInstance) {
    return <WorldsScreen instanceId={selectedInstance.id} instances={instances} onClose={() => setShowWorlds(false)} />
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

      {modBundleUpdate && (
        <div className="update-banner">
          <span>
            Neue Mod-Bundle-Version verfügbar (
            {[
              ...modBundleUpdate.outdatedMods.map((entry) => entry.name),
              ...modBundleUpdate.outdatedResourcepacks.map((entry) => entry.name),
              ...(modBundleUpdate.ownModUpdateAvailable ? ['eigener Mod'] : [])
            ].join(', ')}
            ).{modBundleUpdateError && <span className="error"> {modBundleUpdateError}</span>}
          </span>
          <div className="header-actions">
            <button className="link-button" disabled={applyingModBundleUpdate} onClick={() => void handleApplyModBundleUpdate()}>
              {applyingModBundleUpdate ? 'Wird aktualisiert…' : 'Aktualisieren'}
            </button>
            <button className="link-button" disabled={applyingModBundleUpdate} onClick={() => setModBundleUpdate(null)}>
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
        <button className="secondary-button" onClick={() => setShowMods(true)} disabled={busy || !selectedInstance}>
          Mods…
        </button>
        <button className="secondary-button" onClick={() => setShowWorlds(true)} disabled={busy || !selectedInstance}>
          Welten…
        </button>
        {versionsError && <span className="error">Versionsliste konnte nicht geladen werden: {versionsError}</span>}
        {instances.length === 0 && (
          <span className="version-warning">Noch keine Instanz angelegt - über "Instanzen verwalten…" eine erstellen.</span>
        )}
        {selectedInstance && !isBundleCompatibleVersion(selectedInstance.versionId, bundleCompatibleVersions) && (
          <span className="version-warning">
            {selectedInstance.versionId} hat keine gebündelten Mods/Resourcepacks (Sodium, Lithium, eigener
            Client-Mod, …) — startet als reines Fabric+Vanilla ohne Mods.
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
