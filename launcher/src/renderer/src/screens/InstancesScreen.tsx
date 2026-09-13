import { useEffect, useState } from 'react'
import type { GameVersionSummary, Instance, StorageInfo, StorageMoveProgressEvent } from '../../../shared/types'
import { MINECRAFT_VERSION } from '../../../shared/types'

function formatBytes(bytes: number): string {
  const gb = bytes / 1024 ** 3
  return gb >= 1 ? `${gb.toFixed(1)} GB` : `${(bytes / 1024 ** 2).toFixed(0)} MB`
}

interface Props {
  instances: Instance[]
  selectedInstanceId: string | null
  versions: GameVersionSummary[]
  versionsError: string | null
  showSnapshots: boolean
  onShowSnapshotsChange: (value: boolean) => void
  /** Both instance-list mutations (create/rename) and a delete result (fetched fresh from the
   * main process, see `handleDelete`) funnel through here - the caller (PlayScreen) just mirrors
   * whatever it's given into its own state and lets its existing save effect persist it. */
  onInstancesChange: (instances: Instance[], selectedInstanceId: string | null) => void
  /** Called right after a successful `changeStorageLocation()` so PlayScreen's own mirrored
   * `dataRootOverride` state stays in sync - without this, PlayScreen's next unrelated save-effect
   * run (e.g. toggling `showSnapshots`) would round-trip its now-stale cached value and silently
   * clobber the just-changed location back in `launcher-settings.json`. */
  onDataRootOverrideChange: (path: string) => void
  onSelect: (id: string) => void
  onClose: () => void
}

/** Prefers the bundle-pinned version if it's in the list (should always be, it's a stable
 * release), otherwise falls back to the newest entry so the dropdown never starts empty. */
function pickDefaultVersion(list: GameVersionSummary[]): string {
  if (list.some((v) => v.id === MINECRAFT_VERSION)) return MINECRAFT_VERSION
  return list[0]?.id ?? MINECRAFT_VERSION
}

/**
 * Instance idea (own user request, see `Ideen_für_den_client.md`): instead of one shared install
 * per Minecraft version, several independent instances - e.g. the same version once with a mod,
 * once without, without constantly toggling mods back and forth on a single shared folder. Kept
 * deliberately close to the existing plain-list style of `ModsScreen`/`CreditsScreen` rather than
 * a redesign - the user explicitly wants the launcher's look reworked only once every planned
 * feature (this one included) is actually built, not before.
 */
export function InstancesScreen({
  instances,
  selectedInstanceId,
  versions,
  versionsError,
  showSnapshots,
  onShowSnapshotsChange,
  onInstancesChange,
  onDataRootOverrideChange,
  onSelect,
  onClose
}: Props) {
  const visibleVersions = versions.filter((v) => showSnapshots || v.type === 'release')

  const [newName, setNewName] = useState('')
  const [newVersion, setNewVersion] = useState(() => pickDefaultVersion(visibleVersions))
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // Which instance's name is currently being edited inline (id), plus the in-progress text for it -
  // window.prompt() would have been simpler, but Electron's renderer doesn't implement it (unlike
  // alert()/confirm(), which do show a native dialog - confirmed the hard way: the "Löschen"
  // confirm() worked fine while "Umbenennen"'s prompt() silently did nothing at all).
  const [renamingId, setRenamingId] = useState<string | null>(null)
  const [renameDraft, setRenameDraft] = useState('')

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
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      unsubscribe()
      setMovingStorage(false)
      setMoveProgress(null)
    }
  }

  useEffect(() => {
    if (visibleVersions.length === 0) return
    if (!visibleVersions.some((v) => v.id === newVersion)) {
      setNewVersion(pickDefaultVersion(visibleVersions))
    }
    // Same "only react to the visible set itself changing" reasoning as PlayScreen's equivalent
    // effect - see there for why this isn't keyed on visibleVersions/newVersion directly.
  }, [visibleVersions.map((v) => v.id).join(',')])

  function handleCreate(): void {
    if (!newVersion) return
    const name = newName.trim() || `Instanz ${instances.length + 1}`
    // crypto.randomUUID() is a plain Web Crypto API call, available in the renderer without any
    // Node integration - no main-process round trip needed just to mint an id.
    const instance: Instance = { id: crypto.randomUUID(), name, versionId: newVersion, enabledBundledMods: [] }
    onInstancesChange([...instances, instance], instance.id)
    setNewName('')
  }

  function startRename(instance: Instance): void {
    setRenamingId(instance.id)
    setRenameDraft(instance.name)
  }

  function commitRename(): void {
    const name = renameDraft.trim()
    if (name) {
      onInstancesChange(
        instances.map((candidate) => (candidate.id === renamingId ? { ...candidate, name } : candidate)),
        selectedInstanceId
      )
    }
    setRenamingId(null)
  }

  async function handleDelete(instance: Instance): Promise<void> {
    const confirmed = window.confirm(
      `"${instance.name}" wirklich löschen? Speicherstände, Einstellungen und Mods dieser Instanz gehen dabei unwiderruflich verloren.`
    )
    if (!confirmed) return

    setBusy(true)
    try {
      const updated = await window.api.deleteInstance(instance.id)
      onInstancesChange(updated.instances, updated.selectedInstanceId)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  // Deliberately does not call onClose() - a misclick here used to force reopening the whole
  // screen just to fix it (own user feedback). Selecting now just updates which instance is
  // active while staying on this screen; "Zurück" leaves whenever the user is actually done.
  function handleSelect(instance: Instance): void {
    onSelect(instance.id)
  }

  return (
    <div className="instances-screen">
      <header>
        <strong>Instanzen</strong>
        <button className="link-button" onClick={onClose}>
          Zurück
        </button>
      </header>

      {error && <span className="error">{error}</span>}

      <section className="instances-section">
        <h3>Speicherort</h3>
        <div className="instance-info">
          <span>{storageInfo?.path ?? 'Lädt…'}</span>
          {storageInfo?.freeBytes != null && (
            <span className="instance-version">{formatBytes(storageInfo.freeBytes)} frei</span>
          )}
        </div>
        <button
          className="secondary-button"
          onClick={() => void handleChangeStorageLocation()}
          disabled={busy || movingStorage}
        >
          Ändern…
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
        <h3>Neue Instanz</h3>
        <div className="instance-create-form">
          <input
            type="text"
            className="instance-name-input"
            placeholder={`Instanz ${instances.length + 1}`}
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
          />
          <select value={newVersion} onChange={(e) => setNewVersion(e.target.value)} disabled={visibleVersions.length === 0}>
            {visibleVersions.length === 0 && <option value={newVersion}>{newVersion}</option>}
            {visibleVersions.map((v) => (
              <option key={v.id} value={v.id}>
                {v.id}
              </option>
            ))}
          </select>
          <label className="checkbox-label">
            <input type="checkbox" checked={showSnapshots} onChange={(e) => onShowSnapshotsChange(e.target.checked)} />
            Snapshots anzeigen
          </label>
          <button className="secondary-button" onClick={handleCreate} disabled={movingStorage}>
            Erstellen
          </button>
        </div>
        {versionsError && <span className="error">Versionsliste konnte nicht geladen werden: {versionsError}</span>}
      </section>

      <section className="instances-section">
        <h3>Vorhandene Instanzen</h3>
        <ul className="instances-list">
          {instances.map((instance) =>
            renamingId === instance.id ? (
              <li key={instance.id} className="instances-row">
                <input
                  type="text"
                  className="instance-name-input"
                  autoFocus
                  value={renameDraft}
                  onChange={(e) => setRenameDraft(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') commitRename()
                    if (e.key === 'Escape') setRenamingId(null)
                  }}
                />
                <div className="header-actions">
                  <button className="link-button" onClick={commitRename}>
                    Speichern
                  </button>
                  <button className="link-button" onClick={() => setRenamingId(null)}>
                    Abbrechen
                  </button>
                </div>
              </li>
            ) : (
              <li key={instance.id} className="instances-row">
                <div className="instance-info">
                  <strong>{instance.name}</strong>
                  <span className="instance-version">{instance.versionId}</span>
                  {instance.id === selectedInstanceId && <span className="mock-badge">Aktiv</span>}
                </div>
                <div className="header-actions">
                  {instance.id !== selectedInstanceId && (
                    <button className="link-button" onClick={() => handleSelect(instance)} disabled={busy || movingStorage}>
                      Auswählen
                    </button>
                  )}
                  <button className="link-button" onClick={() => startRename(instance)} disabled={busy || movingStorage}>
                    Umbenennen
                  </button>
                  <button className="link-button" onClick={() => void handleDelete(instance)} disabled={busy || movingStorage}>
                    Löschen
                  </button>
                </div>
              </li>
            )
          )}
          {instances.length === 0 && <li className="mods-empty">Noch keine Instanz angelegt.</li>}
        </ul>
      </section>
    </div>
  )
}
