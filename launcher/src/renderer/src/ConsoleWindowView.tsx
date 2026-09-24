import { useEffect, useState } from 'react'
import { LanguageProvider, useTranslations } from './i18n/LanguageContext'
import { applyThemeColors } from './theme'
import type { GameLogEvent, LaunchProgressEvent, Language } from '../../shared/types'

/**
 * Rendered instead of `App` in the second `BrowserWindow` `main/consoleWindow.ts` opens when the
 * Settings screen's "Konsole in separatem Fenster anzeigen" toggle is on (own wishlist item) -
 * `main.tsx` picks this over `App` purely based on the `?console` query string in that window's
 * URL. Subscribes to the exact same `game:log`/`launch:progress` broadcasts `PlayScreen`'s inline
 * log panel does; `ipc/handlers.ts#LaunchPlay` sends both windows the same events.
 *
 * This is its own separate renderer entry point, never a descendant of `App` - so it needs its
 * own `LanguageProvider`, loading the current language (and theme colors) itself rather than receiving them as props.
 */
export function ConsoleWindowView() {
  const [logs, setLogs] = useState<GameLogEvent[]>([])
  const [progress, setProgress] = useState<LaunchProgressEvent | null>(null)
  const [language, setLanguage] = useState<Language>('de')
  // Mirrors `PlayScreen`'s own `busy` state, which this window has no direct way to know otherwise -
  // it never itself calls `window.api.play(...)`, so it can only learn a launch started/ended from
  // `handlers.ts#LaunchPlay`'s own `LaunchBusyChanged` broadcast (see that handler's `broadcastLaunchBusy`).
  // Gates this window's own Cancel button (own user request: shown here instead of in the main
  // window whenever the Settings screen's "Konsole in separatem Fenster" toggle is on).
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const unsubscribeLog = window.api.onGameLog((event) => setLogs((prev) => [...prev.slice(-499), event]))
    const unsubscribeProgress = window.api.onLaunchProgress(setProgress)
    const unsubscribeBusy = window.api.onLaunchBusyChanged(setBusy)
    // `PlayScreen#handlePlay` opens this window fire-and-forget, right alongside its own
    // `window.api.play(...)` call - `LaunchPlay`'s very first `LaunchBusyChanged: true` can easily
    // fire before this window's own React tree (and therefore the subscription just above) exists
    // yet, and a `webContents.send` to a not-yet-listening page is simply lost, never queued. Asking
    // once here closes that race regardless of how the two ended up ordered.
    window.api
      .isLaunchBusy()
      .then(setBusy)
      .catch(() => undefined)
    return () => {
      unsubscribeLog()
      unsubscribeProgress()
      unsubscribeBusy()
    }
  }, [])

  // Same startup read as `App`'s: this window has its own document, so the user's theme colors
  // have to be applied here too - otherwise it stays on the default CSS colors.
  useEffect(() => {
    window.api
      .loadSettings()
      .then((settings) => {
        applyThemeColors(settings.themeColors)
        setLanguage(settings.language)
      })
      .catch(() => undefined)
  }, [])

  return (
    <LanguageProvider language={language}>
      <ConsoleWindowContent logs={logs} progress={progress} busy={busy} />
    </LanguageProvider>
  )
}

interface ContentProps {
  logs: GameLogEvent[]
  progress: LaunchProgressEvent | null
  busy: boolean
}

function ConsoleWindowContent({ logs, progress, busy }: ContentProps) {
  const t = useTranslations()
  return (
    <div className="play-screen">
      <header>
        <strong>{t.console.title}</strong>
        {busy && (
          <button className="secondary-button" onClick={() => void window.api.cancelLaunch()}>
            {t.play.cancel}
          </button>
        )}
      </header>

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
