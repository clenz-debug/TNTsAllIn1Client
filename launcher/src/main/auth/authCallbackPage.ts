import type { Language } from '../../shared/types'

/**
 * Own user report ("die Bestätigungsseite ist noch sehr hässlich") - the plain unstyled HTML the
 * loopback server in `msOAuth.ts` used to serve after the Microsoft login redirect. Styled to match
 * the launcher's own default dark theme (same hex values as `DEFAULT_THEME_COLORS` in
 * `shared/types.ts` - this runs in the system browser, entirely outside the renderer, so it can't
 * read a user's actual custom theme without a lot more plumbing for a page shown for a few
 * seconds). The "N" logo is the same shape as `designs/branding/N-Logo.svg` (the flat, non-live-
 * themed variant already used for contexts that can't react to the user's own theme, e.g. the
 * packaged app icon), inlined here since this HTML has no bundler/asset pipeline to reference an
 * external file through.
 *
 * Also attempts `window.close()` (own follow-up ask: "kann man ... diesen Browser-Tab komplett
 * schließt (von selbst)") - deliberately best-effort, not relied on: modern browsers only allow a
 * script to close a tab it opened itself via `window.open()`, and this tab was opened by the OS
 * (`shell.openExternal`), which every browser tested refuses to let script-close. The visible
 * "you can close this now" text stays regardless, so the page is still correct on browsers where
 * the close attempt is silently ignored.
 *
 * `language` (own follow-up question: "ist die Nachricht immer auf Deutsch?") - this page is
 * generated in the main process for a plain loopback HTTP response, entirely outside the renderer's
 * own `useTranslations()`/`de.ts`/`en.ts` system, so it needs its own tiny inline DE/EN pair instead
 * of reusing that one. The caller (`msOAuth.ts`, threaded from `performLogin`) reads the current
 * setting itself - this function has no way to reach it on its own.
 */
export function renderAuthCallbackPage(ok: boolean, language: Language): string {
  const heading = language === 'en' ? (ok ? 'Login successful' : 'Login failed') : ok ? 'Login erfolgreich' : 'Login fehlgeschlagen'
  const message =
    language === 'en'
      ? ok
        ? 'You can switch back to the launcher now - this window can be closed.'
        : 'Please try again in the launcher. This window can be closed.'
      : ok
        ? 'Du kannst zum Launcher zurückwechseln - dieses Fenster kann geschlossen werden.'
        : 'Bitte versuch es im Launcher erneut. Dieses Fenster kann geschlossen werden.'
  const accentColor = ok ? '#6d6d6d' : '#d84c4c'

  return `<!doctype html>
<html lang="${language}">
<head>
<meta charset="utf-8">
<title>TNT's All-In-1 Client</title>
<style>
  :root { color-scheme: dark; }
  * { box-sizing: border-box; }
  body {
    margin: 0;
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #000000;
    color: #ffffff;
    font-family: 'Segoe UI', system-ui, sans-serif;
  }
  .card {
    background: #1a1a1a;
    border: 1px solid ${accentColor};
    border-radius: 12px;
    padding: 2.5rem 3rem;
    text-align: center;
    max-width: 360px;
  }
  .logo { width: 88px; height: auto; margin-bottom: 1.25rem; }
  h1 { font-size: 1.25rem; margin: 0 0 0.6rem; color: ${ok ? '#ffffff' : accentColor}; }
  p { margin: 0; color: #cccccc; font-size: 0.95rem; line-height: 1.4; }
</style>
</head>
<body>
  <div class="card">
    <svg class="logo" viewBox="0 0 600 660" xmlns="http://www.w3.org/2000/svg">
      <polygon points="200,40 400,40 560,200 560,400 400,560 200,560 40,400 40,200" fill="none" stroke="#ffffff" stroke-width="4"/>
      <g stroke="#ffffff" stroke-width="3" stroke-linejoin="round">
        <rect x="150" y="150" width="300" height="50" fill="#3d3d3d"/>
        <rect x="185" y="200" width="45" height="240" fill="#4d4d4d"/>
        <rect x="370" y="200" width="45" height="240" fill="#5d5d5d"/>
        <polygon points="185,200 230,200 415,415 415,440 370,440 185,225" fill="#6d6d6d"/>
      </g>
      <text x="300" y="500" text-anchor="middle" font-family="Arial, Helvetica, sans-serif" font-size="72" fill="#ffffff">A I <tspan font-size="78">1</tspan></text>
      <text x="300" y="535" text-anchor="middle" font-family="Arial, Helvetica, sans-serif" font-size="26" letter-spacing="4" fill="#ffffff">CLIENT</text>
    </svg>
    <h1>${heading}</h1>
    <p>${message}</p>
  </div>
  <script>setTimeout(() => { try { window.close() } catch (e) {} }, 600)</script>
</body>
</html>`
}
