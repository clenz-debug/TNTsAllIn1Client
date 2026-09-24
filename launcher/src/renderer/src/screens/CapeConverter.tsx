import { useEffect, useRef, useState } from 'react'
import {
  CAPE_CONVERTER_WIDTHS,
  dataUriByteLength,
  defaultCrop,
  loadImageFile,
  renderCapeFromImage,
  type CapeConverterOptions,
  type CapeFitMode,
  type CapeInsideMode
} from '../capeConverter'
import { Dropdown } from '../Dropdown'
import { useTranslations } from '../i18n/LanguageContext'
import { ColorPicker } from '../skinEditor/ColorPicker'
import { CapeCropSelector } from './CapeCropSelector'

/** Same ceiling the launcher and the cape server enforce - checked here so a too-detailed photo at
 * 2048x1024 is caught before the naming step, with a hint to pick a smaller resolution. */
const CAPE_MAX_BYTES = 5 * 1024 * 1024

interface Props {
  /** Live result for the 3D preview in `CapeScreen` - `null` while no picture is loaded. */
  onPreview: (dataUri: string | null) => void
  /** Hands the finished cape to `CapeScreen`'s normal "name it, save to collection" step. */
  onApply: (dataUri: string, suggestedName: string) => void
  onCancel: () => void
}

/** Classic eyedropper glyph, drawn in the current text color so it follows the theme. */
function EyedropperIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M14.5 4.5l5 5" />
      <path d="M17.2 2.8a2.1 2.1 0 0 1 3 3l-2.7 2.7-3-3z" />
      <path d="M14.5 7.5l-9.3 9.3a2 2 0 0 0-.5.9L4 21l3.3-.7a2 2 0 0 0 .9-.5l9.3-9.3" />
    </svg>
  )
}

/** Picture-to-cape converter (own user request), shown inside `CapeScreen`. */
export function CapeConverter({ onPreview, onApply, onCancel }: Props) {
  const t = useTranslations()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [image, setImage] = useState<HTMLImageElement | null>(null)
  const [fileName, setFileName] = useState('')
  const [options, setOptions] = useState<CapeConverterOptions>({
    width: 512,
    fit: 'cover',
    inside: 'mirror',
    background: '#202020',
    pixelated: false,
    crop: null
  })
  const [result, setResult] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  // Eyedropper for the border color (own user request): armed by its button, disarmed after one pick.
  const [pipetteActive, setPipetteActive] = useState(false)

  useEffect(() => {
    if (!image) {
      setResult(null)
      onPreview(null)
      return
    }
    // Short delay: dragging the crop frame changes the options on every pointer move, and a
    // 2048x1024 PNG encode per move would make the drag stutter.
    const timer = setTimeout(() => {
      const dataUri = renderCapeFromImage(image, options)
      setResult(dataUri)
      onPreview(dataUri)
    }, 60)
    return () => clearTimeout(timer)
  }, [image, options])

  // Leaving the converter must not keep its last result stuck in CapeScreen's preview.
  useEffect(() => () => onPreview(null), [])

  async function handleFileChosen(file: File | undefined): Promise<void> {
    if (!file) return
    setError(null)
    try {
      const loaded = await loadImageFile(file)
      setImage(loaded)
      setOptions((current) => ({ ...current, crop: defaultCrop(loaded.width, loaded.height) }))
      setFileName(file.name.replace(/\.[^.]+$/, ''))
    } catch {
      setError(t.skin.converterLoadFailed)
    }
  }

  const tooLarge = result !== null && dataUriByteLength(result) > CAPE_MAX_BYTES
  const update = (patch: Partial<CapeConverterOptions>): void => setOptions((current) => ({ ...current, ...patch }))

  return (
    <div className="cape-converter">
      <h3>{t.skin.converterHeading}</h3>
      <p className="version-warning">{t.skin.converterDescription}</p>
      {error && <span className="error">{error}</span>}

      <input
        ref={fileInputRef}
        type="file"
        accept="image/png,image/jpeg,image/webp,image/gif,image/bmp"
        hidden
        onChange={(event) => {
          void handleFileChosen(event.target.files?.[0])
          event.target.value = ''
        }}
      />
      <div>
        <button className="secondary-button" onClick={() => fileInputRef.current?.click()}>
          {image ? t.skin.converterChangeImage : t.skin.converterSelectImage}
        </button>
        {fileName && <span className="cape-converter-file">{fileName}</span>}
      </div>

      {image && (
        <div className="cape-converter-options">
          <label className="cape-converter-row">
            <span>{t.skin.converterResolution}</span>
            <Dropdown
              value={String(options.width)}
              onChange={(value) => update({ width: Number(value) })}
              options={CAPE_CONVERTER_WIDTHS.map((width) => ({ value: String(width), label: `${width}x${width / 2}` }))}
              ariaLabel={t.skin.converterResolution}
            />
          </label>
          <label className="cape-converter-row">
            <span>{t.skin.converterFit}</span>
            <Dropdown
              value={options.fit}
              onChange={(value) => update({ fit: value as CapeFitMode })}
              options={[
                { value: 'cover', label: t.skin.converterFitCover },
                { value: 'contain', label: t.skin.converterFitContain }
              ]}
              ariaLabel={t.skin.converterFit}
            />
          </label>
          <CapeCropSelector
            image={image}
            crop={options.fit === 'cover' ? options.crop : null}
            onChange={(crop) => update({ crop })}
            pixelated={options.pixelated}
            onPickColor={
              pipetteActive
                ? (background) => {
                    update({ background })
                    setPipetteActive(false)
                  }
                : null
            }
          />
          <label className="cape-converter-row">
            <span>{t.skin.converterInside}</span>
            <Dropdown
              value={options.inside}
              onChange={(value) => update({ inside: value as CapeInsideMode })}
              options={[
                { value: 'mirror', label: t.skin.converterInsideMirror },
                { value: 'color', label: t.skin.converterInsideColor }
              ]}
              ariaLabel={t.skin.converterInside}
            />
          </label>
          <label className="checkbox-label">
            <input
              type="checkbox"
              className="toggle-switch"
              checked={options.pixelated}
              onChange={(event) => update({ pixelated: event.target.checked })}
            />
            {t.skin.converterPixelated}
          </label>
          <div>
            <span>{t.skin.converterBackground}</span>
            {pipetteActive && <p className="version-warning">{t.skin.converterPipetteHint}</p>}
            <ColorPicker
              color={options.background}
              onChange={(background) => update({ background })}
              hexRowAccessory={
                <button
                  className={`icon-button${pipetteActive ? ' icon-button--active' : ''}`}
                  title={t.skinEditor.toolEyedropper}
                  aria-label={t.skinEditor.toolEyedropper}
                  aria-pressed={pipetteActive}
                  onClick={() => setPipetteActive((active) => !active)}
                >
                  <EyedropperIcon />
                </button>
              }
            />
          </div>
        </div>
      )}

      {tooLarge && <span className="error">{t.skin.converterTooLarge}</span>}

      <div>
        <button
          className="primary-button"
          disabled={!result || tooLarge}
          onClick={() => result && onApply(result, t.skin.converterDefaultName(fileName))}
        >
          {t.skin.converterApply}
        </button>
        <button className="link-button" onClick={onCancel}>
          {t.common.cancel}
        </button>
      </div>
    </div>
  )
}
