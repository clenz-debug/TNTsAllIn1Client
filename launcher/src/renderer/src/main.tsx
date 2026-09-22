import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import { ConsoleWindowView } from './ConsoleWindowView'
import './styles/global.css'

const container = document.getElementById('root')
if (!container) {
  throw new Error('Root element not found.')
}

// `main/consoleWindow.ts` opens its BrowserWindow on this same renderer bundle with a `?console`
// query string rather than a second electron-vite entry point - just a small log viewer.
const isConsoleWindow = new URLSearchParams(window.location.search).has('console')

createRoot(container).render(
  <StrictMode>{isConsoleWindow ? <ConsoleWindowView /> : <App />}</StrictMode>
)
