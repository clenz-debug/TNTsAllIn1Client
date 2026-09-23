import { useEffect, useState } from 'react'
import { ConfirmDialog } from '../ConfirmDialog'
import { formatError } from '../formatError'
import { useTranslations } from '../i18n/LanguageContext'
import type { ResourcepackEntry } from '../../../shared/types'

interface Props {
  instanceId: string
  instanceName: string
  onClose: () => void
}

/** What the confirm dialog is currently asking about - one pack by name, or all of them. */
type PendingRemoval = { kind: 'one'; name: string } | { kind: 'all' }

/**
 * "Texturepacks" screen next to "Mods…"/"Welten…" - own user request: list an instance's external
 * resource packs (the bundled ones stay out of it, see `resourcepacksManager.ts`), upload new ones,
 * remove one or all. Removing deletes the file for good, so both go through a confirm dialog first.
 */
export function ResourcepacksScreen({ instanceId, instanceName, onClose }: Props) {
  const t = useTranslations()
  const [packs, setPacks] = useState<ResourcepackEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [pendingRemoval, setPendingRemoval] = useState<PendingRemoval | null>(null)

  useEffect(() => {
    setLoading(true)
    setError(null)
    window.api
      .listResourcepacks(instanceId)
      .then(setPacks)
      .catch((err) => setError(formatError(err, t)))
      .finally(() => setLoading(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [instanceId])

  async function run(action: () => Promise<ResourcepackEntry[]>): Promise<void> {
    setBusy(true)
    setError(null)
    try {
      setPacks(await action())
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setBusy(false)
    }
  }

  async function confirmRemoval(): Promise<void> {
    if (!pendingRemoval) return
    const removal = pendingRemoval
    await run(() =>
      removal.kind === 'all'
        ? window.api.removeAllResourcepacks(instanceId)
        : window.api.removeResourcepack(instanceId, removal.name)
    )
    setPendingRemoval(null)
  }

  return (
    <div className="worlds-screen">
      <header>
        <strong>{t.resourcepacks.title}</strong>
        <button className="link-button" onClick={onClose}>
          {t.common.back}
        </button>
      </header>

      {error && <span className="error">{error}</span>}

      <section className="mods-section">
        <h3>{t.resourcepacks.heading(instanceName)}</h3>
        <p className="version-warning">{t.resourcepacks.info}</p>
        <div className="header-actions">
          <button
            className="secondary-button"
            onClick={() => void run(() => window.api.addResourcepacks(instanceId, t.resourcepacks.dialogTitle))}
            disabled={busy}
          >
            {t.resourcepacks.add}
          </button>
          <button
            className="secondary-button"
            onClick={() => setPendingRemoval({ kind: 'all' })}
            disabled={busy || packs.length === 0}
          >
            {t.resourcepacks.removeAll}
          </button>
        </div>
        <ul className="mods-list">
          {loading && <li className="mods-empty">{t.common.loading}</li>}
          {!loading && packs.length === 0 && <li className="mods-empty">{t.resourcepacks.empty}</li>}
          {!loading &&
            packs.map((pack) => (
              <li key={pack.name} className="world-row">
                {pack.iconDataUri ? (
                  <img className="world-icon" src={pack.iconDataUri} alt="" />
                ) : (
                  <div className="world-icon-placeholder" />
                )}
                <div className="world-info">
                  <strong>{pack.name}</strong>
                </div>
                <div className="header-actions">
                  <button className="link-button" onClick={() => setPendingRemoval({ kind: 'one', name: pack.name })} disabled={busy}>
                    {t.common.remove}
                  </button>
                </div>
              </li>
            ))}
        </ul>
      </section>

      {pendingRemoval && (
        <ConfirmDialog
          message={
            pendingRemoval.kind === 'all'
              ? t.resourcepacks.confirmRemoveAll(packs.length)
              : t.resourcepacks.confirmRemove(pendingRemoval.name)
          }
          confirmLabel={t.common.remove}
          cancelLabel={t.common.cancel}
          busy={busy}
          onConfirm={() => void confirmRemoval()}
          onCancel={() => setPendingRemoval(null)}
        />
      )}
    </div>
  )
}
