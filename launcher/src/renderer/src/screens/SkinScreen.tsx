import { useEffect, useState } from 'react'
import { ConfirmDialog } from '../ConfirmDialog'
import { formatError } from '../formatError'
import { useLanguage, useTranslations } from '../i18n/LanguageContext'
import type { MinecraftProfile, SkinLibraryEntry, SkinVariant } from '../../../shared/types'
import { SkinModelPreview } from '../skinEditor/SkinModelPreview'

/** A freshly picked-but-not-yet-uploaded skin PNG, awaiting name + variant on the pending-upload
 * preview screen below - own user request: choose both there, after picking the file, instead of
 * the variant beforehand and the name afterward. */
interface PendingSkinUpload {
  dataUri: string
  variant: SkinVariant
  name: string
}

/** `loadSkinPngForEditor` hands back a `data:image/png;base64,...` URI (small enough to inline,
 * same reasoning as `SkinLibraryEntry.dataUri`) - the actual upload IPC needs raw bytes instead
 * (structured-clone carries `ArrayBuffer` across the context bridge natively, see
 * `skinBuffer.ts#canvasToPngBytes`), so this decodes the base64 payload back into one. */
function dataUriToArrayBuffer(dataUri: string): ArrayBuffer {
  const base64 = dataUri.slice(dataUri.indexOf(',') + 1)
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  return bytes.buffer
}

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

/** Mojang's texture CDN can take a moment to actually start serving a just-uploaded skin's brand
 * new URL - fetching it the instant the upload call returns occasionally 404s/errors even though
 * the account API itself already reports the new skin as ACTIVE. Retrying a few times with a
 * short pause gives that a chance to catch up instead of the preview silently staying stuck. */
async function fetchSkinTextureWithRetry(url: string, attempts = 5, delayMs = 1000): Promise<string> {
  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      return await window.api.fetchSkinTexture(url)
    } catch (err) {
      if (attempt === attempts) throw err
      await new Promise((resolve) => setTimeout(resolve, delayMs))
    }
  }
  throw new Error('unreachable')
}

/** Phase 7, step 1 of the roadmap's three-part order: view the current skin/cape and upload a
 * replacement PNG. Step 3 (the pixel editor, `SkinEditorScreen`) added a "Meine Skins" library
 * section here - Mojang's account API only ever has one active skin, so remembering every skin
 * the user has created/uploaded, letting them switch which one is active, and deleting old ones is
 * purely local bookkeeping done by this screen and `main/skin/skinLibrary.ts`. Renamed from "Skin &
 * Cape" to "Skin & Capes" and reworked so every skin (current + library) renders as a real,
 * drag-to-rotate 3D model instead of a flat thumbnail - own user request. */
export function SkinScreen({ profile, onProfileUpdate, onClose, onOpenEditor }: Props) {
  const t = useTranslations()
  const language = useLanguage()
  const activeSkin = profile.skins.find((s) => s.state === 'ACTIVE') ?? profile.skins[0] ?? null
  const activeCape = profile.capes.find((c) => c.state === 'ACTIVE') ?? null
  const activeSkinVariant: SkinVariant = activeSkin?.variant === 'SLIM' ? 'slim' : 'classic'

  const [skinPreview, setSkinPreview] = useState<string | null>(null)
  const [capePreview, setCapePreview] = useState<string | null>(null)
  // Single toggle shared by every rendered model (current + library) - the user explicitly wants
  // one "with/without cape" choice that applies everywhere, not a per-skin setting.
  const [showCape, setShowCape] = useState(true)
  // Bumped by every action that changes which skin is active (upload, "Verwenden") - forces the
  // preview effect below to refetch even if `activeSkin.url` happens to come back unchanged (own
  // user request after "Verwenden" didn't visibly update the 3D model): Mojang's skin URLs are
  // content-hashed, so switching to a skin whose bytes match one already seen would otherwise
  // never re-trigger a fetch by URL alone.
  const [skinRevision, setSkinRevision] = useState(0)
  // Set right after picking a file for direct upload - own user request: choose the name *and*
  // variant together here, after picking the file (not the variant beforehand and the name
  // afterward), on its own dedicated screen showing the picked skin as a live 3D model before it's
  // ever actually uploaded to Mojang. "Abbrechen"/the header back button both just discard this
  // without calling the upload IPC at all.
  const [pendingUpload, setPendingUpload] = useState<PendingSkinUpload | null>(null)
  const [uploadBusy, setUploadBusy] = useState(false)
  const [library, setLibrary] = useState<SkinLibraryEntry[]>([])
  const [libraryPage, setLibraryPage] = useState(0)
  const [busy, setBusy] = useState(false)
  const [busyLibraryId, setBusyLibraryId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  // Own themed replacement for window.confirm() (ConfirmDialog).
  const [pendingDeleteLibrary, setPendingDeleteLibrary] = useState<SkinLibraryEntry | null>(null)

  useEffect(() => {
    setSkinPreview(null)
    if (!activeSkin) return
    fetchSkinTextureWithRetry(activeSkin.url)
      .then(setSkinPreview)
      .catch((err) => setError(formatError(err, t)))
  }, [activeSkin?.url, skinRevision])

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
      .catch((err) => setError(formatError(err, t)))
  }, [])

  async function handleSelectSkinFile(): Promise<void> {
    setBusy(true)
    setError(null)
    try {
      const picked = await window.api.loadSkinPngForEditor()
      if (picked) {
        setPendingUpload({
          dataUri: picked.dataUri,
          variant: 'classic',
          name: t.skinEditor.defaultName(new Date().toLocaleDateString(language === 'de' ? 'de-DE' : 'en-US'))
        })
      }
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setBusy(false)
    }
  }

  async function handleConfirmUpload(): Promise<void> {
    if (!pendingUpload) return
    setUploadBusy(true)
    setError(null)
    try {
      const pngBytes = dataUriToArrayBuffer(pendingUpload.dataUri)
      const result = await window.api.uploadSkin(profile, pngBytes, pendingUpload.variant, pendingUpload.name)
      onProfileUpdate(result.profile)
      setSkinRevision((r) => r + 1)
      setLibrary(await window.api.listSkinLibrary())
      setPendingUpload(null)
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setUploadBusy(false)
    }
  }

  async function handleUseLibrarySkin(id: string): Promise<void> {
    setBusyLibraryId(id)
    setError(null)
    try {
      const updated = await window.api.useSkinFromLibrary(profile, id)
      if (updated) {
        onProfileUpdate(updated)
        setSkinRevision((r) => r + 1)
      }
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setBusyLibraryId(null)
    }
  }

  async function handleConfirmDeleteLibrarySkin(): Promise<void> {
    if (!pendingDeleteLibrary) return
    setBusyLibraryId(pendingDeleteLibrary.id)
    setError(null)
    try {
      await window.api.deleteSkinFromLibrary(pendingDeleteLibrary.id)
      setLibrary((current) => current.filter((e) => e.id !== pendingDeleteLibrary.id))
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setBusyLibraryId(null)
      setPendingDeleteLibrary(null)
    }
  }

  const totalPages = Math.max(1, Math.ceil(library.length / LIBRARY_PAGE_SIZE))
  const safePage = Math.min(libraryPage, totalPages - 1)
  const pageEntries = library.slice(safePage * LIBRARY_PAGE_SIZE, safePage * LIBRARY_PAGE_SIZE + LIBRARY_PAGE_SIZE)

  if (pendingUpload) {
    return (
      <div className="mods-screen">
        <header>
          <strong>{t.skin.renameTitle}</strong>
          <button className="link-button" disabled={uploadBusy} onClick={() => setPendingUpload(null)}>
            {t.common.back}
          </button>
        </header>

        {error && <span className="error">{error}</span>}

        <section className="mods-section skin-upload-confirm">
          <SkinModelPreview
            skinDataUri={pendingUpload.dataUri}
            variant={pendingUpload.variant}
            capeDataUri={null}
            showCape={false}
            width={220}
            height={260}
          />
          <input
            type="text"
            className="skin-editor-name-input"
            value={pendingUpload.name}
            onChange={(event) => setPendingUpload({ ...pendingUpload, name: event.target.value })}
            placeholder={t.skin.namePlaceholder}
            autoFocus
          />
          <label className="checkbox-label">
            <input
              type="radio"
              name="pending-upload-variant"
              checked={pendingUpload.variant === 'classic'}
              onChange={() => setPendingUpload({ ...pendingUpload, variant: 'classic' })}
            />
            {t.skin.variantClassic}
          </label>
          <label className="checkbox-label">
            <input
              type="radio"
              name="pending-upload-variant"
              checked={pendingUpload.variant === 'slim'}
              onChange={() => setPendingUpload({ ...pendingUpload, variant: 'slim' })}
            />
            {t.skin.variantSlim}
          </label>
          <div>
            <button className="primary-button" disabled={uploadBusy} onClick={() => void handleConfirmUpload()}>
              {uploadBusy ? t.skin.uploading : t.common.save}
            </button>
            <button className="link-button" disabled={uploadBusy} onClick={() => setPendingUpload(null)}>
              {t.common.cancel}
            </button>
          </div>
        </section>
      </div>
    )
  }

  return (
    <div className="mods-screen">
      <header>
        <strong>{t.skin.title}</strong>
        <button className="link-button" onClick={onClose}>
          {t.common.back}
        </button>
      </header>

      {error && <span className="error">{error}</span>}

      <section className="mods-section">
        <h3>{t.skin.currentHeading}</h3>
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
          <p>{t.common.loading}</p>
        ) : (
          <p className="mods-empty">{t.skin.noSkin}</p>
        )}
        {activeCape && (
          <label className="checkbox-label">
            <input type="checkbox" className="toggle-switch" checked={showCape} onChange={(event) => setShowCape(event.target.checked)} />
            {t.skin.showCape}
          </label>
        )}
      </section>

      <section className="mods-section">
        <h3>{t.skin.libraryHeading}</h3>
        <div>
          <button className="primary-button" onClick={() => onOpenEditor()}>
            {t.skin.createNew}
          </button>
        </div>
        {library.length === 0 ? (
          <p className="mods-empty">{t.skin.libraryEmpty}</p>
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
                      disabled={busyLibraryId === entry.id}
                      onClick={() => void handleUseLibrarySkin(entry.id)}
                    >
                      {t.skin.use}
                    </button>
                    <button className="secondary-button" disabled={busyLibraryId === entry.id} onClick={() => onOpenEditor(entry)}>
                      {t.skin.edit}
                    </button>
                    <button
                      className="link-button"
                      disabled={busyLibraryId === entry.id}
                      onClick={() => setPendingDeleteLibrary(entry)}
                    >
                      {t.common.delete}
                    </button>
                  </div>
                </li>
              ))}
            </ul>
            {totalPages > 1 && (
              <div className="skin-library-pagination">
                <button className="secondary-button" disabled={safePage === 0} onClick={() => setLibraryPage(safePage - 1)}>
                  {t.common.back}
                </button>
                <span>{t.skin.pageOf(safePage + 1, totalPages)}</span>
                <button
                  className="secondary-button"
                  disabled={safePage >= totalPages - 1}
                  onClick={() => setLibraryPage(safePage + 1)}
                >
                  {t.skin.next}
                </button>
              </div>
            )}
          </>
        )}
      </section>

      <section className="mods-section">
        <h3>{t.skin.uploadHeading}</h3>
        <div>
          <button className="secondary-button" onClick={() => void handleSelectSkinFile()} disabled={busy}>
            {busy ? t.common.loading : t.skin.selectAndUpload}
          </button>
        </div>
      </section>

      {pendingDeleteLibrary && (
        <ConfirmDialog
          message={t.skin.deleteLibraryConfirm(pendingDeleteLibrary.name)}
          confirmLabel={t.common.delete}
          cancelLabel={t.common.cancel}
          busy={busyLibraryId === pendingDeleteLibrary.id}
          onConfirm={() => void handleConfirmDeleteLibrarySkin()}
          onCancel={() => setPendingDeleteLibrary(null)}
        />
      )}

    </div>
  )
}
