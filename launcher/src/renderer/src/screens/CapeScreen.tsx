import { useEffect, useState } from 'react'
import { ConfirmDialog } from '../ConfirmDialog'
import { formatError } from '../formatError'
import { useLanguage, useTranslations } from '../i18n/LanguageContext'
import type { CapeLibraryEntry, CustomCapeStatus, MinecraftProfile, SkinVariant } from '../../../shared/types'
import { SkinModelPreview } from '../skinEditor/SkinModelPreview'
import { CapeConverter } from './CapeConverter'
import { CapeEditorScreen } from './CapeEditorScreen'

interface Props {
  profile: MinecraftProfile
  onClose: () => void
}

/**
 * Custom capes (own cosmetic system: our cape server + the bundled "Cape Provider" mod) - its own
 * launcher area, own user request, split out of the Skin screen, which keeps only Mojang's own
 * skins/capes. Own user decision: the collection stays on this PC (`main/cape/capeLibrary.ts`),
 * only the one active cape is uploaded. `pendingCape` is a picked PNG awaiting its name before it's
 * saved to the collection; `previewCapeId` is whichever collection entry was last clicked.
 */
export function CapeScreen({ profile, onClose }: Props) {
  const t = useTranslations()
  const language = useLanguage()
  const activeSkin = profile.skins.find((s) => s.state === 'ACTIVE') ?? profile.skins[0] ?? null
  const activeSkinVariant: SkinVariant = activeSkin?.variant === 'SLIM' ? 'slim' : 'classic'

  const [skinPreview, setSkinPreview] = useState<string | null>(null)
  const [customCape, setCustomCape] = useState<CustomCapeStatus>({ exists: false, dataUri: null, sha1: null })
  const [capeLibrary, setCapeLibrary] = useState<CapeLibraryEntry[]>([])
  const [pendingCape, setPendingCape] = useState<{ dataUri: string; name: string } | null>(null)
  const [previewCapeId, setPreviewCapeId] = useState<string | null>(null)
  const [capeBusy, setCapeBusy] = useState(false)
  const [busyCapeId, setBusyCapeId] = useState<string | null>(null)
  const [capeError, setCapeError] = useState<string | null>(null)
  const [confirmingRemoveCape, setConfirmingRemoveCape] = useState(false)
  const [pendingDeleteCape, setPendingDeleteCape] = useState<CapeLibraryEntry | null>(null)
  const [converterOpen, setConverterOpen] = useState(false)
  const [converterPreview, setConverterPreview] = useState<string | null>(null)
  // Cape editor (own user request: draw capes yourself) - `entry: null` starts a new cape.
  const [editor, setEditor] = useState<{ entry: CapeLibraryEntry | null } | null>(null)

  useEffect(() => {
    setSkinPreview(null)
    if (!activeSkin) return
    window.api
      .fetchSkinTexture(activeSkin.url)
      .then(setSkinPreview)
      .catch((err) => setCapeError(formatError(err, t)))
  }, [activeSkin?.url])

  useEffect(() => {
    // No active cape (or the server being briefly unreachable) isn't worth alarming the user with
    // on every screen open - the collection below still works either way.
    window.api
      .getCapeStatus(profile)
      .then(setCustomCape)
      .catch(() => undefined)
    window.api
      .listCapeLibrary()
      .then(setCapeLibrary)
      .catch((err) => setCapeError(formatError(err, t)))
  }, [profile.id])

  async function handleSelectCapePng(): Promise<void> {
    setCapeError(null)
    try {
      const picked = await window.api.selectCapePng()
      if (picked) {
        const date = new Date().toLocaleDateString(language === 'de' ? 'de-DE' : 'en-US')
        setPendingCape({ dataUri: picked.dataUri, name: t.skin.capeDefaultName(date) })
      }
    } catch (err) {
      setCapeError(formatError(err, t))
    }
  }

  async function handleSaveCapeToCollection(): Promise<void> {
    if (!pendingCape) return
    setCapeBusy(true)
    setCapeError(null)
    try {
      const entry = await window.api.saveCapeToLibrary(pendingCape.dataUri, pendingCape.name.trim() || t.skin.capeNamePlaceholder)
      setCapeLibrary((current) => [...current, entry])
      setPreviewCapeId(entry.id)
      setPendingCape(null)
    } catch (err) {
      setCapeError(formatError(err, t))
    } finally {
      setCapeBusy(false)
    }
  }

  async function handleActivateCape(entry: CapeLibraryEntry): Promise<void> {
    setBusyCapeId(entry.id)
    setCapeError(null)
    try {
      const result = await window.api.activateLibraryCape(profile, entry.id)
      setCustomCape({ exists: true, dataUri: result.dataUri, sha1: result.sha1 })
      setPreviewCapeId(entry.id)
    } catch (err) {
      setCapeError(formatError(err, t))
    } finally {
      setBusyCapeId(null)
    }
  }

  async function handleConfirmDeleteCape(): Promise<void> {
    if (!pendingDeleteCape) return
    setBusyCapeId(pendingDeleteCape.id)
    setCapeError(null)
    try {
      await window.api.deleteCapeFromLibrary(pendingDeleteCape.id)
      setCapeLibrary((current) => current.filter((entry) => entry.id !== pendingDeleteCape.id))
      if (previewCapeId === pendingDeleteCape.id) setPreviewCapeId(null)
    } catch (err) {
      setCapeError(formatError(err, t))
    } finally {
      setBusyCapeId(null)
      setPendingDeleteCape(null)
    }
  }

  /** Takes the active cape off the server (others stop seeing it) - the collection entry stays, so
   * it can be activated again any time. Only confirmed when the active cape isn't in this PC's
   * collection (e.g. activated from another PC), because then it would really be gone. */
  async function handleDeactivateCape(): Promise<void> {
    setCapeBusy(true)
    setCapeError(null)
    try {
      await window.api.deleteCape(profile)
      setCustomCape({ exists: false, dataUri: null, sha1: null })
    } catch (err) {
      setCapeError(formatError(err, t))
    } finally {
      setCapeBusy(false)
      setConfirmingRemoveCape(false)
    }
  }

  const previewCape = capeLibrary.find((entry) => entry.id === previewCapeId) ?? null
  const shownCapeDataUri = pendingCape?.dataUri ?? converterPreview ?? previewCape?.dataUri ?? customCape.dataUri
  const activeLibraryCape = customCape.sha1 ? (capeLibrary.find((entry) => entry.sha1 === customCape.sha1) ?? null) : null

  if (editor) {
    return (
      <CapeEditorScreen
        entry={editor.entry}
        skinDataUri={skinPreview}
        skinVariant={activeSkinVariant}
        onSaved={(saved, isNew) => {
          setCapeLibrary((current) => (isNew ? [...current, saved] : current.map((entry) => (entry.id === saved.id ? saved : entry))))
          setPreviewCapeId(saved.id)
          setEditor(null)
        }}
        onClose={() => setEditor(null)}
      />
    )
  }

  return (
    <div className="mods-screen">
      <header>
        <strong>{t.skin.capeHeading}</strong>
        <button className="link-button" onClick={onClose}>
          {t.common.back}
        </button>
      </header>

      {capeError && <span className="error">{capeError}</span>}

      <section className="mods-section">
        <p className="version-warning">{t.skin.capeDescription}</p>
        <p className="version-warning">{t.skin.capeRequirements}</p>

        {skinPreview ? (
          <SkinModelPreview
            skinDataUri={skinPreview}
            variant={activeSkinVariant}
            capeDataUri={shownCapeDataUri}
            showCape={true}
            width={200}
            height={240}
          />
        ) : activeSkin ? (
          <p>{t.common.loading}</p>
        ) : (
          <p className="mods-empty">{t.skin.noSkin}</p>
        )}

        <p>{customCape.exists ? `${t.skin.capeActive}: ${activeLibraryCape?.name ?? '—'}` : t.skin.capeNoActive}</p>

        {pendingCape ? (
          <div className="cape-pending">
            <input
              type="text"
              className="skin-editor-name-input"
              value={pendingCape.name}
              onChange={(event) => setPendingCape({ ...pendingCape, name: event.target.value })}
              placeholder={t.skin.capeNamePlaceholder}
              autoFocus
            />
            <button className="primary-button" disabled={capeBusy} onClick={() => void handleSaveCapeToCollection()}>
              {capeBusy ? t.skin.saving : t.skin.capeSaveToCollection}
            </button>
            <button className="link-button" disabled={capeBusy} onClick={() => setPendingCape(null)}>
              {t.common.cancel}
            </button>
          </div>
        ) : converterOpen ? (
          <CapeConverter
            onPreview={setConverterPreview}
            onApply={(dataUri, name) => {
              setConverterOpen(false)
              setPendingCape({ dataUri, name })
            }}
            onCancel={() => setConverterOpen(false)}
          />
        ) : (
          <div className="cape-actions">
            <button className="secondary-button" onClick={() => void handleSelectCapePng()}>
              {t.skin.selectCapePng}
            </button>
            <button className="secondary-button" onClick={() => setConverterOpen(true)}>
              {t.skin.converterOpen}
            </button>
            <button className="secondary-button" onClick={() => setEditor({ entry: null })}>
              {t.capeEditor.open}
            </button>
            {customCape.exists && (
              <button
                className="link-button"
                disabled={capeBusy}
                onClick={() => (activeLibraryCape ? void handleDeactivateCape() : setConfirmingRemoveCape(true))}
              >
                {t.skin.capeRemoveActive}
              </button>
            )}
          </div>
        )}
      </section>

      <section className="mods-section">
        <h3>{t.skin.capeCollectionHeading}</h3>
        {capeLibrary.length === 0 ? (
          <p className="mods-empty">{t.skin.capeCollectionEmpty}</p>
        ) : (
          <>
            <p className="version-warning">{t.skin.capePreviewHint}</p>
            <ul className="skin-library-grid">
              {capeLibrary.map((entry) => {
                const isActive = entry.sha1 === customCape.sha1
                return (
                  <li
                    key={entry.id}
                    className={`skin-library-entry cape-library-entry${entry.id === previewCapeId ? ' cape-library-entry--selected' : ''}`}
                  >
                    <button className="cape-thumbnail-button" onClick={() => setPreviewCapeId(entry.id)} title={entry.name}>
                      <img className="cape-thumbnail" src={entry.dataUri} alt={entry.name} />
                    </button>
                    <span className="skin-library-name">{entry.name}</span>
                    <span className="cape-library-size">
                      {entry.width}x{entry.height}
                      {isActive && <strong className="cape-active-badge">{t.skin.capeActive}</strong>}
                    </span>
                    <div className="skin-library-actions">
                      {isActive ? (
                        <button className="secondary-button" disabled={capeBusy || busyCapeId !== null} onClick={() => void handleDeactivateCape()}>
                          {capeBusy ? t.skin.capeDeactivating : t.skin.capeDeactivate}
                        </button>
                      ) : (
                        <button
                          className="secondary-button"
                          disabled={capeBusy || busyCapeId !== null}
                          onClick={() => void handleActivateCape(entry)}
                        >
                          {busyCapeId === entry.id ? t.skin.capeActivating : t.skin.capeActivate}
                        </button>
                      )}
                      <button className="secondary-button" onClick={() => setEditor({ entry })}>
                        {t.capeEditor.edit}
                      </button>
                      <button className="link-button" disabled={busyCapeId !== null} onClick={() => setPendingDeleteCape(entry)}>
                        {t.common.delete}
                      </button>
                    </div>
                  </li>
                )
              })}
            </ul>
          </>
        )}
      </section>

      {pendingDeleteCape && (
        <ConfirmDialog
          message={t.skin.deleteCapeConfirm(pendingDeleteCape.name)}
          confirmLabel={t.common.delete}
          cancelLabel={t.common.cancel}
          busy={busyCapeId === pendingDeleteCape.id}
          onConfirm={() => void handleConfirmDeleteCape()}
          onCancel={() => setPendingDeleteCape(null)}
        />
      )}

      {confirmingRemoveCape && (
        <ConfirmDialog
          message={t.skin.removeCapeConfirm}
          confirmLabel={t.common.remove}
          cancelLabel={t.common.cancel}
          busy={capeBusy}
          onConfirm={() => void handleDeactivateCape()}
          onCancel={() => setConfirmingRemoveCape(false)}
        />
      )}
    </div>
  )
}
