import type { Camera, Object3D } from 'three'
import { Raycaster, Vector2 } from 'three'

export interface PaintHit {
  object: Object3D
  uv: { x: number; y: number }
}

const raycaster = new Raycaster()
const pointer = new Vector2()

/**
 * Finds which of the given (already visibility-filtered - see `bodyParts.ts`) body-part objects
 * sits under a mouse position, and where on its UV-mapped texture the hit landed. Callers must
 * only pass currently *visible* parts: verified directly against the installed `three` source
 * (`src/core/Raycaster.js`/`src/objects/Mesh.js`) that raycasting does **not** skip
 * `object.visible === false` targets on its own - unlike what's commonly assumed, an invisible
 * mesh is still perfectly hittable unless excluded from the candidate list beforehand.
 *
 * `recursive: true` because a `BodyPart`'s `innerLayer`/`outerLayer` (see `bodyParts.ts`) may be a
 * `Group` wrapping the actual mesh rather than a mesh itself (skinview3d's own doc comment on
 * `BodyPart`), so the real hit-testable geometry can be one level deeper than the object we toggle
 * visibility on.
 */
export function hitTest(clientX: number, clientY: number, canvas: HTMLCanvasElement, camera: Camera, targets: Object3D[]): PaintHit | null {
  const rect = canvas.getBoundingClientRect()
  pointer.x = ((clientX - rect.left) / rect.width) * 2 - 1
  pointer.y = -((clientY - rect.top) / rect.height) * 2 + 1

  raycaster.setFromCamera(pointer, camera)
  const intersections = raycaster.intersectObjects(targets, true)
  const hit = intersections.find((intersection) => intersection.uv !== undefined)
  return hit?.uv ? { object: hit.object, uv: { x: hit.uv.x, y: hit.uv.y } } : null
}

/**
 * skinview3d writes UV coordinates per box face already pointing at the correct region of the
 * full skin texture atlas, so a hit's UV maps straight onto a pixel with no separate body-part
 * lookup needed. The V-axis flip (three.js UV space has v=0 at the texture's bottom edge, canvas
 * pixel rows count from the top) is the standard convention but has not been visually confirmed
 * yet against this specific atlas layout - if a painted pixel ever lands mirrored vertically from
 * where it was clicked, flip this to `uv.y * textureHeight`.
 */
export function uvToPixel(uv: { x: number; y: number }, textureWidth: number, textureHeight: number): { x: number; y: number } {
  return {
    x: Math.floor(uv.x * textureWidth),
    y: Math.floor((1 - uv.y) * textureHeight)
  }
}
