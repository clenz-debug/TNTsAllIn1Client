import { createHash } from 'node:crypto'
import { mkdir, readFile, rename, rm, stat, writeFile } from 'node:fs/promises'
import { dirname } from 'node:path'
import { localizedError } from '../../shared/errorMessages'

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

export async function downloadFile(task: DownloadTask, signal?: AbortSignal): Promise<void> {
  if (await alreadyValid(task)) return

  await mkdir(dirname(task.destination), { recursive: true })
  const response = await fetch(task.url, { signal })
  if (!response.ok) {
    throw localizedError('download.failed', { status: response.status, url: task.url })
  }
  const buffer = Buffer.from(await response.arrayBuffer())

  if (task.sha1) {
    const actual = createHash('sha1').update(buffer).digest('hex')
    if (actual !== task.sha1) {
      throw localizedError('download.sha1Mismatch', { label: task.url, expected: task.sha1, actual })
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
  onProgress: (completed: number, total: number, label: string) => void,
  signal?: AbortSignal
): Promise<void> {
  if (tasks.length === 0) return

  let completed = 0
  let nextIndex = 0

  async function worker(): Promise<void> {
    while (nextIndex < tasks.length) {
      const current = tasks[nextIndex++]
      await downloadFile(current, signal)
      completed++
      onProgress(completed, tasks.length, current.destination)
    }
  }

  await Promise.all(Array.from({ length: Math.min(concurrency, tasks.length) }, () => worker()))
}
