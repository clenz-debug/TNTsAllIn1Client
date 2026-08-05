import { useEffect, useState } from 'react'
import { isBundleCompatibleVersion, MINECRAFT_VERSION } from '../../../shared/types'

interface Props {
  selectedVersion: string
  disabledBundledMods: string[]
  onToggleBundledMod: (fileName: string, enabled: boolean) => void
  onClose: () => void
}

export function ModsScreen({ selectedVersion, disabledBundledMods, onToggleBundledMod, onClose }: Props) {
  const [bundledMods, setBundledMods] = useState<string[]>([])
  const [customMods, setCustomMods] = useState<string[]>([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    window.api.listBundledMods().then(setBundledMods).catch((err) => setError(String(err)))
  }, [])

  useEffect(() => {
    window.api.listCustomMods(selectedVersion).then(setCustomMods).catch((err) => setError(String(err)))
  }, [selectedVersion])

  async function handleAdd(): Promise<void> {
    setBusy(true)
    try {
      setCustomMods(await window.api.addCustomMods(selectedVersion))
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  async function handleRemove(fileName: string): Promise<void> {
    setBusy(true)
    try {
      setCustomMods(await window.api.removeCustomMod(selectedVersion, fileName))
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="mods-screen">
      <header>
        <strong>Mods</strong>
        <button className="link-button" onClick={onClose}>
          Zurück
        </button>
      </header>

      {error && <span className="error">{error}</span>}

      <section className="mods-section">
        <h3>Gebündelte Mods</h3>
        {!isBundleCompatibleVersion(selectedVersion) && (
          <p className="version-warning">
            Wirkt sich aktuell nicht aus - gebündelte Mods laufen nur bei {MINECRAFT_VERSION}, {selectedVersion}{' '}
            startet ohnehin ohne sie.
          </p>
        )}
        <ul className="mods-list">
          {bundledMods.map((fileName) => (
            <li key={fileName} className="mods-row">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={!disabledBundledMods.includes(fileName)}
                  onChange={(e) => onToggleBundledMod(fileName, e.target.checked)}
                />
                {fileName}
              </label>
            </li>
          ))}
          {bundledMods.length === 0 && <li className="mods-empty">Kein mods-bundle/ vorhanden.</li>}
        </ul>
      </section>

      <section className="mods-section">
        <h3>Eigene Mods ({selectedVersion})</h3>
        <button className="secondary-button" onClick={() => void handleAdd()} disabled={busy}>
          Mod hinzufügen…
        </button>
        <ul className="mods-list">
          {customMods.map((fileName) => (
            <li key={fileName} className="mods-row">
              <span>{fileName}</span>
              <button className="link-button" onClick={() => void handleRemove(fileName)} disabled={busy}>
                Entfernen
              </button>
            </li>
          ))}
          {customMods.length === 0 && <li className="mods-empty">Keine eigenen Mods hinzugefügt.</li>}
        </ul>
      </section>
    </div>
  )
}
