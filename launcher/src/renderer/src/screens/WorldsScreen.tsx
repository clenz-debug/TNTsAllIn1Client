import { useEffect, useState } from 'react'
import { formatError } from '../formatError'
import type { Instance } from '../../../shared/types'

interface PendingAction {
  world: string
  kind: 'copy' | 'move'
}

interface Props {
  instanceId: string
  instances: Instance[]
  onClose: () => void
}

/**
 * Standalone "Welten" screen, next to the existing "Mods…" button - own user request, follow-up to
 * an earlier attempt that buried this inside the Instances screen per-instance-row with an inline
 * `window.confirm()`; the user explicitly wanted its own button, world icons shown, and a proper
 * confirm-with-a-button dialog instead of a browser confirm() popup, so this replaces that version
 * rather than extending it.
 */
export function WorldsScreen({ instanceId, instances, onClose }: Props) {
  const [worlds, setWorlds] = useState<string[]>([])
  const [icons, setIcons] = useState<Record<string, string | null>>({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [status, setStatus] = useState<string | null>(null)

  const [pendingAction, setPendingAction] = useState<PendingAction | null>(null)
  const [targetInstanceId, setTargetInstanceId] = useState('')
  const [busy, setBusy] = useState(false)

  const otherInstances = instances.filter((instance) => instance.id !== instanceId)

  useEffect(() => {
    setLoading(true)
    setError(null)
    window.api
      .listInstanceWorlds(instanceId)
      .then(async (list) => {
        setWorlds(list)
        // One icon fetch per world, in parallel - a missing icon.png resolves to `null` rather
        // than rejecting (see `instanceManager.ts#getWorldIcon`), so this never needs its own
        // per-world error handling.
        const entries = await Promise.all(list.map(async (world) => [world, await window.api.getWorldIcon(instanceId, world)] as const))
        setIcons(Object.fromEntries(entries))
      })
      .catch((err) => setError(formatError(err)))
      .finally(() => setLoading(false))
  }, [instanceId])

  function openAction(world: string, kind: 'copy' | 'move'): void {
    setPendingAction({ world, kind })
    setTargetInstanceId(otherInstances[0]?.id ?? '')
    setStatus(null)
  }

  async function confirmAction(): Promise<void> {
    if (!pendingAction || !targetInstanceId) return
    const target = instances.find((instance) => instance.id === targetInstanceId)
    if (!target) return

    setBusy(true)
    setError(null)
    try {
      if (pendingAction.kind === 'move') {
        await window.api.moveWorldBetweenInstances(instanceId, pendingAction.world, targetInstanceId)
        setWorlds(await window.api.listInstanceWorlds(instanceId))
        setStatus(`Welt "${pendingAction.world}" nach "${target.name}" verschoben.`)
      } else {
        const { copiedTo } = await window.api.copyWorldBetweenInstances(instanceId, pendingAction.world, targetInstanceId)
        setStatus(`Welt "${pendingAction.world}" nach "${target.name}" kopiert (dort als "${copiedTo}").`)
      }
      setPendingAction(null)
    } catch (err) {
      setError(formatError(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="worlds-screen">
      <header>
        <strong>Welten</strong>
        <button className="link-button" onClick={onClose}>
          Zurück
        </button>
      </header>

      {error && <span className="error">{error}</span>}
      {status && <span className="status">{status}</span>}

      <section className="mods-section">
        <h3>Welten dieser Instanz</h3>
        <ul className="mods-list">
          {loading && <li className="mods-empty">Lädt…</li>}
          {!loading && worlds.length === 0 && <li className="mods-empty">Keine Welten in dieser Instanz.</li>}
          {!loading &&
            worlds.map((world) => (
              <li key={world} className="world-row">
                {icons[world] ? (
                  <img className="world-icon" src={icons[world] ?? undefined} alt="" />
                ) : (
                  <div className="world-icon-placeholder" />
                )}
                <div className="world-info">
                  <strong>{world}</strong>
                </div>
                <div className="header-actions">
                  <button
                    className="link-button"
                    onClick={() => openAction(world, 'copy')}
                    disabled={otherInstances.length === 0}
                  >
                    Kopieren
                  </button>
                  <button
                    className="link-button"
                    onClick={() => openAction(world, 'move')}
                    disabled={otherInstances.length === 0}
                  >
                    Verschieben
                  </button>
                </div>
              </li>
            ))}
        </ul>
        {otherInstances.length === 0 && (
          <p className="version-warning">
            Kopieren/Verschieben braucht mindestens eine weitere Instanz - lege dafür erst eine zweite an.
          </p>
        )}
      </section>

      {pendingAction && (
        <div className="modal-overlay">
          <div className="modal-box">
            <strong>
              Welt "{pendingAction.world}" {pendingAction.kind === 'move' ? 'verschieben' : 'kopieren'}
            </strong>
            <label className="checkbox-label">
              Ziel-Instanz:
              <select value={targetInstanceId} onChange={(e) => setTargetInstanceId(e.target.value)}>
                {otherInstances.map((instance) => (
                  <option key={instance.id} value={instance.id}>
                    {instance.name} ({instance.versionId})
                  </option>
                ))}
              </select>
            </label>
            {pendingAction.kind === 'move' && (
              <p className="version-warning">
                Achtung: Unterschiedliche Minecraft-Versionen oder Mods zwischen den Instanzen können diese Welt
                beschädigen oder zum Absturz führen (z.B. fehlende Blöcke/Items aus Mods, die in der Zielinstanz
                nicht installiert sind, oder ein Chunk-Format, das eine ältere Version nicht laden kann). Das
                geschieht auf eigene Gefahr und kann danach nicht rückgängig gemacht werden.
              </p>
            )}
            <div className="modal-actions">
              <button className="secondary-button" onClick={() => setPendingAction(null)} disabled={busy}>
                Abbrechen
              </button>
              <button className="primary-button" onClick={() => void confirmAction()} disabled={busy || !targetInstanceId}>
                {busy ? 'Wird ausgeführt…' : 'Bestätigen'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
