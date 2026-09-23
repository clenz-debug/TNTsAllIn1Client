import { useEffect, useRef, useState } from 'react'
import type { KeyboardEvent } from 'react'
import { PlayerHeadIcon } from './PlayerHeadIcon'

interface Props {
  name: string
  headTextureDataUri: string | null
  logoutLabel: string
  menuLabel: string
  onLogout: () => void
}

/**
 * Own user request ("vll dass man nicht den Abmelden-Button hat sondern man den Namen zu nem
 * Button macht und man dann eine Auswahl [bekommt], abmelden oder so") - replaces the header's
 * standalone "Abmelden" button and the plain-text player name with a single trigger (head icon +
 * name) that opens a small menu. Same open/close-on-outside-click/Escape interaction
 * {@link Dropdown} already established elsewhere in this app, just without Dropdown's "trigger
 * reflects the currently selected value" shape - this is a one-shot action menu, not a value
 * picker, so the trigger always shows the player's own identity regardless of what's in the menu.
 */
export function PlayerMenuButton({ name, headTextureDataUri, logoutLabel, menuLabel, onLogout }: Props) {
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    // mousedown (not click) so this fires before the logout item's own onClick, checked against the
    // whole container (trigger + menu) so clicking the trigger again never double-closes/re-opens
    // via this listener - only a genuine outside click does. Same pattern as Dropdown.
    function handlePointerDown(event: MouseEvent): void {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', handlePointerDown)
    return () => document.removeEventListener('mousedown', handlePointerDown)
  }, [open])

  function handleTriggerKeyDown(event: KeyboardEvent<HTMLButtonElement>): void {
    if (event.key === 'Escape') {
      event.preventDefault()
      setOpen(false)
    }
  }

  function handleLogoutClick(): void {
    setOpen(false)
    onLogout()
  }

  return (
    <div className="player-menu" ref={containerRef}>
      <button
        type="button"
        className="player-menu-trigger"
        aria-label={menuLabel}
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
        onKeyDown={handleTriggerKeyDown}
      >
        <PlayerHeadIcon textureDataUri={headTextureDataUri} size={24} />
        <strong>{name}</strong>
      </button>
      {open && (
        <ul className="player-menu-list" role="menu">
          <li role="none">
            <button type="button" role="menuitem" className="player-menu-item" onClick={handleLogoutClick}>
              {logoutLabel}
            </button>
          </li>
        </ul>
      )}
    </div>
  )
}
