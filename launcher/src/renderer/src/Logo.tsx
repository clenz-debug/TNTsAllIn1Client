interface Props {
  className?: string
}

/**
 * The "N" client logo (own wishlist item: "das Logo für die Nutzerfarben anpassen, das wirkt cool
 * und individuell") - inline SVG, not an `<img src>`, specifically so its `fill`/`stroke`
 * `var(--...)` references inherit this page's own theme CSS custom properties and re-theme live
 * exactly like the rest of the UI whenever a color changes in the Settings screen. The octagon
 * outline, beam outlines and both text lines use `--bg-panel` (background2); the four beams use
 * `--green-1..4` (accent1-4), one each, per the reference mockup: top beam=accent1, left
 * leg=accent2, right leg=accent3, diagonal=accent4. No background fill at all - transparent both
 * outside and inside the octagon, so it sits on whatever it's placed over.
 *
 * `designs/branding/N-Logo.svg` is the equivalent static asset (same shape, flat default-theme
 * colors baked in) used for things that can't react live to a theme, e.g. the packaged app's icon
 * - keep both in sync if this shape itself ever changes.
 */
export function Logo({ className }: Props) {
  return (
    <svg viewBox="0 0 600 660" className={className} role="img" aria-label="TNT's All-In-1 Client">
      <polygon
        points="200,40 400,40 560,200 560,400 400,560 200,560 40,400 40,200"
        fill="none"
        stroke="var(--bg-panel)"
        strokeWidth={4}
      />

      <g stroke="var(--bg-panel)" strokeWidth={3} strokeLinejoin="round">
        {/* Top horizontal beam - Akzent 1 */}
        <rect x={150} y={150} width={300} height={50} fill="var(--green-1)" />
        {/* Left vertical leg - Akzent 2 */}
        <rect x={185} y={200} width={45} height={240} fill="var(--green-2)" />
        {/* Right vertical leg - Akzent 3 */}
        <rect x={370} y={200} width={45} height={240} fill="var(--green-3)" />
        {/* Diagonal beam connecting the two legs - Akzent 4 */}
        <polygon points="185,200 230,200 415,415 415,440 370,440 185,225" fill="var(--green-4)" />
      </g>

      <text
        x={300}
        y={500}
        textAnchor="middle"
        fontFamily="Arial, Helvetica, sans-serif"
        fontSize={72}
        fill="var(--bg-panel)"
      >
        A I <tspan fontSize={78}>1</tspan>
      </text>
      <text
        x={300}
        y={535}
        textAnchor="middle"
        fontFamily="Arial, Helvetica, sans-serif"
        fontSize={26}
        letterSpacing={4}
        fill="var(--bg-panel)"
      >
        CLIENT
      </text>
    </svg>
  )
}
