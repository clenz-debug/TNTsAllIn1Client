import { useEffect, useRef } from 'react'
import type { PointerEvent as ReactPointerEvent } from 'react'

interface Props {
  color: string
  onChange: (color: string) => void
}

const WHEEL_SIZE = 140

/** A small, fixed palette of common colors for one-click picking - not an attempt to match any
 * particular reference palette, just a reasonable starting set. */
const SWATCHES = [
  '#ffffff',
  '#7c3aed',
  '#7c2d5a',
  '#1f9d55',
  '#a0522d',
  '#4b4b4b',
  '#c0c0c0',
  '#e6e6fa',
  '#e53e3e',
  '#f6ad55',
  '#f6e05e',
  '#68d391',
  '#63b3ed',
  '#9f7aea',
  '#f687b3',
  '#f5deb3'
]

function hsvToRgb(h: number, s: number, v: number): [number, number, number] {
  const c = v * s
  const x = c * (1 - Math.abs(((h / 60) % 2) - 1))
  const m = v - c
  let r = 0
  let g = 0
  let b = 0
  if (h < 60) [r, g, b] = [c, x, 0]
  else if (h < 120) [r, g, b] = [x, c, 0]
  else if (h < 180) [r, g, b] = [0, c, x]
  else if (h < 240) [r, g, b] = [0, x, c]
  else if (h < 300) [r, g, b] = [x, 0, c]
  else [r, g, b] = [c, 0, x]
  return [Math.round((r + m) * 255), Math.round((g + m) * 255), Math.round((b + m) * 255)]
}

function rgbToHsv(r: number, g: number, b: number): [number, number, number] {
  const rf = r / 255
  const gf = g / 255
  const bf = b / 255
  const max = Math.max(rf, gf, bf)
  const min = Math.min(rf, gf, bf)
  const d = max - min
  let h = 0
  if (d !== 0) {
    if (max === rf) h = (((gf - bf) / d) % 6) * 60
    else if (max === gf) h = ((bf - rf) / d + 2) * 60
    else h = ((rf - gf) / d + 4) * 60
    if (h < 0) h += 360
  }
  const s = max === 0 ? 0 : d / max
  return [h, s, max]
}

function rgbToHex(r: number, g: number, b: number): string {
  return `#${[r, g, b].map((channel) => channel.toString(16).padStart(2, '0')).join('')}`
}

function hexToRgb(hex: string): [number, number, number] | null {
  const match = /^#?([0-9a-f]{6})$/i.exec(hex.trim())
  if (!match) return null
  const value = parseInt(match[1], 16)
  return [(value >> 16) & 255, (value >> 8) & 255, value & 255]
}

/**
 * Custom hue/saturation wheel + value slider + hex field + swatches, replacing the plain native
 * `<input type="color">` this screen used before - own user feedback: modern Chromium's built-in
 * color-input popup has its own eyedropper button, which duplicates (and was found more annoying
 * than) this editor's dedicated "Pipette" tool. There is no way to disable just that one native
 * button, so the whole native picker is replaced with this component, which has no eyedropper at
 * all - sampling a color is exclusively the Pipette tool's job now.
 */
export function ColorPicker({ color, onChange }: Props) {
  const wheelCanvasRef = useRef<HTMLCanvasElement>(null)
  const rgb = hexToRgb(color) ?? [0, 0, 0]
  const [hue, saturation, value] = rgbToHsv(rgb[0], rgb[1], rgb[2])

  // Drawn once - the wheel image itself never changes, only the marker position on top of it does.
  useEffect(() => {
    const canvas = wheelCanvasRef.current
    const ctx = canvas?.getContext('2d')
    if (!canvas || !ctx) return
    const radius = WHEEL_SIZE / 2
    const image = ctx.createImageData(WHEEL_SIZE, WHEEL_SIZE)
    for (let y = 0; y < WHEEL_SIZE; y++) {
      for (let x = 0; x < WHEEL_SIZE; x++) {
        const dx = x - radius
        const dy = y - radius
        const dist = Math.sqrt(dx * dx + dy * dy)
        const idx = (y * WHEEL_SIZE + x) * 4
        if (dist > radius) {
          image.data[idx + 3] = 0
          continue
        }
        let angle = (Math.atan2(dy, dx) * 180) / Math.PI
        if (angle < 0) angle += 360
        const sat = Math.min(1, dist / radius)
        const [r, g, b] = hsvToRgb(angle, sat, 1)
        image.data[idx] = r
        image.data[idx + 1] = g
        image.data[idx + 2] = b
        image.data[idx + 3] = 255
      }
    }
    ctx.putImageData(image, 0, 0)
  }, [])

  function pickFromWheel(clientX: number, clientY: number): void {
    const canvas = wheelCanvasRef.current
    if (!canvas) return
    const rect = canvas.getBoundingClientRect()
    const radius = WHEEL_SIZE / 2
    const x = clientX - rect.left - radius
    const y = clientY - rect.top - radius
    const dist = Math.min(radius, Math.sqrt(x * x + y * y))
    let angle = (Math.atan2(y, x) * 180) / Math.PI
    if (angle < 0) angle += 360
    const sat = dist / radius
    const [r, g, b] = hsvToRgb(angle, sat, value)
    onChange(rgbToHex(r, g, b))
  }

  function handleWheelPointerDown(event: ReactPointerEvent<HTMLCanvasElement>): void {
    event.currentTarget.setPointerCapture(event.pointerId)
    pickFromWheel(event.clientX, event.clientY)
  }

  function handleWheelPointerMove(event: ReactPointerEvent<HTMLCanvasElement>): void {
    if (event.buttons !== 1) return
    pickFromWheel(event.clientX, event.clientY)
  }

  function handleValueChange(newValue: number): void {
    const [r, g, b] = hsvToRgb(hue, saturation, newValue)
    onChange(rgbToHex(r, g, b))
  }

  function handleHexInput(text: string): void {
    const parsed = hexToRgb(text)
    if (parsed) onChange(rgbToHex(parsed[0], parsed[1], parsed[2]))
  }

  function handleChannelInput(channel: 0 | 1 | 2, text: string): void {
    const parsed = Number(text)
    if (!Number.isFinite(parsed)) return
    const clamped = Math.min(255, Math.max(0, Math.round(parsed)))
    const next: [number, number, number] = [...rgb]
    next[channel] = clamped
    onChange(rgbToHex(next[0], next[1], next[2]))
  }

  const radius = WHEEL_SIZE / 2
  const markerAngleRad = (hue * Math.PI) / 180
  const markerDist = saturation * radius
  const markerX = radius + markerDist * Math.cos(markerAngleRad)
  const markerY = radius + markerDist * Math.sin(markerAngleRad)
  const [pureR, pureG, pureB] = hsvToRgb(hue, saturation, 1)

  return (
    <div className="color-picker">
      <div className="color-picker-wheel-wrap">
        <canvas
          ref={wheelCanvasRef}
          width={WHEEL_SIZE}
          height={WHEEL_SIZE}
          className="color-picker-wheel"
          onPointerDown={handleWheelPointerDown}
          onPointerMove={handleWheelPointerMove}
        />
        <div className="color-picker-wheel-marker" style={{ left: markerX, top: markerY }} />
      </div>

      <input
        type="range"
        min={0}
        max={100}
        value={Math.round(value * 100)}
        onChange={(event) => handleValueChange(Number(event.target.value) / 100)}
        className="color-picker-value-slider"
        style={{ background: `linear-gradient(to right, #000, rgb(${pureR}, ${pureG}, ${pureB}))` }}
      />

      <div className="color-picker-hex-row">
        <span>Hex</span>
        <input
          type="text"
          className="color-picker-hex-input"
          defaultValue={color}
          key={color}
          onBlur={(event) => handleHexInput(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') handleHexInput(event.currentTarget.value)
          }}
        />
        <span className="color-picker-current-swatch" style={{ background: color }} />
      </div>

      <div className="color-picker-rgb-row">
        {(['R', 'G', 'B'] as const).map((label, channel) => (
          <label key={label} className="color-picker-rgb-field">
            {label}
            <input
              type="number"
              min={0}
              max={255}
              className="color-picker-rgb-input"
              defaultValue={rgb[channel]}
              key={`${label}-${rgb[channel]}`}
              onBlur={(event) => handleChannelInput(channel as 0 | 1 | 2, event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') handleChannelInput(channel as 0 | 1 | 2, event.currentTarget.value)
              }}
            />
          </label>
        ))}
      </div>

      <div className="color-picker-swatches">
        {SWATCHES.map((swatch) => (
          <button
            key={swatch}
            type="button"
            className="color-picker-swatch"
            style={{ background: swatch }}
            onClick={() => onChange(swatch)}
            aria-label={swatch}
          />
        ))}
      </div>
    </div>
  )
}
