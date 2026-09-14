import { useEffect, useState } from 'react'
import type { MinecraftProfile, SkinLibraryEntry, SkinVariant } from '../../../shared/types'
import { SkinModelPreview } from '../skinEditor/SkinModelPreview'

interface Props {
  profile: MinecraftProfile
  onProfileUpdate: (profile: MinecraftProfile) => void
  onClose: () => void
  /** Opens the pixel editor (`SkinEditorScreen`, owned by `PlayScreen` like every other secondary
   * screen) - omitted for "start a new skin", or a library entry for its "Bearbeiten" button. */
  onOpenEditor: (entry?: SkinLibraryEntry) => void
}

/** How many library entries get a live, rotatable 3D preview at once - each one is its own WebGL
 * context, and Chromium silently evicts the oldest past ~16 concurrent ones. Paginating (own user
 * request, over lazy-loading-on-scroll) keeps this well under that ceiling regardless of how many
 * skins accumulate over time - only the current page's entries ever get a `SkinModelPreview`
 * mounted, the rest are plain data until paged into view. */
const LIBRARY_PAGE_SIZE = 6

/** Phase 7, step 1 of the roadmap's three-part order: view the current skin/cape and upload a
 * replacement PNG. Step 3 (the pixel editor, `SkinEditorScreen`) added a "Meine Skins" library
 * section here - Mojang's account API only ever has one active skin, so remembering every skin
 * the user has created/uploaded, letting them switch which one is active, and deleting old ones is
 * purely local bookkeeping done by this screen and `main/skin/skinLibrary.ts`. Renamed from "Skin &
 * Cape" to "Skin & Capes" and reworked so every skin (current + library) renders as a real,
 * drag-to-rotate 3D model instead of a flat thumbnail - own user request. */
export function SkinScreen({ profile, onProfileUpdate, onClose, onOpenEditor }: Props) {
  const activeSkin = profile.skins.find((s) => s.state === 'ACTIVE') ?? profile.skins[0] ?? null
  const activeCape = profile.capes.find((c) => c.state === 'ACTIVE') ?? null
  const activeSkinVariant: SkinVariant = activeSkin?.variant === 'SLIM' ? 'slim' : 'classic'

  const [skinPreview, setSkinPreview] = useState<string | null>(null)
  const [capePreview, setCapePreview] = useState<string | null>(null)
  // Single toggle shared by every rendered model (current + library) - the user explicitly wants
  // one "with/without cape" choice that applies everywhere, not a per-skin setting.
  const [showCape, setShowCape] = useState(true)
  const [variant, setVariant] = useState<SkinVariant>(activeSkin?.variant === 'SLIM' ? 'slim' : 'classic')
  // Set right after a direct upload succeeds - own user request: name the skin *after* picking/
  // uploading the file, not before, on its own dedicated screen showing the uploaded skin as a
  // live 3D model (not just a text prompt). The upload itself starts with a placeholder name;
  // saving here calls SkinLibraryRename with the id already known from the upload result.
  const [pendingRename, setPendingRename] = useState<SkinLibraryEntry | null>(null)
  const [renamingBusy, setRenamingBusy] = useState(false)
  const [library, setLibrary] = useState<SkinLibraryEntry[]>([])
  const [libraryPage, setLibraryPage] = useState(0)
  const [busy, setBusy] = useState(false)
  const [busyLibraryId, setBusyLibraryId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setSkinPreview(null)
    if (!activeSkin) return
    window.api
      .fetchSkinTexture(activeSkin.url)
      .then(setSkinPreview)
      .catch((err) => setError(err instanceof Error ? err.message : String(err)))
  }, [activeSkin?.url])

  useEffect(() => {
    setCapePreview(null)
    if (!activeCape) return
    // A missing/failed cape preview isn't worth surfacing as an error the way a failed skin
    // preview is - it's secondary information, not something the user needs to act on.
    window.api.fetchSkinTexture(activeCape.url).then(setCapePreview).catch(() => undefined)
  }, [activeCape?.url])

  useEffect(() => {
    window.api
      .listSkinLibrary()
      .then(setLibrary)
      .catch((err) => setError(err instanceof Error ? err.message : String(err)))
  }, [])

  async function handleUpload(): Promise<void> {
    setBusy(true)
    setError(null)
    try {
      const result = await window.api.uploadSkin(profile, variant)
      if (result) {
        onProfileUpdate(result.profile)
        const updatedLibrary = await window.api.listSkinLibrary()
        setLibrary(updatedLibrary)
        const newEntry = updatedLibrary.find((entry) => entry.id === result.libraryEntryId)
        if (newEntry) setPendingRename(newEntry)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  async function handleConfirmRename(): Promise<void> {
    if (!pendingRename) return
    setRenamingBusy(true)
    setError(null)
    try {
      await window.api.renameSkinInLibrary(pendingRename.id, pendingRename.name)
      setLibrary(await window.api.listSkinLibrary())
      setPendingRename(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setRenamingBusy(false)
    }
  }

  async function handleUseLibrarySkin(id: string): Promise<void> {
    setBusyLibraryId(id)
    setError(null)
    try {
      const updated = await window.api.useSkinFromLibrary(profile, id)
      if (updated) onProfileUpdate(updated)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusyLibraryId(null)
    }
  }

  async function handleDeleteLibrarySkin(entry: SkinLibraryEntry): Promise<void> {
    const confirmed = window.confirm(`"${entry.name}" wirklich aus der Bibliothek löschen?`)
    if (!confirmed) return
    setBusyLibraryId(entry.id)
    setError(null)
    try {
      await window.api.deleteSkinFromLibrary(entry.id)
      setLibrary((current) => current.filter((e) => e.id !== entry.id))
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusyLibraryId(null)
    }
  }

  const totalPages = Math.max(1, Math.ceil(library.length / LIBRARY_PAGE_SIZE))
  const safePage = Math.min(libraryPage, totalPages - 1)
  const pageEntries = library.slice(safePage * LIBRARY_PAGE_SIZE, safePage * LIBRARY_PAGE_SIZE + LIBRARY_PAGE_SIZE)

  if (pendingRename) {
    return (
      <div className="mods-screen">
        <header>
          <strong>Skin benennen</strong>
          <button className="link-button" onClick={() => setPendingRename(null)}>
            Zurück
          </button>
        </header>

        {error && <span className="error">{error}</span>}

        <section className="mods-section skin-upload-confirm">
          <SkinModelPreview
            skinDataUri={pendingRename.dataUri}
            variant={pendingRename.variant}
            capeDataUri={null}
            showCape={false}
            width={220}
            height={260}
          />
          <input
            type="text"
            className="skin-editor-name-input"
            value={pendingRename.name}
            onChange={(event) => setPendingRename({ ...pendingRename, name: event.target.value })}
            placeholder="Name des Skins"
            autoFocus
          />
          <button className="primary-button" disabled={renamingBusy} onClick={() => void handleConfirmRename()}>
            {renamingBusy ? 'Speichert…' : 'Speichern'}
          </button>
        </section>
      </div>
    )
  }

  return (
    <div className="mods-screen">
      <header>
        <strong>Skin &amp; Capes</strong>
        <button className="link-button" onClick={onClose}>
          Zurück
        </button>
      </header>

      {profile.isMock && (
        <p className="version-warning">
          Skin-Verwaltung braucht die echte Mojang-API-Freischaltung (aktuell im Dev-Mock-Modus) - die Vorschau
          funktioniert schon, ein echter Upload noch nicht.
        </p>
      )}

      {error && <span className="error">{error}</span>}

      <section className="mods-section">
        <h3>Aktueller Skin</h3>
        {activeSkin && skinPreview ? (
          <SkinModelPreview
            skinDataUri={skinPreview}
            variant={activeSkinVariant}
            capeDataUri={capePreview}
            showCape={showCape}
            width={200}
            height={240}
          />
        ) : activeSkin ? (
          <p>Lädt…</p>
        ) : (
          <p className="mods-empty">Kein Skin gesetzt.</p>
        )}
        {activeCape && (
          <label className="checkbox-label">
            <input type="checkbox" checked={showCape} onChange={(event) => setShowCape(event.target.checked)} />
            Cape anzeigen (gilt für alle Skins hier)
          </label>
        )}
      </section>

      <section className="mods-section">
        <h3>Meine Skins</h3>
        <div>
          <button className="primary-button" onClick={() => onOpenEditor()}>
            Neuen Skin erstellen
          </button>
        </div>
        {library.length === 0 ? (
          <p className="mods-empty">Noch keine Skins erstellt.</p>
        ) : (
          <>
            <ul className="skin-library-grid">
              {pageEntries.map((entry) => (
                <li key={entry.id} className="skin-library-entry">
                  <SkinModelPreview
                    skinDataUri={entry.dataUri}
                    variant={entry.variant}
                    capeDataUri={capePreview}
                    showCape={showCape}
                    width={110}
                    height={130}
                  />
                  <span className="skin-library-name">{entry.name}</span>
                  <div className="skin-library-actions">
                    <button
                      className="secondary-button"
                      disabled={busyLibraryId === entry.id || profile.isMock}
                      onClick={() => void handleUseLibrarySkin(entry.id)}
                    >
                      Verwenden
                    </button>
                    <button className="secondary-button" disabled={busyLibraryId === entry.id} onClick={() => onOpenEditor(entry)}>
                      Bearbeiten
                    </button>
                    <button
                      className="link-button"
                      disabled={busyLibraryId === entry.id}
                      onClick={() => void handleDeleteLibrarySkin(entry)}
                    >
                      Löschen
                    </button>
                  </div>
                </li>
              ))}
            </ul>
            {totalPages > 1 && (
              <div className="skin-library-pagination">
                <button className="secondary-button" disabled={safePage === 0} onClick={() => setLibraryPage(safePage - 1)}>
                  Zurück
                </button>
                <span>
                  Seite {safePage + 1} / {totalPages}
                </span>
                <button
                  className="secondary-button"
                  disabled={safePage >= totalPages - 1}
                  onClick={() => setLibraryPage(safePage + 1)}
                >
                  Weiter
                </button>
              </div>
            )}
          </>
        )}
      </section>

      <section className="mods-section">
        <h3>Direkt hochladen</h3>
        <label className="checkbox-label">
          <input type="radio" name="variant" checked={variant === 'classic'} onChange={() => setVariant('classic')} />
          Classic (Steve-Arme)
        </label>
        <label className="checkbox-label">
          <input type="radio" name="variant" checked={variant === 'slim'} onChange={() => setVariant('slim')} />
          Slim (Alex-Arme)
        </label>
        <div>
          <button className="secondary-button" onClick={() => void handleUpload()} disabled={busy || profile.isMock}>
            {busy ? 'Lädt hoch…' : 'PNG auswählen & hochladen'}
          </button>
        </div>
      </section>
    </div>
  )
}
