import { readdir } from 'node:fs/promises'
import { join } from 'node:path'
import type { Instance } from '../../shared/types'
import { dataRoot } from '../dataRoot'
import { moveTree } from '../storageMove'
import { instanceDir } from './installer'

/** Exact 1:1 relative-path correspondence with the new shared layout - this is literally how the
 * pre-refactor code already laid these three out (`<instanceDir>/versions/<id>/<id>.jar`,
 * `<instanceDir>/libraries/<mavenPath>`, `<instanceDir>/assets/{indexes,objects}/...`), so a
 * generic "move everything, skip if already present" (`moveTree`) handles all three without any
 * special-casing. */
const LEGACY_SUBFOLDERS = ['versions', 'libraries', 'assets'] as const

async function consolidateInstance(instanceId: string): Promise<void> {
  const dir = instanceDir(instanceId)
  for (const subfolder of LEGACY_SUBFOLDERS) {
    const legacyDir = join(dir, subfolder)
    const exists = await readdir(legacyDir)
      .then(() => true)
      .catch(() => false)
    if (!exists) continue
    await moveTree(legacyDir, join(dataRoot(), subfolder), 8)
  }
}

/**
 * One-time cleanup for installs made before shared storage existed: each instance used to keep its
 * own full copy of `versions/`/`libraries/`/`assets/` (see `installer.ts`/`classpath.ts`'s own
 * history) - this moves whatever's still sitting in those per-instance folders into the new shared
 * `dataRoot()` locations (skipping any file another instance already "brought" there first) and
 * deletes the now-empty per-instance copies, reclaiming the duplicated space with **no re-download**.
 *
 * Only ever looks at real, known instances from `settings.instances` (never a blind directory
 * scan) - same "only touch what's actually a real instance" caution `launcherSettings.ts`'s own
 * `migrateLegacyInstances` already applies for the pre-instance-system migration.
 *
 * Idempotent and safe to interrupt by construction, no marker file needed (unlike
 * `javaRuntime.ts`'s `.version` marker): every individual file move is itself
 * skip-if-destination-already-valid (`storageMove.ts#moveOrSkip`), and the only "completion" state
 * is the filesystem itself - a run killed mid-way leaves some files already relocated and the rest
 * untouched, and the next startup's identical scan simply finishes where it left off.
 *
 * Called from `main/index.ts` right after `dataRoot.ts#initDataRoot()` - needs `dataRoot()` already
 * resolved, so it can't live inside `launcherSettings.ts#loadLauncherSettings` itself (that would
 * make `initDataRoot()` depend on its own result).
 */
export async function consolidateInstanceStorage(instances: Instance[]): Promise<void> {
  for (const instance of instances) await consolidateInstance(instance.id)
}
