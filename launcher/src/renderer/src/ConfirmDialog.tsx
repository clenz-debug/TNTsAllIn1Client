interface Props {
  message: string
  confirmLabel: string
  cancelLabel: string
  busy?: boolean
  onConfirm: () => void
  onCancel: () => void
}

/**
 * Themed replacement for the native `window.confirm()` popup - reuses the exact `.modal-overlay`/
 * `.modal-box`/`.modal-actions` classes `WorldsScreen`'s move/copy dialog already established, so
 * every confirmation in the app follows the current color theme (`var(--bg-panel)`/`var(--green-*)`
 * etc., see `global.css`) instead of an unthemed OS dialog - own user request
 * (`Projekt_Roadmap.md`'s wishlist: "im client design ... weil das dynamisch ist").
 */
export function ConfirmDialog({ message, confirmLabel, cancelLabel, busy, onConfirm, onCancel }: Props) {
  return (
    <div className="modal-overlay">
      <div className="modal-box">
        <p>{message}</p>
        <div className="modal-actions">
          <button className="secondary-button" onClick={onCancel} disabled={busy}>
            {cancelLabel}
          </button>
          <button className="primary-button" onClick={onConfirm} disabled={busy}>
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
