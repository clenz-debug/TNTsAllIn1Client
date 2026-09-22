import { useEffect, useState } from 'react'
import { Dropdown } from '../Dropdown'
import { formatError } from '../formatError'
import { useTranslations } from '../i18n/LanguageContext'
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
  const t = useTranslations()
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
      .catch((err) => setError(formatError(err, t)))
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
        setStatus(t.worlds.movedStatus(pendingAction.world, target.name))
      } else {
        const { copiedTo } = await window.api.copyWorldBetweenInstances(instanceId, pendingAction.world, targetInstanceId)
        setStatus(t.worlds.copiedStatus(pendingAction.world, target.name, copiedTo))
      }
      setPendingAction(null)
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="worlds-screen">
      <header>
        <strong>{t.worlds.title}</strong>
        <button className="link-button" onClick={onClose}>
          {t.common.back}
        </button>
      </header>

      {error && <span className="error">{error}</span>}
      {status && <span className="status">{status}</span>}

      <section className="mods-section">
        <h3>{t.worlds.heading}</h3>
        <ul className="mods-list">
          {loading && <li className="mods-empty">{t.common.loading}</li>}
          {!loading && worlds.length === 0 && <li className="mods-empty">{t.worlds.empty}</li>}
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
                    {t.worlds.copy}
                  </button>
                  <button
                    className="link-button"
                    onClick={() => openAction(world, 'move')}
                    disabled={otherInstances.length === 0}
                  >
                    {t.worlds.move}
                  </button>
                </div>
              </li>
            ))}
        </ul>
        {otherInstances.length === 0 && <p className="version-warning">{t.worlds.needsAnotherInstance}</p>}
      </section>

      {pendingAction && (
        <div className="modal-overlay">
          <div className="modal-box">
            <strong>
              {pendingAction.kind === 'move' ? t.worlds.actionTitleMove(pendingAction.world) : t.worlds.actionTitleCopy(pendingAction.world)}
            </strong>
            <label className="checkbox-label">
              {t.worlds.targetInstanceLabel}
              <Dropdown
                value={targetInstanceId}
                onChange={setTargetInstanceId}
                options={otherInstances.map((instance) => ({
                  value: instance.id,
                  label: `${instance.name} (${instance.versionId})`
                }))}
              />
            </label>
            {pendingAction.kind === 'move' && <p className="version-warning">{t.worlds.moveWarning}</p>}
            <div className="modal-actions">
              <button className="secondary-button" onClick={() => setPendingAction(null)} disabled={busy}>
                {t.common.cancel}
              </button>
              <button className="primary-button" onClick={() => void confirmAction()} disabled={busy || !targetInstanceId}>
                {busy ? t.worlds.working : t.worlds.confirm}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
