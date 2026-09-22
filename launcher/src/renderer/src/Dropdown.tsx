import { useEffect, useRef, useState } from 'react'
import type { KeyboardEvent } from 'react'

export interface DropdownOption {
  value: string
  label: string
}

interface Props {
  value: string
  options: DropdownOption[]
  onChange: (value: string) => void
  disabled?: boolean
  id?: string
  ariaLabel?: string
  /** Extra class on the trigger button - lets a screen keep its own sizing rules (e.g. the
   * Instanz-picker's fixed-width trigger) without this component hardcoding them. */
  className?: string
}

/**
 * Replaces the native `<select>` everywhere in the app (own follow-up to the theme-color work) -
 * Chromium's Windows `<select>` popup hard-codes its hovered/currently-selected row to the OS
 * accent color (confirmed via a real native-window capture - `page.screenshot()` can't see an open
 * native popup at all, it's a separate OS-drawn surface outside the page's own render tree),
 * completely ignoring any author CSS there, including the classic `background: color
 * linear-gradient(...)` workaround. A custom-built dropdown has no such native popup - it's a
 * plain `<ul role="listbox">` this component fully controls, so its hover/selected row uses this
 * app's own theme colors like everything else.
 *
 * Deliberately minimal keyboard support (Up/Down/Enter/Escape) and click-outside-to-close, not a
 * full ARIA combobox pattern with typeahead - this app's dropdowns are all short, non-searchable
 * lists (instances, versions, a handful of sort orders), not the editable-autocomplete case a
 * general-purpose library would also need to cover.
 */
export function Dropdown({ value, options, onChange, disabled, id, ariaLabel, className }: Props) {
  const [open, setOpen] = useState(false)
  const [highlightedIndex, setHighlightedIndex] = useState(0)
  const containerRef = useRef<HTMLDivElement>(null)
  const triggerRef = useRef<HTMLButtonElement>(null)
  const menuRef = useRef<HTMLUListElement>(null)

  const selected = options.find((option) => option.value === value)

  useEffect(() => {
    if (!open) return
    // mousedown (not click) so this fires before an option's own onClick, and checked against the
    // whole container (trigger + menu) so clicking the trigger again or an option inside never
    // double-closes/re-opens via this listener - only a genuine outside click does.
    function handlePointerDown(event: MouseEvent): void {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', handlePointerDown)
    return () => document.removeEventListener('mousedown', handlePointerDown)
  }, [open])

  useEffect(() => {
    if (open) menuRef.current?.focus()
  }, [open])

  function openMenu(): void {
    if (disabled || options.length === 0) return
    const currentIndex = options.findIndex((option) => option.value === value)
    setHighlightedIndex(currentIndex >= 0 ? currentIndex : 0)
    setOpen(true)
  }

  function selectOption(option: DropdownOption): void {
    onChange(option.value)
    setOpen(false)
    triggerRef.current?.focus()
  }

  function handleTriggerKeyDown(event: KeyboardEvent<HTMLButtonElement>): void {
    if (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      openMenu()
    }
  }

  function handleMenuKeyDown(event: KeyboardEvent<HTMLUListElement>): void {
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setHighlightedIndex((index) => Math.min(options.length - 1, index + 1))
    } else if (event.key === 'ArrowUp') {
      event.preventDefault()
      setHighlightedIndex((index) => Math.max(0, index - 1))
    } else if (event.key === 'Enter') {
      event.preventDefault()
      const option = options[highlightedIndex]
      if (option) selectOption(option)
    } else if (event.key === 'Escape') {
      event.preventDefault()
      setOpen(false)
      triggerRef.current?.focus()
    }
  }

  return (
    <div className="dropdown" ref={containerRef}>
      <button
        ref={triggerRef}
        type="button"
        id={id}
        aria-label={ariaLabel}
        className={className ? `dropdown-trigger ${className}` : 'dropdown-trigger'}
        disabled={disabled || options.length === 0}
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => (open ? setOpen(false) : openMenu())}
        onKeyDown={handleTriggerKeyDown}
      >
        <span className="dropdown-trigger-label">{selected?.label ?? ''}</span>
        <span className="dropdown-trigger-arrow" aria-hidden="true" />
      </button>
      {open && (
        <ul className="dropdown-menu" role="listbox" tabIndex={-1} ref={menuRef} onKeyDown={handleMenuKeyDown}>
          {options.map((option, index) => (
            <li
              key={option.value}
              role="option"
              aria-selected={option.value === value}
              className={index === highlightedIndex ? 'dropdown-option highlighted' : 'dropdown-option'}
              onMouseEnter={() => setHighlightedIndex(index)}
              onClick={() => selectOption(option)}
            >
              {option.label}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
