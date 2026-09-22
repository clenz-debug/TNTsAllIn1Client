import { useEffect, useState } from 'react'
import { Dropdown } from '../Dropdown'
import { formatError } from '../formatError'
import { useTranslations } from '../i18n/LanguageContext'
import type { GameVersionSummary, Instance } from '../../../shared/types'

interface Props {
  instances: Instance[]
  selectedInstanceId: string | null
  versions: GameVersionSummary[]
  versionsError: string | null
  /** Read-only here - the toggle itself now lives in the Settings screen (not per-instance, so it
   * doesn't belong on this screen's own state), this is only used to steer
   * {@link pickDefaultVersion}'s pre-selection and filter the version dropdown below. */
  showSnapshots: boolean
  /** Which Minecraft versions currently have bundle content available (dynamic, manifest-driven -
   * see `bundleCompat.ts`) - used only to steer {@link pickDefaultVersion}'s pre-selection, the
   * dropdown itself still lists every release/snapshot Fabric-supported version regardless. */
  bundleCompatibleVersions: string[]
  /** Both instance-list mutations (create/rename) and a delete result (fetched fresh from the
   * main process, see `handleDelete`) funnel through here - the caller (PlayScreen) just mirrors
   * whatever it's given into its own state and lets its existing save effect persist it. */
  onInstancesChange: (instances: Instance[], selectedInstanceId: string | null) => void
  onSelect: (id: string) => void
  onClose: () => void
}

/** Prefers the newest bundle-compatible version in the list (multi-version support follow-up -
 * `list` is already newest-first, same order `versionList.ts#fetchAvailableVersions` returns it
 * in), otherwise falls back to the newest entry overall so the dropdown never starts empty. */
function pickDefaultVersion(list: GameVersionSummary[], bundleCompatibleVersions: readonly string[]): string {
  return list.find((v) => bundleCompatibleVersions.includes(v.id))?.id ?? list[0]?.id ?? ''
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
  bundleCompatibleVersions,
  onInstancesChange,
  onSelect,
  onClose
}: Props) {
  const t = useTranslations()
  const visibleVersions = versions.filter((v) => showSnapshots || v.type === 'release')

  const [newName, setNewName] = useState('')
  const [newVersion, setNewVersion] = useState(() => pickDefaultVersion(visibleVersions, bundleCompatibleVersions))
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // Which instance's name is currently being edited inline (id), plus the in-progress text for it -
  // window.prompt() would have been simpler, but Electron's renderer doesn't implement it (unlike
  // alert()/confirm(), which do show a native dialog - confirmed the hard way: the "Löschen"
  // confirm() worked fine while "Umbenennen"'s prompt() silently did nothing at all).
  const [renamingId, setRenamingId] = useState<string | null>(null)
  const [renameDraft, setRenameDraft] = useState('')
  const [cloningId, setCloningId] = useState<string | null>(null)

  const [importBusy, setImportBusy] = useState(false)
  const [importResult, setImportResult] = useState<string | null>(null)

  useEffect(() => {
    if (visibleVersions.length === 0) return
    if (!visibleVersions.some((v) => v.id === newVersion)) {
      setNewVersion(pickDefaultVersion(visibleVersions, bundleCompatibleVersions))
    }
    // Same "only react to the visible set itself changing" reasoning as PlayScreen's equivalent
    // effect - see there for why this isn't keyed on visibleVersions/newVersion directly.
  }, [visibleVersions.map((v) => v.id).join(',')])

  function handleCreate(): void {
    if (!newVersion) return
    const name = newName.trim() || t.instances.defaultName(instances.length + 1)
    // crypto.randomUUID() is a plain Web Crypto API call, available in the renderer without any
    // Node integration - no main-process round trip needed just to mint an id.
    const instance: Instance = { id: crypto.randomUUID(), name, versionId: newVersion, enabledBundledMods: [] }
    onInstancesChange([...instances, instance], instance.id)
    setNewName('')
  }

  /**
   * "Einstellungen/Mods von einem anderen, bereits installierten Client übernehmen" (own user
   * request). Creates a new instance exactly like `handleCreate` (same client-side
   * `crypto.randomUUID()` pattern), then immediately asks the main process to pick a folder and
   * copy from it - unlike a normal new instance, this one needs its `game/` folder to exist right
   * away instead of waiting for the first "Play" click, since the import has to put files
   * somewhere now (see `clientImport.ts`'s own doc comment for the full reasoning, including why
   * imported `options.txt` intentionally lands in the *shared* settings cache, affecting every
   * instance, not just this new one - confirmed with the user, see the warning text below the
   * button in the UI).
   */
  async function handleImportFromClient(): Promise<void> {
    if (!newVersion) return
    setImportBusy(true)
    setError(null)
    setImportResult(null)
    try {
      const folder = await window.api.pickExternalClientFolder()
      if (!folder) return
      const name = newName.trim() || t.instances.defaultName(instances.length + 1)
      const instance: Instance = { id: crypto.randomUUID(), name, versionId: newVersion, enabledBundledMods: [] }
      onInstancesChange([...instances, instance], instance.id)
      setNewName('')

      const result = await window.api.importFromExternalClient(folder, instance.id, instance.versionId)
      const parts: string[] = []
      if (result.copiedMods.length > 0) parts.push(t.instances.importResultMods(result.copiedMods.length))
      if (result.importedOptions) parts.push(t.instances.importResultOptions)
      setImportResult(parts.length > 0 ? parts.join(', ') : t.instances.importResultNone)
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setImportBusy(false)
    }
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

  /**
   * Duplicates an instance including its whole on-disk content (saves/mods/options/resourcepacks)
   * - own user request, so trying something risky (a new mod, a config tweak) never has to touch
   * the original. Names the copy "<name> (Kopie)" right away rather than prompting first - same
   * "just create it, rename afterwards if needed" flow `handleCreate` already uses, and this
   * screen's existing "Umbenennen" already covers the rename step.
   */
  async function handleClone(instance: Instance): Promise<void> {
    setCloningId(instance.id)
    setError(null)
    try {
      const updated = await window.api.cloneInstance(instance.id, t.instances.copySuffix(instance.name))
      onInstancesChange(updated.instances, updated.selectedInstanceId)
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setCloningId(null)
    }
  }

  async function handleDelete(instance: Instance): Promise<void> {
    const confirmed = window.confirm(t.instances.deleteConfirm(instance.name))
    if (!confirmed) return

    setBusy(true)
    try {
      const updated = await window.api.deleteInstance(instance.id)
      onInstancesChange(updated.instances, updated.selectedInstanceId)
    } catch (err) {
      setError(formatError(err, t))
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
        <strong>{t.instances.title}</strong>
        <button className="link-button" onClick={onClose}>
          {t.common.back}
        </button>
      </header>

      {error && <span className="error">{error}</span>}

      <section className="instances-section">
        <h3>{t.instances.newInstanceHeading}</h3>
        <div className="instance-create-form">
          {/* Explicit label + autoFocus so this reads as "type here", not decoration - the
              placeholder alone looked identical in shape to an already-chosen name, so it was easy
              to miss that this field does anything (reported: had to rename after creating instead). */}
          <span className="instance-name-label">{t.instances.nameLabel}</span>
          <input
            type="text"
            className="instance-name-input"
            placeholder={t.instances.defaultName(instances.length + 1)}
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            autoFocus
          />
          <Dropdown
            value={newVersion}
            onChange={setNewVersion}
            disabled={visibleVersions.length === 0}
            options={
              visibleVersions.length === 0
                ? [{ value: newVersion, label: newVersion }]
                : visibleVersions.map((v) => ({ value: v.id, label: v.id }))
            }
          />
          <button className="secondary-button" onClick={handleCreate}>
            {t.instances.create}
          </button>
          <button
            className="secondary-button"
            onClick={() => void handleImportFromClient()}
            disabled={importBusy}
          >
            {importBusy ? t.instances.importing : t.instances.importFromClient}
          </button>
        </div>
        <p className="version-warning">{t.instances.importedSettingsWarning}</p>
        {importResult && <span className="status">{importResult}</span>}
        {versionsError && <span className="error">{t.instances.versionListError(versionsError)}</span>}
      </section>

      <section className="instances-section">
        <h3>{t.instances.existingHeading}</h3>
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
                    {t.common.save}
                  </button>
                  <button className="link-button" onClick={() => setRenamingId(null)}>
                    {t.common.cancel}
                  </button>
                </div>
              </li>
            ) : (
              <li key={instance.id} className="instances-row">
                <div className="instance-info">
                  <strong>{instance.name}</strong>
                  <span className="instance-version">{instance.versionId}</span>
                  {instance.id === selectedInstanceId && <span className="mock-badge">{t.instances.active}</span>}
                </div>
                <div className="header-actions">
                  {instance.id !== selectedInstanceId && (
                    <button className="link-button" onClick={() => handleSelect(instance)} disabled={busy}>
                      {t.instances.select}
                    </button>
                  )}
                  <button className="link-button" onClick={() => startRename(instance)} disabled={busy}>
                    {t.instances.rename}
                  </button>
                  <button
                    className="link-button"
                    onClick={() => void handleClone(instance)}
                    disabled={busy || cloningId !== null}
                  >
                    {cloningId === instance.id ? t.instances.cloning : t.instances.clone}
                  </button>
                  <button className="link-button" onClick={() => void handleDelete(instance)} disabled={busy}>
                    {t.common.delete}
                  </button>
                </div>
              </li>
            )
          )}
          {instances.length === 0 && <li className="mods-empty">{t.instances.empty}</li>}
        </ul>
      </section>
    </div>
  )
}
