import type { Object3D } from 'three'
import type { SkinObject } from 'skinview3d'

type SkinPartKey = 'head' | 'body' | 'rightArm' | 'leftArm' | 'rightLeg' | 'leftLeg'

const BASE_PARTS: Array<{ id: string; label: string; key: SkinPartKey }> = [
  { id: 'head', label: 'Kopf', key: 'head' },
  { id: 'body', label: 'Körper', key: 'body' },
  { id: 'right-arm', label: 'Rechter Arm', key: 'rightArm' },
  { id: 'left-arm', label: 'Linker Arm', key: 'leftArm' },
  { id: 'right-leg', label: 'Rechtes Bein', key: 'rightLeg' },
  { id: 'left-leg', label: 'Linkes Bein', key: 'leftLeg' }
]

export interface BodyPartToggle {
  id: string
  label: string
  getObject: (skin: SkinObject) => Object3D
}

/**
 * The 12 visibility toggles (6 base body parts + their 6 overlay/"layer 2" counterparts, see
 * `SkinObject`/`BodyPart` in skinview3d's `libs/model.d.ts`) - data-driven instead of writing 12
 * near-identical checkbox blocks by hand. Both layers are paintable, not just the base one - own
 * user feedback after live-testing an earlier version that only ever raycast against the base
 * meshes: clicking on a visible overlay part (e.g. a hat) painted straight through it onto the
 * head underneath, since the overlay mesh was never a valid raycast target at all. The fix
 * (`SkinEditorScreen.tsx#paintableTargets`) is simply to include every *currently visible* toggle,
 * base or overlay, as a raycast target - three.js's `Raycaster.intersectObjects` already returns
 * hits sorted nearest-first, and the overlay geometry is a strictly larger box wrapping the base
 * one, so whichever layer is actually visible in front at a given screen point is naturally the
 * one that gets hit and painted, with no extra "which layer is topmost" logic needed.
 */
export const BODY_PART_TOGGLES: BodyPartToggle[] = [
  ...BASE_PARTS.map((part) => ({
    id: `base-${part.id}`,
    label: `${part.label} (Basis)`,
    getObject: (skin: SkinObject) => skin[part.key].innerLayer
  })),
  ...BASE_PARTS.map((part) => ({
    id: `overlay-${part.id}`,
    label: `${part.label} (Overlay)`,
    getObject: (skin: SkinObject) => skin[part.key].outerLayer
  }))
]
