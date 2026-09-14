interface Props {
  layerLabel: string
  layerPrefix: 'base' | 'overlay'
  visibility: Record<string, boolean>
  onToggle: (id: string, value: boolean) => void
}

/**
 * Simple front-facing "paper doll" silhouette, one rectangle per body part - own user request,
 * replacing a checkbox list ("wie bei skinmc.net", two small clickable body diagrams instead of
 * checkboxes). Laid out as if looking *at* the character (matching skinview3d's own front view):
 * `right-arm`/`right-leg` are the character's own right side, which appears on the *viewer's*
 * left when facing them, same mirroring convention the 3D model itself uses - getting this
 * backwards here would make a diagram click hide the wrong (mirrored) arm/leg on the actual model.
 */
const PART_LAYOUT: Array<{ part: string; label: string; x: number; y: number; width: number; height: number }> = [
  { part: 'head', label: 'Kopf', x: 26, y: 2, width: 20, height: 20 },
  { part: 'body', label: 'Körper', x: 24, y: 24, width: 24, height: 38 },
  { part: 'right-arm', label: 'Rechter Arm', x: 10, y: 24, width: 12, height: 38 },
  { part: 'left-arm', label: 'Linker Arm', x: 50, y: 24, width: 12, height: 38 },
  { part: 'right-leg', label: 'Rechtes Bein', x: 24, y: 64, width: 11, height: 42 },
  { part: 'left-leg', label: 'Linkes Bein', x: 37, y: 64, width: 11, height: 42 }
]

export function BodyPartDiagram({ layerLabel, layerPrefix, visibility, onToggle }: Props) {
  return (
    <div className="body-part-diagram">
      <svg viewBox="0 0 72 110" className="body-part-diagram-svg">
        {PART_LAYOUT.map(({ part, label, x, y, width, height }) => {
          const id = `${layerPrefix}-${part}`
          const on = visibility[id] ?? true
          return (
            <rect
              key={id}
              x={x}
              y={y}
              width={width}
              height={height}
              rx={2}
              className={`body-part-shape${on ? ' on' : ' off'}`}
              onClick={() => onToggle(id, !on)}
            >
              <title>{label}</title>
            </rect>
          )
        })}
      </svg>
      <span className="body-part-diagram-label">{layerLabel}</span>
    </div>
  )
}
