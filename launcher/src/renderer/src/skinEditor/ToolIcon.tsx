export type ToolIconName = 'pencil' | 'eraser' | 'fill' | 'eyedropper' | 'view' | 'undo' | 'redo'

/** Outline paths on a 24x24 grid, drawn in `currentColor` so they follow the theme's text color. */
const PATHS: Record<ToolIconName, string[]> = {
  pencil: ['M16 3.5 L20.5 8 L8 20.5 L3 21 L3.5 16 Z', 'M13.5 6 L18 10.5'],
  eraser: ['M9.5 21 L3 14.5 L13.5 4 L20 10.5 Z', 'M7.7 9.8 L14.2 16.3', 'M9.5 21 H21'],
  fill: [
    'M10.5 3.5 L18.5 11.5 L10.5 19.5 L2.5 11.5 Z',
    'M2.5 11.5 H18.5',
    'M8 6 L4.5 2.5',
    'M21 15 C21 15 19.2 17.4 19.2 18.6 A1.8 1.8 0 0 0 22.8 18.6 C22.8 17.4 21 15 21 15 Z'
  ],
  eyedropper: ['M13 8 L5 16 V19 H8 L16 11', 'M5 19 L3 21', 'M12.5 7.5 L16 4 A2.8 2.8 0 0 1 20 8 L16.5 11.5', 'M11 6 L18 13'],
  view: [
    'M12 2.5 L18 6 V13 L12 16.5 L6 13 V6 Z',
    'M6 6 L12 9.5 L18 6',
    'M12 9.5 V16.5',
    'M4 13.5 C1.5 17 6.5 20.5 12 20.5 C16 20.5 19.5 19 20.8 16.8',
    'M21 13.5 L20.8 16.8 L17.6 16.2'
  ],
  undo: ['M9 13 L4 8 L9 3', 'M4 8 H14.5 A5.5 5.5 0 0 1 14.5 19 H11'],
  redo: ['M15 13 L20 8 L15 3', 'M20 8 H9.5 A5.5 5.5 0 0 0 9.5 19 H13']
}

/**
 * Own user request: the skin and cape editors' tool buttons show symbols instead of the tools'
 * names. Drawn here rather than pulled from an icon library - seven small shapes don't justify a
 * new dependency (and its license notice in the credits). The tool's name stays on the button as
 * its tooltip and accessible label.
 */
export function ToolIcon({ name }: { name: ToolIconName }) {
  return (
    <svg
      className="tool-icon"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {PATHS[name].map((d) => (
        <path key={d} d={d} />
      ))}
    </svg>
  )
}
