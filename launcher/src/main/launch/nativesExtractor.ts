import { createWriteStream } from 'node:fs'
import { mkdir, rm } from 'node:fs/promises'
import { dirname, join, normalize, sep } from 'node:path'
import { pipeline } from 'node:stream/promises'
import { openPromise } from 'yauzl'

export interface NativesJar {
  path: string
  /** Entry-name prefixes to skip, from the version JSON's `extract.exclude` (usually `META-INF/`). */
  exclude: string[]
}

/**
 * Versions up to 1.18.2 (LWJGL 3.2 and older) ship their native libraries (lwjgl.dll, OpenAL, ...)
 * as separate classifier jars the launcher has to unpack itself - LWJGL only loads them from
 * `-Djava.library.path`, it doesn't extract them from the classpath like 1.19+ does. Without this
 * step those versions crash on start with "[LWJGL] Failed to load a library".
 *
 * Re-extracted on every launch into a freshly emptied directory, same as Mojang's own launcher -
 * a few MB, and it can never end up mixing natives from a previous version of the instance.
 */
export async function extractNatives(jars: NativesJar[], targetDir: string): Promise<void> {
  await rm(targetDir, { recursive: true, force: true })
  await mkdir(targetDir, { recursive: true })

  for (const jar of jars) {
    const zipfile = await openPromise(jar.path, { lazyEntries: true, autoClose: true })
    try {
      for await (const entry of zipfile.eachEntry()) {
        if (entry.fileName.endsWith('/')) continue
        if (jar.exclude.some((prefix) => entry.fileName.startsWith(prefix))) continue
        const destination = normalize(join(targetDir, entry.fileName))
        if (!destination.startsWith(normalize(targetDir) + sep)) continue
        await mkdir(dirname(destination), { recursive: true })
        await pipeline(await zipfile.openReadStreamPromise(entry), createWriteStream(destination))
      }
    } finally {
      zipfile.close()
    }
  }
}
