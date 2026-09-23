import { useEffect, useRef } from 'react'

interface Props {
  /** Already-fetched `data:` URI (see `window.api.fetchSkinTexture`) - this component only draws,
   * it never touches the network itself, same "renderer never talks to the network directly" split
   * every other texture consumer in this app follows. `null` while it's still loading or there's no
   * active skin at all - renders a blank icon rather than erroring. */
  textureDataUri: string | null
  size?: number
}

const HEAD_LAYER_SIZE = 8
const HEAD_LAYER_X = 8
const HEAD_LAYER_Y = 8
const OVERLAY_LAYER_X = 40
const OVERLAY_LAYER_Y = 8
/** The second skin layer (hat/hair/glasses/...) only exists in the modern 64x64 skin format -
 * legacy 64x32 skins have nothing below y=32 at all, so drawing from y=8 there would just grab
 * unrelated leg/arm pixels instead of a real overlay. */
const MIN_HEIGHT_FOR_OVERLAY = 64

/**
 * Own user request ("der Button mit dem Namen sollte zusätzlich die Vorderseite vom Kopf des
 * aktuellen mc skins (mit den 3D-Effekten) zeigen") - the front head layer plus its overlay ("hat")
 * layer composited on top, the same two-layer look most Minecraft avatar generators (Crafatar,
 * NameMC, ...) render as a "3D head", as opposed to a flat single-layer crop that'd be missing any
 * hair/hat/glasses detail baked into the overlay. A plain `<canvas>` rather than `skinview3d`'s full
 * WebGL model (already a dependency, used by the skin editor's live preview) - own follow-up
 * decision after weighing it against a real WebGL render: this icon sits permanently in the header,
 * and running a whole persistent WebGL context just for a ~24px static icon would be wasteful
 * compared to two cheap `drawImage` calls.
 */
export function PlayerHeadIcon({ textureDataUri, size = 24 }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null)

  useEffect(() => {
    const canvas = canvasRef.current
    const ctx = canvas?.getContext('2d')
    if (!canvas || !ctx) return
    ctx.clearRect(0, 0, size, size)
    if (!textureDataUri) return

    const image = new Image()
    image.onload = () => {
      // Nearest-neighbor, not smoothed - scaling a blocky 8x8 skin crop up to icon size with the
      // canvas default (bilinear) filtering would blur it into mush instead of keeping crisp pixels.
      ctx.imageSmoothingEnabled = false
      ctx.clearRect(0, 0, size, size)
      ctx.drawImage(image, HEAD_LAYER_X, HEAD_LAYER_Y, HEAD_LAYER_SIZE, HEAD_LAYER_SIZE, 0, 0, size, size)
      if (image.naturalHeight >= MIN_HEIGHT_FOR_OVERLAY) {
        ctx.drawImage(image, OVERLAY_LAYER_X, OVERLAY_LAYER_Y, HEAD_LAYER_SIZE, HEAD_LAYER_SIZE, 0, 0, size, size)
      }
    }
    image.src = textureDataUri
  }, [textureDataUri, size])

  return <canvas ref={canvasRef} width={size} height={size} className="player-head-icon" aria-hidden="true" />
}
