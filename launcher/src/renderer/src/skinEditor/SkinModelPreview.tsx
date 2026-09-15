import { useEffect, useRef } from 'react'
import { SkinViewer } from 'skinview3d'
import type { SkinVariant } from '../../../shared/types'

interface Props {
  skinDataUri: string
  variant: SkinVariant
  capeDataUri?: string | null
  showCape: boolean
  width: number
  height: number
}

/**
 * A read-only, mouse-drag-rotatable 3D preview of one skin (+ optional cape) - reused for both
 * the "currently worn" preview and every entry in the paginated skin library grid in
 * `SkinScreen.tsx`. Unlike `SkinEditorScreen.tsx`, this never paints, so skinview3d's own default
 * `OrbitControls` (left-drag to rotate in whichever direction you drag, wheel to zoom) can be used
 * entirely as-is - no custom pointer handling needed here at all.
 */
export function SkinModelPreview({ skinDataUri, variant, capeDataUri, showCape, width, height }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const viewerRef = useRef<SkinViewer | null>(null)

  useEffect(() => {
    if (!canvasRef.current) return
    const viewer = new SkinViewer({
      canvas: canvasRef.current,
      width,
      height,
      skin: skinDataUri,
      model: variant === 'slim' ? 'slim' : 'default'
    })
    viewerRef.current = viewer
    // A fresh viewer starts cape-less - without this, switching skins (which recreates the viewer)
    // made the cape silently disappear until `showCape`/`capeDataUri` themselves happened to
    // change, since that's a separate effect keyed on different deps that wouldn't rerun here.
    if (capeDataUri && showCape) {
      void viewer.loadCape(capeDataUri, { backEquipment: 'cape' })
    }
    return () => {
      viewer.dispose()
      viewerRef.current = null
    }
    // Only re-created if the underlying skin/variant identity actually changes - each call site
    // (one per library entry, plus the "currently worn" one) has a stable skin for its lifetime.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [skinDataUri, variant])

  useEffect(() => {
    const viewer = viewerRef.current
    if (!viewer) return
    if (capeDataUri && showCape) {
      void viewer.loadCape(capeDataUri, { backEquipment: 'cape' })
    } else {
      viewer.resetCape()
    }
  }, [capeDataUri, showCape])

  return <canvas ref={canvasRef} className="skin-model-canvas" width={width} height={height} />
}
