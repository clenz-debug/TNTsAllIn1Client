import { createHash } from 'node:crypto'
import { mkdir, readFile, rename, rm, stat, writeFile } from 'node:fs/promises'
import { dirname } from 'node:path'

export interface DownloadTask {
  url: string
  destination: string
  sha1?: string
}

async function sha1Of(path: string): Promise<string | null> {
  try {
    const data = await readFile(path)
    return createHash('sha1').update(data).digest('hex')
  } catch {
    return null
  }
}

async function alreadyValid(task: DownloadTask): Promise<boolean> {
  if (!task.sha1) {
    // No hash to verify against (e.g. some Fabric meta libraries) — fall back to "file exists
    // and is non-empty" so re-launches don't redownload these every time.
    try {
      return (await stat(task.destination)).size > 0
    } catch {
      return false
    }
  }
  return (await sha1Of(task.destination)) === task.sha1
}

export async function downloadFile(task: DownloadTask): Promise<void> {
  if (await alreadyValid(task)) return

  await mkdir(dirname(task.destination), { recursive: true })
  const response = await fetch(task.url)
  if (!response.ok) {
    throw new Error(`Download failed (${response.status}): ${task.url}`)
  }
  const buffer = Buffer.from(await response.arrayBuffer())

  if (task.sha1) {
    const actual = createHash('sha1').update(buffer).digest('hex')
    if (actual !== task.sha1) {
      throw new Error(`SHA-1 mismatch for ${task.url}: expected ${task.sha1}, got ${actual}`)
    }
  }

  const tmpPath = `${task.destination}.part`
  await writeFile(tmpPath, buffer)
  await rm(task.destination, { force: true })
  await rename(tmpPath, task.destination)
}

export async function downloadAll(
  tasks: DownloadTask[],
  concurrency: number,
  onProgress: (completed: number, total: number, label: string) => void
): Promise<void> {
  if (tasks.length === 0) return

  let completed = 0
  let nextIndex = 0

  async function worker(): Promise<void> {
    while (nextIndex < tasks.length) {
      const current = tasks[nextIndex++]
      await downloadFile(current)
      completed++
      onProgress(completed, tasks.length, current.destination)
    }
  }

  await Promise.all(Array.from({ length: Math.min(concurrency, tasks.length) }, () => worker()))
}
