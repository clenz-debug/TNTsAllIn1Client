import { useEffect, useState } from 'react'
import type { RefObject } from 'react'

/**
 * Whether a flex-wrap container's last child has been pushed onto a new row - used by the skin and
 * cape editors to show "make the window wider" only while their tools (with the color palette)
 * actually sit below the canvas instead of next to it (own user request: no scrolling up and down
 * just to change color). Re-checked whenever the container resizes.
 */
export function useIsWrapped(ref: RefObject<HTMLElement | null>): boolean {
  const [wrapped, setWrapped] = useState(false)

  useEffect(() => {
    const container = ref.current
    if (!container) return
    const check = (): void => {
      const first = container.firstElementChild as HTMLElement | null
      const last = container.lastElementChild as HTMLElement | null
      setWrapped(!!first && !!last && first !== last && last.offsetTop > first.offsetTop + 1)
    }
    check()
    const observer = new ResizeObserver(check)
    observer.observe(container)
    return () => observer.disconnect()
  })

  return wrapped
}
