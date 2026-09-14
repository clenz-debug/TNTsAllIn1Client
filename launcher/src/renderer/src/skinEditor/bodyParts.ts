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

/** The 12 visibility toggles (6 base body parts + their 6 overlay/"layer 2" counterparts, see
 * `SkinObject`/`BodyPart` in skinview3d's `libs/model.d.ts`) - data-driven instead of writing 12
 * near-identical checkbox blocks by hand. Only *visibility* is toggled here; the base layer is the
 * only one actually paintable in this version (see the plan's v1/v2 tool split). */
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

/** Only the base layers are paintable (overlay *painting* is a deferred v2 feature, see the plan) -
 * the raycast target list is built from these, filtered further by whichever are currently
 * visible (see `raycastPaint.ts`'s doc comment on why hidden parts must be excluded explicitly). */
export const PAINTABLE_PART_IDS = new Set(BASE_PARTS.map((part) => `base-${part.id}`))
