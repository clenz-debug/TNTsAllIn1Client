import { useEffect, useState } from 'react'
import type { GameVersionSummary, Instance } from '../../../shared/types'
import { MINECRAFT_VERSION } from '../../../shared/types'

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
  onSelect,
  onClose
}: Props) {
  const visibleVersions = versions.filter((v) => showSnapshots || v.type === 'release')

  const [newName, setNewName] = useState('')
  const [newVersion, setNewVersion] = useState(() => pickDefaultVersion(visibleVersions))
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

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

  function handleRename(instance: Instance): void {
    const name = window.prompt('Neuer Name für die Instanz:', instance.name)
    if (!name || !name.trim() || name.trim() === instance.name) return
    onInstancesChange(
      instances.map((candidate) => (candidate.id === instance.id ? { ...candidate, name: name.trim() } : candidate)),
      selectedInstanceId
    )
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

  function handleSelect(instance: Instance): void {
    onSelect(instance.id)
    onClose()
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
          <button className="secondary-button" onClick={handleCreate}>
            Erstellen
          </button>
        </div>
        {versionsError && <span className="error">Versionsliste konnte nicht geladen werden: {versionsError}</span>}
      </section>

      <section className="instances-section">
        <h3>Vorhandene Instanzen</h3>
        <ul className="instances-list">
          {instances.map((instance) => (
            <li key={instance.id} className="instances-row">
              <div className="instance-info">
                <strong>{instance.name}</strong>
                <span className="instance-version">{instance.versionId}</span>
                {instance.id === selectedInstanceId && <span className="mock-badge">Aktiv</span>}
              </div>
              <div className="header-actions">
                {instance.id !== selectedInstanceId && (
                  <button className="link-button" onClick={() => handleSelect(instance)} disabled={busy}>
                    Auswählen
                  </button>
                )}
                <button className="link-button" onClick={() => handleRename(instance)} disabled={busy}>
                  Umbenennen
                </button>
                <button className="link-button" onClick={() => void handleDelete(instance)} disabled={busy}>
                  Löschen
                </button>
              </div>
            </li>
          ))}
          {instances.length === 0 && <li className="mods-empty">Noch keine Instanz angelegt.</li>}
        </ul>
      </section>
    </div>
  )
}
