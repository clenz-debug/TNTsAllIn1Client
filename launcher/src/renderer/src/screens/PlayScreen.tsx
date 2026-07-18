import { useState } from 'react'
import type { GameLogEvent, LaunchProgressEvent, MinecraftProfile } from '../../../shared/types'

interface Props {
  profile: MinecraftProfile
  onLogout: () => void
}

export function PlayScreen({ profile, onLogout }: Props) {
  const [busy, setBusy] = useState(false)
  const [progress, setProgress] = useState<LaunchProgressEvent | null>(null)
  const [logs, setLogs] = useState<GameLogEvent[]>([])

  async function handlePlay(): Promise<void> {
    setBusy(true)
    setLogs([])
    setProgress(null)
    const unsubscribeProgress = window.api.onLaunchProgress(setProgress)
    const unsubscribeLog = window.api.onGameLog((event) => setLogs((prev) => [...prev.slice(-499), event]))
    try {
      await window.api.play(profile)
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      setLogs((prev) => [...prev, { source: 'launcher', level: 'error', message }])
    } finally {
      unsubscribeProgress()
      unsubscribeLog()
      setBusy(false)
    }
  }

  return (
    <div className="play-screen">
      <header>
        <div>
          <strong>{profile.name}</strong>
          {profile.isMock && <span className="mock-badge">Dev-Mock-Profil</span>}
        </div>
        <button className="link-button" onClick={onLogout}>
          Abmelden
        </button>
      </header>

      <button className="primary-button play-button" onClick={() => void handlePlay()} disabled={busy}>
        {busy ? 'Läuft…' : 'Play'}
      </button>

      {progress && (
        <div className="progress">
          <span>
            {progress.stage}
            {progress.label ? ` — ${progress.label}` : ''} ({progress.completed}/{progress.total})
          </span>
          <progress value={progress.completed} max={Math.max(progress.total, 1)} />
        </div>
      )}

      <pre className="log-panel">
        {logs.map((log, index) => (
          <div key={index} className={log.level === 'error' ? 'log-error' : 'log-info'}>
            {log.message}
          </div>
        ))}
      </pre>
    </div>
  )
}
