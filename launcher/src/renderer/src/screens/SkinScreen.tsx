import { useEffect, useState } from 'react'
import type { MinecraftProfile } from '../../../shared/types'

interface Props {
  profile: MinecraftProfile
  onProfileUpdate: (profile: MinecraftProfile) => void
  onClose: () => void
}

/** Phase 7, step 1 of the roadmap's three-part order: view the current skin/cape and upload a
 * replacement PNG. No curated gallery or pixel editor yet - those are separate, later steps that
 * don't block this one. */
export function SkinScreen({ profile, onProfileUpdate, onClose }: Props) {
  const activeSkin = profile.skins.find((s) => s.state === 'ACTIVE') ?? profile.skins[0] ?? null
  const activeCape = profile.capes.find((c) => c.state === 'ACTIVE') ?? null

  const [skinPreview, setSkinPreview] = useState<string | null>(null)
  const [capePreview, setCapePreview] = useState<string | null>(null)
  const [variant, setVariant] = useState<'classic' | 'slim'>(activeSkin?.variant === 'SLIM' ? 'slim' : 'classic')
  const [busy, setBusy] = useState(false)
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

  async function handleUpload(): Promise<void> {
    setBusy(true)
    setError(null)
    try {
      const updated = await window.api.uploadSkin(profile, variant)
      if (updated) onProfileUpdate(updated)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="mods-screen">
      <header>
        <strong>Skin &amp; Cape</strong>
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
        {activeSkin ? (
          skinPreview ? (
            <img className="skin-preview" src={skinPreview} alt="Aktueller Skin" />
          ) : (
            <p>Lädt…</p>
          )
        ) : (
          <p className="mods-empty">Kein Skin gesetzt.</p>
        )}
      </section>

      <section className="mods-section">
        <h3>Aktuelles Cape</h3>
        {activeCape ? (
          capePreview ? (
            <img className="skin-preview" src={capePreview} alt="Aktuelles Cape" />
          ) : (
            <p>Lädt…</p>
          )
        ) : (
          <p className="mods-empty">Kein Cape.</p>
        )}
      </section>

      <section className="mods-section">
        <h3>Neuen Skin hochladen</h3>
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
