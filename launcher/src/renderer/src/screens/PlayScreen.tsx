import { useEffect, useState } from 'react'
import { Dropdown } from '../Dropdown'
import { errorCode, formatError } from '../formatError'
import { useTranslations } from '../i18n/LanguageContext'
import { Logo } from '../Logo'
import { PlayerMenuButton } from '../PlayerMenuButton'
import type {
  ClientDesign,
  GameLogEvent,
  GameVersionSummary,
  Instance,
  Language,
  LaunchProgressEvent,
  LauncherSettings,
  MinecraftProfile,
  ModBundleUpdateInfo,
  SkinLibraryEntry,
  ThemeColors,
  UpdateStatus
} from '../../../shared/types'
import { isBundleCompatibleVersion } from '../../../shared/types'
import { CreditsScreen } from './CreditsScreen'
import { InstancesScreen } from './InstancesScreen'
import { ModsScreen } from './ModsScreen'
import { SettingsScreen } from './SettingsScreen'
import { SkinEditorScreen } from './SkinEditorScreen'
import { SkinScreen } from './SkinScreen'
import { ResourcepacksScreen } from './ResourcepacksScreen'
import { WorldsScreen } from './WorldsScreen'

/** `'new'` opens the editor blank (template/own-PNG chooser); a `SkinLibraryEntry` opens it
 * pre-loaded via that entry's "Bearbeiten" button; `null` means the editor isn't open. */
type SkinEditorRequest = 'new' | SkinLibraryEntry | null

interface Props {
  profile: MinecraftProfile
  onProfileUpdate: (profile: MinecraftProfile) => void
  onLogout: () => void
  /** Owned by `App.tsx` (its `LanguageProvider` needs it too, for screens outside this one that
   * aren't reachable from here, e.g. LoginScreen) - this screen only mirrors it into its own
   * settings load/save cycle so a change made in `SettingsScreen` actually persists, the same way
   * every other field below does. */
  language: Language
  onLanguageChange: (language: Language) => void
}

export function PlayScreen({ profile, onProfileUpdate, onLogout, language, onLanguageChange }: Props) {
  const t = useTranslations()
  const [busy, setBusy] = useState(false)
  const [progress, setProgress] = useState<LaunchProgressEvent | null>(null)
  const [logs, setLogs] = useState<GameLogEvent[]>([])
  const [showCredits, setShowCredits] = useState(false)
  const [showMods, setShowMods] = useState(false)
  const [showWorlds, setShowWorlds] = useState(false)
  const [showResourcepacks, setShowResourcepacks] = useState(false)
  const [showSkin, setShowSkin] = useState(false)
  const [skinEditorRequest, setSkinEditorRequest] = useState<SkinEditorRequest>(null)
  const [showInstances, setShowInstances] = useState(false)
  const [showSettings, setShowSettings] = useState(false)

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
  const [maxMemoryMb, setMaxMemoryMb] = useState<LauncherSettings['maxMemoryMb']>(null)
  const [consoleInSeparateWindow, setConsoleInSeparateWindow] = useState(false)
  const [clientDesign, setClientDesign] = useState<ClientDesign>('minecraft')
  const [themeColors, setThemeColors] = useState<ThemeColors | null>(null)
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

  const [headTextureDataUri, setHeadTextureDataUri] = useState<string | null>(null)

  // Own follow-up request ("der Button mit dem Namen sollte zusätzlich die Vorderseite vom Kopf
  // des aktuellen mc skins ... zeigen") - refetches whenever the account's active skin changes
  // (e.g. right after uploading a new one in the Skin screen), same "find the ACTIVE entry, fall
  // back to the first one" logic SkinScreen already uses to pick which skin is "the" current one.
  useEffect(() => {
    const activeSkin = profile.skins.find((skin) => skin.state === 'ACTIVE') ?? profile.skins[0] ?? null
    if (!activeSkin) {
      setHeadTextureDataUri(null)
      return
    }
    let cancelled = false
    window.api
      .fetchSkinTexture(activeSkin.url)
      .then((dataUri) => {
        if (!cancelled) setHeadTextureDataUri(dataUri)
      })
      .catch(() => {
        if (!cancelled) setHeadTextureDataUri(null)
      })
    return () => {
      cancelled = true
    }
  }, [profile.skins])

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
        setMaxMemoryMb(settings.maxMemoryMb)
        setConsoleInSeparateWindow(settings.consoleInSeparateWindow)
        setClientDesign(settings.clientDesign)
        setThemeColors(settings.themeColors)
        onLanguageChange(settings.language)
        setSettingsLoaded(true)
      })
      .catch((err) => setVersionsError(formatError(err, t)))
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
      appliedResourcepackVersions,
      maxMemoryMb,
      consoleInSeparateWindow,
      themeColors,
      language,
      clientDesign,
      // Always true here, never a field this screen itself tracks - PlayScreen only ever mounts
      // once App.tsx's own onboarding gate has already passed (see OnboardingScreen.tsx), so there's
      // no scenario where a save from here could still be pre-onboarding.
      onboardingCompleted: true
    })
  }, [
    settingsLoaded,
    showSnapshots,
    instances,
    selectedInstanceId,
    dataRootOverride,
    appliedModBundleVersions,
    appliedOwnModVersions,
    appliedResourcepackVersions,
    maxMemoryMb,
    consoleInSeparateWindow,
    themeColors,
    language,
    clientDesign
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
      setModBundleUpdateError(formatError(err, t))
    } finally {
      setApplyingModBundleUpdate(false)
    }
  }

  async function handlePlay(): Promise<void> {
    if (!selectedInstance) return
    setBusy(true)
    setLogs([])
    setProgress(null)
    if (consoleInSeparateWindow) {
      // Fire-and-forget: a failed open just means the log panel silently stays inline for this run
      // instead of blocking "Play" over a window that's only ever a convenience.
      void window.api.openConsoleWindow()
    }
    const unsubscribeProgress = window.api.onLaunchProgress(setProgress)
    const unsubscribeLog = window.api.onGameLog((event) => setLogs((prev) => [...prev.slice(-499), event]))
    try {
      await window.api.play(profile, selectedInstance.id)
    } catch (err) {
      // main already sent a "Start abgebrochen."/"Launch cancelled." info-level log line to both
      // windows (see handlers.ts's LaunchPlay) before throwing this - showing it again here, in red,
      // would duplicate it and misrepresent a deliberate Cancel-button click as a failure.
      if (errorCode(err) !== 'launch.cancelled') {
        const message = formatError(err, t)
        setLogs((prev) => [...prev, { source: 'launcher', level: 'error', message }])
      }
    } finally {
      unsubscribeProgress()
      unsubscribeLog()
      setBusy(false)
      // The title screen's logo button can switch the design in-game - main already copied that
      // into the settings file once the game exited (clientDesignSync.ts#readBackClientDesign), so
      // pick it up here too, before this screen's next save would write the old value back.
      window.api
        .loadSettings()
        .then((settings) => setClientDesign(settings.clientDesign))
        .catch(() => undefined)
    }
  }

  async function handleCancel(): Promise<void> {
    await window.api.cancelLaunch()
  }

  if (showCredits) {
    return <CreditsScreen onClose={() => setShowCredits(false)} />
  }

  if (showSettings) {
    return (
      <SettingsScreen
        showSnapshots={showSnapshots}
        onShowSnapshotsChange={setShowSnapshots}
        maxMemoryMb={maxMemoryMb}
        onMaxMemoryMbChange={setMaxMemoryMb}
        consoleInSeparateWindow={consoleInSeparateWindow}
        onConsoleInSeparateWindowChange={setConsoleInSeparateWindow}
        themeColors={themeColors}
        onThemeColorsChange={setThemeColors}
        language={language}
        onLanguageChange={onLanguageChange}
        clientDesign={clientDesign}
        onClientDesignChange={setClientDesign}
        onDataRootOverrideChange={setDataRootOverride}
        onClose={() => setShowSettings(false)}
      />
    )
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
        bundleCompatibleVersions={bundleCompatibleVersions}
        onToggleBundledMod={handleToggleBundledMod}
        onClose={() => setShowMods(false)}
      />
    )
  }

  if (showWorlds && selectedInstance) {
    return <WorldsScreen instanceId={selectedInstance.id} instances={instances} onClose={() => setShowWorlds(false)} />
  }

  if (showResourcepacks && selectedInstance) {
    return (
      <ResourcepacksScreen
        instanceId={selectedInstance.id}
        instanceName={selectedInstance.name}
        onClose={() => setShowResourcepacks(false)}
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
        <div className="identity-row">
          <Logo className="app-logo" />
          <PlayerMenuButton
            name={profile.name}
            headTextureDataUri={headTextureDataUri}
            logoutLabel={t.play.headerLogout}
            menuLabel={t.play.playerMenuLabel}
            onLogout={onLogout}
          />
        </div>
        <div className="header-actions">
          <button className="link-button" onClick={() => setShowSkin(true)}>
            {t.play.headerSkin}
          </button>
          <button className="link-button" onClick={() => setShowCredits(true)}>
            {t.play.headerCredits}
          </button>
          <button className="link-button" onClick={() => setShowSettings(true)}>
            {t.play.headerSettings}
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
            {updateStatus.state === 'available' && t.play.update.available(updateStatus.version ?? '')}
            {updateStatus.state === 'downloading' && t.play.update.downloading(updateStatus.percent ?? 0)}
            {updateStatus.state === 'downloaded' && t.play.update.downloaded(updateStatus.version ?? '')}
          </span>
          <div className="header-actions">
            {updateStatus.state === 'downloaded' && (
              <button className="link-button" onClick={() => void window.api.installUpdateNow()}>
                {t.play.update.restartNow}
              </button>
            )}
            <button className="link-button" onClick={() => setUpdateDismissed(true)}>
              {t.common.hide}
            </button>
          </div>
        </div>
      )}

      {modBundleUpdate && (
        <div className="update-banner">
          <span>
            {t.play.modBundleUpdate.available(
              [
                ...modBundleUpdate.outdatedMods.map((entry) => entry.name),
                ...modBundleUpdate.outdatedResourcepacks.map((entry) => entry.name),
                ...(modBundleUpdate.ownModUpdateAvailable ? [t.play.modBundleUpdate.ownMod] : [])
              ].join(', ')
            )}
            {modBundleUpdateError && <span className="error"> {modBundleUpdateError}</span>}
          </span>
          <div className="header-actions">
            <button className="link-button" disabled={applyingModBundleUpdate} onClick={() => void handleApplyModBundleUpdate()}>
              {applyingModBundleUpdate ? t.play.modBundleUpdate.applying : t.play.modBundleUpdate.apply}
            </button>
            <button className="link-button" disabled={applyingModBundleUpdate} onClick={() => setModBundleUpdate(null)}>
              {t.common.hide}
            </button>
          </div>
        </div>
      )}

      <div className="version-picker">
        <label htmlFor="instance-select">{t.play.instanceLabel}</label>
        <Dropdown
          id="instance-select"
          value={selectedInstanceId ?? ''}
          onChange={setSelectedInstanceId}
          disabled={busy || instances.length === 0}
          options={
            instances.length === 0
              ? [{ value: '', label: t.play.noInstance }]
              : instances.map((instance) => ({ value: instance.id, label: `${instance.name} (${instance.versionId})` }))
          }
        />
        <button className="secondary-button" onClick={() => setShowInstances(true)} disabled={busy}>
          {t.play.manageInstances}
        </button>
        <button className="secondary-button" onClick={() => setShowMods(true)} disabled={busy || !selectedInstance}>
          {t.play.mods}
        </button>
        <button className="secondary-button" onClick={() => setShowWorlds(true)} disabled={busy || !selectedInstance}>
          {t.play.worlds}
        </button>
        <button className="secondary-button" onClick={() => setShowResourcepacks(true)} disabled={busy || !selectedInstance}>
          {t.play.resourcepacks}
        </button>
        {versionsError && <span className="error">{t.play.versionListError(versionsError)}</span>}
        {instances.length === 0 && <span className="version-warning">{t.play.noInstanceWarning}</span>}
        {selectedInstance && !isBundleCompatibleVersion(selectedInstance.versionId, bundleCompatibleVersions) && (
          <span className="version-warning">{t.play.bundleIncompatibleWarning(selectedInstance.versionId)}</span>
        )}
      </div>

      <div className="play-button-row">
        <button className="primary-button play-button" onClick={() => void handlePlay()} disabled={busy || !selectedInstance}>
          {busy ? t.play.playing : t.play.play}
        </button>
        {/* Own user request: a Cancel button "wie bei anderen Clients üblich" that aborts an
            in-progress launch (still downloading/installing, or already-running Minecraft alike -
            see handlers.ts's LaunchPlay/gameProcess.ts). Shown here only when the console isn't in
            its own separate window - that window gets the identical button instead
            (ConsoleWindowView.tsx), so there's never a second one competing for the same click. */}
        {busy && !consoleInSeparateWindow && (
          <button className="secondary-button" onClick={() => void handleCancel()}>
            {t.play.cancel}
          </button>
        )}
      </div>

      {/* When the Settings screen's "Konsole in separatem Fenster" toggle is on, handlePlay opens
          a second BrowserWindow (main/consoleWindow.ts) that receives the exact same log/progress
          events instead - showing both here too would just be a confusing duplicate. */}
      {!consoleInSeparateWindow && (
        <>
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
        </>
      )}
    </div>
  )
}
