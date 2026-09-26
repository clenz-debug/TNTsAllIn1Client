import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import type { CSSProperties, WheelEvent } from 'react'
import { useTranslations } from '../i18n/LanguageContext'
import type { TourStep } from './tourSteps'

interface Box {
  top: number
  left: number
  width: number
  height: number
}

interface Size {
  width: number
  height: number
}

export interface FinishOffer {
  text: string
  yesLabel: string
  /** Ends the tour, like "Fertig". */
  noLabel: string
  onYes: () => void
}

interface Props {
  step: TourStep
  stepNumber: number
  stepCount: number
  canGoBack: boolean
  isLast: boolean
  /** Only for a step with `waitFor`: `'waiting'` until it happened, then `'done'`. */
  waitState: 'waiting' | 'done' | null
  /** Last step only: a question with its own yes/no instead of plain "Fertig" (continue in the game). */
  finishOffer: FinishOffer | null
  onBack: () => void
  onNext: () => void
  onEnd: () => void
}

const HOLE_PADDING = 6
const BUBBLE_GAP = 12
const EDGE_MARGIN = 12
/** How long a step looks for its target before showing its text in the middle of the window instead
 * (a screen still loading, or an element that doesn't exist in the current state). */
const TARGET_TIMEOUT_MS = 700

/** Bounding box around every element matching any of `selectors` - several sections can share one
 * spotlight (e.g. RAM + storage location in the settings). */
function measureTargets(selectors: string[]): { box: Box; elements: Element[] } | null {
  let top = Infinity
  let left = Infinity
  let right = -Infinity
  let bottom = -Infinity
  const elements: Element[] = []
  for (const selector of selectors) {
    document.querySelectorAll(selector).forEach((element) => {
      const rect = element.getBoundingClientRect()
      if (rect.width === 0 && rect.height === 0) return
      elements.push(element)
      top = Math.min(top, rect.top)
      left = Math.min(left, rect.left)
      right = Math.max(right, rect.right)
      bottom = Math.max(bottom, rect.bottom)
    })
  }
  return elements.length === 0 ? null : { box: { top, left, width: right - left, height: bottom - top }, elements }
}

function sameBox(a: Box | null, b: Box | null): boolean {
  if (a === null || b === null) return a === b
  return (
    Math.round(a.top) === Math.round(b.top) &&
    Math.round(a.left) === Math.round(b.left) &&
    Math.round(a.width) === Math.round(b.width) &&
    Math.round(a.height) === Math.round(b.height)
  )
}

/** Every sub-screen scrolls inside its own container (`.mods-screen` etc.), not the page. */
function findScrollParent(element: Element): HTMLElement | null {
  for (let node = element.parentElement; node; node = node.parentElement) {
    const overflowY = getComputedStyle(node).overflowY
    if ((overflowY === 'auto' || overflowY === 'scroll') && node.scrollHeight > node.clientHeight) return node
  }
  return null
}

/** Centers the target in its scroll container if it isn't fully visible yet; a target taller than
 * the container is scrolled to its top instead. */
function scrollIntoParent(parent: HTMLElement, box: Box): void {
  const view = parent.getBoundingClientRect()
  const top = view.top + EDGE_MARGIN
  const bottom = view.bottom - EDGE_MARGIN
  if (box.top >= top && box.top + box.height <= bottom) return
  const delta = box.height <= bottom - top ? box.top + box.height / 2 - (view.top + view.height / 2) : box.top - top
  parent.scrollBy({ top: delta, behavior: 'smooth' })
}

function holeFor(box: Box, viewport: Size): Box {
  const top = Math.max(box.top - HOLE_PADDING, 0)
  const left = Math.max(box.left - HOLE_PADDING, 0)
  const bottom = Math.min(box.top + box.height + HOLE_PADDING, viewport.height)
  const right = Math.min(box.left + box.width + HOLE_PADDING, viewport.width)
  return { top, left, width: Math.max(right - left, 0), height: Math.max(bottom - top, 0) }
}

/** Transparent click-catchers around the spotlight (and over it, unless the step is interactive). */
function blockerBoxes(hole: Box | null, viewport: Size, interactive: boolean): Box[] {
  if (!hole) return [{ top: 0, left: 0, width: viewport.width, height: viewport.height }]
  const bottom = hole.top + hole.height
  const right = hole.left + hole.width
  const boxes = [
    { top: 0, left: 0, width: viewport.width, height: hole.top },
    { top: bottom, left: 0, width: viewport.width, height: viewport.height - bottom },
    { top: hole.top, left: 0, width: hole.left, height: hole.height },
    { top: hole.top, left: right, width: viewport.width - right, height: hole.height }
  ]
  if (!interactive) boxes.push(hole)
  return boxes.filter((box) => box.width > 0 && box.height > 0)
}

/** Below the spotlight if it fits, else above, else beside it, else in the bottom right corner (only
 * for targets that fill nearly the whole window - every screen's content is left-aligned, so that
 * corner covers the least of it). Always kept inside the window. */
function placeBubble(hole: Box | null, size: Size, viewport: Size): { top: number; left: number } {
  const clampLeft = (left: number): number => Math.min(Math.max(left, EDGE_MARGIN), viewport.width - size.width - EDGE_MARGIN)
  const clampTop = (top: number): number => Math.min(Math.max(top, EDGE_MARGIN), viewport.height - size.height - EDGE_MARGIN)
  if (!hole) return { top: clampTop((viewport.height - size.height) / 2), left: clampLeft((viewport.width - size.width) / 2) }

  const centeredLeft = clampLeft(hole.left + hole.width / 2 - size.width / 2)
  const holeBottom = hole.top + hole.height
  if (holeBottom + BUBBLE_GAP + size.height <= viewport.height - EDGE_MARGIN) return { top: holeBottom + BUBBLE_GAP, left: centeredLeft }
  if (hole.top - BUBBLE_GAP - size.height >= EDGE_MARGIN) return { top: hole.top - BUBBLE_GAP - size.height, left: centeredLeft }

  const holeRight = hole.left + hole.width
  if (holeRight + BUBBLE_GAP + size.width <= viewport.width - EDGE_MARGIN) return { top: clampTop(hole.top), left: holeRight + BUBBLE_GAP }
  if (hole.left - BUBBLE_GAP - size.width >= EDGE_MARGIN) return { top: clampTop(hole.top), left: hole.left - BUBBLE_GAP - size.width }
  return { top: clampTop(viewport.height - size.height - EDGE_MARGIN), left: clampLeft(viewport.width - size.width - EDGE_MARGIN) }
}

function boxStyle(box: Box): CSSProperties {
  return { top: box.top, left: box.left, width: box.width, height: box.height }
}

/**
 * The guided tour's layer over the whole launcher (own user request): dims everything except the
 * step's target (followed every frame, so it stays right while screens load, scroll or resize),
 * blocks clicks and keys everywhere else - which is also what keeps the account menu from logging
 * out mid-tour - and shows the step's explanation next to the target.
 */
export function TourOverlay({ step, stepNumber, stepCount, canGoBack, isLast, waitState, finishOffer, onBack, onNext, onEnd }: Props) {
  const t = useTranslations()
  const texts = t.tour.steps[step.id]
  const interactive = step.interactive === true
  const offer = isLast ? finishOffer : null

  const [box, setBox] = useState<Box | null>(null)
  const [timedOut, setTimedOut] = useState(false)
  const [viewport, setViewport] = useState<Size>({ width: window.innerWidth, height: window.innerHeight })
  const [bubbleSize, setBubbleSize] = useState<Size | null>(null)
  const bubbleRef = useRef<HTMLDivElement>(null)
  const primaryRef = useRef<HTMLButtonElement>(null)
  const targetElementsRef = useRef<Element[]>([])
  const scrollParentRef = useRef<HTMLElement | null>(null)

  // Lifts open dropdown menus above the dimming, for the one interactive step (version picker).
  useEffect(() => {
    document.body.classList.add('tour-active')
    return () => document.body.classList.remove('tour-active')
  }, [])

  useEffect(() => {
    setBox(null)
    setTimedOut(false)
    targetElementsRef.current = []
    scrollParentRef.current = null
    let scrolled = false
    let frame = 0
    const timer = window.setTimeout(() => setTimedOut(true), TARGET_TIMEOUT_MS)
    const follow = (): void => {
      const measured = measureTargets(step.targets)
      targetElementsRef.current = measured?.elements ?? []
      if (measured && !scrolled) {
        scrolled = true
        scrollParentRef.current = findScrollParent(measured.elements[0])
        if (scrollParentRef.current) scrollIntoParent(scrollParentRef.current, measured.box)
      }
      const next = measured?.box ?? null
      setBox((previous) => (sameBox(previous, next) ? previous : next))
      setViewport((previous) =>
        previous.width === window.innerWidth && previous.height === window.innerHeight
          ? previous
          : { width: window.innerWidth, height: window.innerHeight }
      )
      frame = requestAnimationFrame(follow)
    }
    follow()
    return () => {
      cancelAnimationFrame(frame)
      window.clearTimeout(timer)
    }
  }, [step.id])

  const hole = box ? holeFor(box, viewport) : null
  const ready = (box !== null || timedOut) && bubbleSize !== null
  const position = bubbleSize ? placeBubble(hole, bubbleSize, viewport) : { top: 0, left: 0 }

  // Enter/Space go to "Weiter" right away; the interactive step keeps the focus where the page put it
  // (the new instance's name field). Waits for `ready` - a `visibility: hidden` bubble can't take focus.
  useEffect(() => {
    if (ready && !interactive) primaryRef.current?.focus({ preventScroll: true })
  }, [step.id, ready])

  // Keys only reach the bubble (and, in the interactive step, the spotlight) - otherwise a still
  // focused launcher button could be triggered with Enter behind the overlay.
  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent): void {
      if (event.key === 'Tab') return
      const target = event.target instanceof Node ? event.target : null
      if (target && bubbleRef.current?.contains(target)) return
      if (interactive && target && targetElementsRef.current.some((element) => element.contains(target))) return
      event.preventDefault()
      event.stopPropagation()
    }
    window.addEventListener('keydown', handleKeyDown, true)
    return () => window.removeEventListener('keydown', handleKeyDown, true)
  }, [interactive])

  useLayoutEffect(() => {
    const element = bubbleRef.current
    if (!element) return
    const size = { width: element.offsetWidth, height: element.offsetHeight }
    setBubbleSize((previous) => (previous && previous.width === size.width && previous.height === size.height ? previous : size))
  })

  // The blockers aren't inside the screen's scroll container, so scrolling over them is passed on -
  // tall sections stay readable.
  function forwardWheel(event: WheelEvent<HTMLDivElement>): void {
    scrollParentRef.current?.scrollBy({ top: event.deltaY })
  }

  return (
    <>
      {hole ? <div className="tour-hole" style={boxStyle(hole)} /> : <div className="tour-dim" />}
      {blockerBoxes(hole, viewport, interactive).map((blocker, index) => (
        <div key={index} className="tour-blocker" style={boxStyle(blocker)} onWheel={forwardWheel} />
      ))}
      <div
        ref={bubbleRef}
        className="tour-bubble"
        role="dialog"
        aria-modal="true"
        aria-labelledby="tour-bubble-title"
        style={{ top: position.top, left: position.left, visibility: ready ? 'visible' : 'hidden' }}
      >
        <span className="tour-counter">{t.tour.counter(stepNumber, stepCount)}</span>
        <strong id="tour-bubble-title" className="tour-title">
          {texts.title}
        </strong>
        <p>{texts.text}</p>
        {waitState === 'waiting' && (
          <p className="tour-hint">
            {t.tour.waitingForInstance}{' '}
            <button type="button" className="tour-inline-link" onClick={onNext}>
              {t.tour.skipWaiting}
            </button>
          </p>
        )}
        {waitState === 'done' && <p className="tour-hint tour-hint-done">{t.tour.instanceCreated}</p>}
        {offer && <p className="tour-offer">{offer.text}</p>}
        <div className="tour-actions">
          <button type="button" className="tour-inline-link tour-end" onClick={onEnd}>
            {t.tour.end}
          </button>
          {canGoBack && (
            <button type="button" className="secondary-button" onClick={onBack}>
              {t.tour.back}
            </button>
          )}
          {offer ? (
            <>
              <button type="button" className="secondary-button" onClick={onNext}>
                {offer.noLabel}
              </button>
              <button ref={primaryRef} type="button" className="primary-button" onClick={offer.onYes}>
                {offer.yesLabel}
              </button>
            </>
          ) : (
            <button ref={primaryRef} type="button" className="primary-button" onClick={onNext} disabled={waitState === 'waiting'}>
              {isLast ? t.tour.finish : t.tour.next}
            </button>
          )}
        </div>
      </div>
    </>
  )
}
