import { useEffect, useState } from 'react'
import { ConfirmDialog } from '../ConfirmDialog'
import { formatError } from '../formatError'
import { useTranslations } from '../i18n/LanguageContext'
import type { CapeDecision, CapeModerationState, CapeReport } from '../../../shared/types'

/**
 * Reporting a player's custom cape - shown to everyone at the bottom of the cape screen. Reports
 * go by Minecraft name, because that is what a player sees above the cape in the game.
 */
export function CapeReportForm() {
  const t = useTranslations()
  const [name, setName] = useState('')
  const [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [status, setStatus] = useState<string | null>(null)

  async function send(): Promise<void> {
    const target = name.trim()
    setBusy(true)
    setError(null)
    setStatus(null)
    try {
      await window.api.reportCape(target, reason)
      setStatus(t.capeReport.sent(target))
      setName('')
      setReason('')
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="mods-section" data-tour="capes-report">
      <h3>{t.capeReport.heading}</h3>
      <p className="version-warning">{t.capeReport.info}</p>
      <div className="mods-search-row">
        <input
          className="skin-editor-name-input"
          value={name}
          maxLength={16}
          placeholder={t.capeReport.namePlaceholder}
          onChange={(e) => setName(e.target.value)}
        />
        <input
          className="skin-editor-name-input"
          value={reason}
          maxLength={200}
          placeholder={t.capeReport.reasonPlaceholder}
          onChange={(e) => setReason(e.target.value)}
        />
        <button className="secondary-button" disabled={busy || !name.trim()} onClick={() => void send()}>
          {t.capeReport.send}
        </button>
      </div>
      {error && <span className="error">{error}</span>}
      {status && <span className="status">{status}</span>}
    </section>
  )
}

/**
 * The moderators' side: open reports with the cape as it was reported, and the accounts barred from
 * uploading. Only rendered for an account the server names as moderator - and the server checks
 * that again on every call, so showing this to anyone else would only show them errors.
 */
export function CapeModeration() {
  const t = useTranslations()
  const [state, setState] = useState<CapeModerationState | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState<{ report: CapeReport; decision: CapeDecision } | null>(null)
  const [banReason, setBanReason] = useState('')

  async function run(work: () => Promise<CapeModerationState>): Promise<void> {
    setBusy(true)
    setError(null)
    try {
      setState(await work())
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setBusy(false)
    }
  }

  useEffect(() => {
    void run(() => window.api.listCapeReports())
  }, [])

  function confirmText(report: CapeReport, decision: CapeDecision): string {
    if (decision === 'dismiss') return t.moderation.confirmDismiss(report.name)
    if (decision === 'remove') return t.moderation.confirmRemove(report.name)
    return t.moderation.confirmBan(report.name)
  }

  return (
    <section className="mods-section">
      <h3>{t.moderation.heading(state?.reports.length ?? 0)}</h3>
      <p className="version-warning">{t.moderation.info}</p>
      <div>
        <button className="link-button" disabled={busy} onClick={() => void run(() => window.api.listCapeReports())}>
          {t.moderation.refresh}
        </button>
      </div>
      {error && <span className="error">{error}</span>}

      <ul className="mods-list">
        {!state && !error && <li className="mods-empty">{t.common.loading}</li>}
        {state && state.reports.length === 0 && <li className="mods-empty">{t.moderation.empty}</li>}
        {state?.reports.map((report) => (
          <li key={`${report.target}/${report.sha}`} className="world-row">
            {report.dataUri && <img className="cape-thumbnail" src={report.dataUri} alt={report.name} />}
            <div className="world-info">
              <strong>{report.name}</strong>
              <span className="friend-activity">
                {t.moderation.reportCount(report.count)} · {report.current ? t.moderation.stillWorn : t.moderation.replaced}
              </span>
              {report.reasons.map((reason, index) => (
                <span key={index} className="friend-activity">
                  „{reason}“
                </span>
              ))}
            </div>
            <div className="header-actions">
              <button className="link-button" disabled={busy} onClick={() => setPending({ report, decision: 'dismiss' })}>
                {t.moderation.dismiss}
              </button>
              <button className="link-button" disabled={busy} onClick={() => setPending({ report, decision: 'remove' })}>
                {t.moderation.remove}
              </button>
              <button
                className="link-button"
                disabled={busy}
                onClick={() => {
                  setBanReason(report.reasons[0] ?? '')
                  setPending({ report, decision: 'ban' })
                }}
              >
                {t.moderation.ban}
              </button>
            </div>
          </li>
        ))}
      </ul>

      {state && state.bans.length > 0 && (
        <>
          <h3>{t.moderation.bansHeading(state.bans.length)}</h3>
          <ul className="mods-list">
            {state.bans.map((ban) => (
              <li key={ban.uuid} className="world-row">
                <div className="world-info">
                  <strong>{ban.name ?? ban.uuid}</strong>
                  <span className="friend-activity">{ban.reason}</span>
                </div>
                <div className="header-actions">
                  <button className="link-button" disabled={busy} onClick={() => void run(() => window.api.liftCapeBan(ban.uuid))}>
                    {t.moderation.unban}
                  </button>
                </div>
              </li>
            ))}
          </ul>
        </>
      )}

      {pending && pending.decision === 'ban' && (
        <div className="modal-overlay">
          <div className="modal-box">
            <p>{confirmText(pending.report, 'ban')}</p>
            <input
              className="skin-editor-name-input"
              value={banReason}
              maxLength={200}
              placeholder={t.moderation.banReasonPlaceholder}
              onChange={(e) => setBanReason(e.target.value)}
              autoFocus
            />
            <div className="modal-actions">
              <button className="secondary-button" disabled={busy} onClick={() => setPending(null)}>
                {t.common.cancel}
              </button>
              <button
                className="primary-button"
                disabled={busy || !banReason.trim()}
                onClick={() => {
                  const target = pending.report.target
                  setPending(null)
                  void run(() => window.api.decideCapeReports(target, 'ban', banReason))
                }}
              >
                {t.moderation.ban}
              </button>
            </div>
          </div>
        </div>
      )}
      {pending && pending.decision !== 'ban' && (
        <ConfirmDialog
          message={confirmText(pending.report, pending.decision)}
          confirmLabel={pending.decision === 'dismiss' ? t.moderation.dismiss : t.moderation.remove}
          cancelLabel={t.common.cancel}
          busy={busy}
          onConfirm={() => {
            const { report, decision } = pending
            setPending(null)
            void run(() => window.api.decideCapeReports(report.target, decision, ''))
          }}
          onCancel={() => setPending(null)}
        />
      )}
    </section>
  )
}
