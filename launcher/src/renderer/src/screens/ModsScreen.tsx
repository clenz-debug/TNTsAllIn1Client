import { useEffect, useState } from 'react'
import { Dropdown } from '../Dropdown'
import { formatError } from '../formatError'
import { useTranslations } from '../i18n/LanguageContext'
import {
  isBundleCompatibleVersion,
  MODRINTH_SEARCH_PAGE_SIZE,
  type CustomModEntry,
  type ModrinthSearchResult,
  type ModrinthSortIndex
} from '../../../shared/types'

// Debounce for search-as-you-type (mirrors Modrinth's own browse view, which updates the list as
// you type instead of requiring Enter/a button click) - short enough to feel live, long enough to
// not fire a request per keystroke.
const SEARCH_DEBOUNCE_MS = 350

/** Page numbers to render around `current`, always including page 1 and `total`, with 'ellipsis'
 * markers for the gaps - same "1 2 … 262" shape as Modrinth's own pagination. */
function buildPageNumbers(current: number, total: number): (number | 'ellipsis')[] {
  const keep = new Set<number>([1, total, current - 1, current, current + 1])
  const sorted = [...keep].filter((p) => p >= 1 && p <= total).sort((a, b) => a - b)
  const result: (number | 'ellipsis')[] = []
  let previous = 0
  for (const page of sorted) {
    if (previous && page - previous > 1) result.push('ellipsis')
    result.push(page)
    previous = page
  }
  return result
}

interface Props {
  instanceId: string
  versionId: string
  enabledBundledMods: string[]
  /** Which Minecraft versions currently have bundle content available (dynamic, manifest-driven -
   * see `bundleCompat.ts`), replacing the old single hardcoded `MINECRAFT_VERSION` check. */
  bundleCompatibleVersions: string[]
  onToggleBundledMod: (fileName: string, enabled: boolean) => void
  onClose: () => void
}

export function ModsScreen({ instanceId, versionId, enabledBundledMods, bundleCompatibleVersions, onToggleBundledMod, onClose }: Props) {
  const t = useTranslations()
  // Built inside the component (not at module scope) so its labels can come from `t` - the sort
  // order values themselves (`ModrinthSortIndex`) still match Modrinth's own `index=` API values.
  const sortOptions: { value: ModrinthSortIndex; label: string }[] = [
    { value: 'relevance', label: t.mods.sortOptions.relevance },
    { value: 'downloads', label: t.mods.sortOptions.downloads },
    { value: 'follows', label: t.mods.sortOptions.follows },
    { value: 'newest', label: t.mods.sortOptions.newest },
    { value: 'updated', label: t.mods.sortOptions.updated }
  ]

  const [bundledMods, setBundledMods] = useState<string[]>([])
  const [customMods, setCustomMods] = useState<CustomModEntry[]>([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [searchQuery, setSearchQuery] = useState('')
  const [sortIndex, setSortIndex] = useState<ModrinthSortIndex>('relevance')
  const [searchResults, setSearchResults] = useState<ModrinthSearchResult[]>([])
  const [totalHits, setTotalHits] = useState(0)
  const [currentPage, setCurrentPage] = useState(1)
  const [searching, setSearching] = useState(false)
  const [installingId, setInstallingId] = useState<string | null>(null)
  // Modrinth project ids currently sitting in this instance's `game/mods` (resolved from the real
  // files via `listCustomModProjectIds`, same "hash the jars, ask Modrinth" approach already used
  // for `bundledProjectIds` below) - own user request: the "Installieren" button otherwise reverted
  // to its normal state right after a successful install, looking as if nothing had happened.
  // Re-read after every install/remove rather than tracked as "added this session" so a mod
  // installed via search and then removed again through "Eigene Mods" correctly goes back to
  // showing "Installieren" instead of staying stuck on "Installiert".
  const [installedProjectIds, setInstalledProjectIds] = useState<Set<string>>(new Set())

  async function refreshInstalledProjectIds(): Promise<void> {
    try {
      setInstalledProjectIds(new Set(await window.api.listCustomModProjectIds(instanceId)))
    } catch (err) {
      setError(formatError(err, t))
    }
  }
  // Which search results are mods we already bundle for this version (toggleable or always-on
  // alike - Sodium/Fabric API/etc. never show up in `bundledMods` above since that's toggleable-only,
  // but they're still already running) - own user request: without this, searching for e.g. "Sodium"
  // showed a plain "Installieren" button with no indication it's already there, which would just
  // download a redundant second copy as a custom mod if clicked.
  const [bundledProjectIds, setBundledProjectIds] = useState<Set<string>>(new Set())

  useEffect(() => {
    window.api.listBundledMods(versionId).then(setBundledMods).catch((err) => setError(formatError(err, t)))
    window.api
      .listBundledModProjectIds(versionId)
      .then((ids) => setBundledProjectIds(new Set(ids)))
      .catch((err) => setError(formatError(err, t)))
  }, [versionId])

  useEffect(() => {
    window.api.listCustomMods(instanceId).then(setCustomMods).catch((err) => setError(formatError(err, t)))
    void refreshInstalledProjectIds()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [instanceId])

  async function handleAdd(): Promise<void> {
    setBusy(true)
    try {
      setCustomMods(await window.api.addCustomMods(instanceId))
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setBusy(false)
    }
  }

  async function handleRemove(fileName: string): Promise<void> {
    setBusy(true)
    try {
      setCustomMods(await window.api.removeCustomMod(instanceId, fileName))
      await refreshInstalledProjectIds()
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setBusy(false)
    }
  }

  async function handleToggle(fileName: string, enabled: boolean): Promise<void> {
    setBusy(true)
    try {
      setCustomMods(await window.api.setCustomModEnabled(instanceId, fileName, enabled))
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setBusy(false)
    }
  }

  async function runSearch(pageNumber: number): Promise<void> {
    if (!isBundleCompatibleVersion(versionId, bundleCompatibleVersions)) return
    setSearching(true)
    setError(null)
    try {
      const offset = (pageNumber - 1) * MODRINTH_SEARCH_PAGE_SIZE
      const page = await window.api.searchModrinthMods(searchQuery.trim(), versionId, offset, sortIndex)
      setSearchResults(page.results)
      setTotalHits(page.totalHits)
      setCurrentPage(pageNumber)
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setSearching(false)
    }
  }

  // Browse by default (empty query = Modrinth's full listing for this game version, same as
  // opening modrinth.com/app's own "Discover content" view) and re-run live as the user types or
  // changes the sort, instead of requiring a manual search - own user request. A query/sort change
  // always jumps back to page 1, since the previous page's offset no longer means anything for a
  // different result set.
  useEffect(() => {
    const handle = setTimeout(() => void runSearch(1), SEARCH_DEBOUNCE_MS)
    return () => clearTimeout(handle)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchQuery, sortIndex, versionId])

  async function handleInstall(projectId: string): Promise<void> {
    setInstallingId(projectId)
    setError(null)
    try {
      setCustomMods(await window.api.installModrinthMod(instanceId, projectId, versionId))
      await refreshInstalledProjectIds()
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setInstallingId(null)
    }
  }

  return (
    <div className="mods-screen">
      <header>
        <strong>{t.mods.title}</strong>
        <button className="link-button" onClick={onClose}>
          {t.common.back}
        </button>
      </header>

      {error && <span className="error">{error}</span>}

      <section className="mods-section">
        <h3>{t.mods.bundledHeading}</h3>
        <p className="version-warning">{t.mods.bundledInfo}</p>
        {!isBundleCompatibleVersion(versionId, bundleCompatibleVersions) && (
          <p className="version-warning">{t.mods.bundleIncompatible(versionId)}</p>
        )}
        <ul className="mods-list">
          {bundledMods.map((fileName) => (
            <li key={fileName} className="mods-row">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  className="toggle-switch"
                  checked={enabledBundledMods.includes(fileName)}
                  onChange={(e) => onToggleBundledMod(fileName, e.target.checked)}
                />
                {fileName}
              </label>
            </li>
          ))}
          {bundledMods.length === 0 && <li className="mods-empty">{t.mods.noToggleable}</li>}
        </ul>
      </section>

      <section className="mods-section">
        <h3>{t.mods.searchHeading}</h3>
        {!isBundleCompatibleVersion(versionId, bundleCompatibleVersions) ? (
          <p className="version-warning">{t.mods.searchUnavailable}</p>
        ) : (
          <>
            <div className="mods-search-row">
              <input
                type="text"
                className="skin-editor-name-input"
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') void runSearch(1)
                }}
                placeholder={t.mods.searchPlaceholder}
              />
              <Dropdown value={sortIndex} onChange={(value) => setSortIndex(value as ModrinthSortIndex)} options={sortOptions} ariaLabel={t.mods.sortAriaLabel} />
              <button className="secondary-button" onClick={() => void runSearch(1)} disabled={searching}>
                {searching ? t.mods.searching : t.mods.search}
              </button>
            </div>
            <ul className="mods-list mods-search-results">
              {searchResults.map((result) => (
                <li key={result.projectId} className="mods-row mods-search-row-item">
                  {result.iconDataUri && <img className="mods-search-icon" src={result.iconDataUri} alt="" />}
                  <div className="mods-search-info">
                    <strong>{result.title}</strong>
                    <span className="mods-search-description">{result.description}</span>
                  </div>
                  <button
                    className="secondary-button"
                    disabled={
                      installingId === result.projectId ||
                      installedProjectIds.has(result.projectId) ||
                      bundledProjectIds.has(result.projectId)
                    }
                    onClick={() => void handleInstall(result.projectId)}
                  >
                    {bundledProjectIds.has(result.projectId)
                      ? t.mods.alreadyBundled
                      : installedProjectIds.has(result.projectId)
                        ? t.mods.installed
                        : installingId === result.projectId
                          ? t.mods.installing
                          : t.mods.install}
                  </button>
                </li>
              ))}
              {searchResults.length === 0 && !searching && <li className="mods-empty">{t.mods.noResults}</li>}
            </ul>
            {totalHits > MODRINTH_SEARCH_PAGE_SIZE &&
              (() => {
                const totalPages = Math.ceil(totalHits / MODRINTH_SEARCH_PAGE_SIZE)
                return (
                  <nav className="mods-pagination" aria-label={t.mods.pagesAriaLabel}>
                    <button
                      className="secondary-button"
                      disabled={searching || currentPage <= 1}
                      onClick={() => void runSearch(currentPage - 1)}
                    >
                      ‹
                    </button>
                    {buildPageNumbers(currentPage, totalPages).map((page, index) =>
                      page === 'ellipsis' ? (
                        <span key={`ellipsis-${index}`} className="mods-pagination-ellipsis">
                          …
                        </span>
                      ) : (
                        <button
                          key={page}
                          className={`secondary-button mods-pagination-page${page === currentPage ? ' mods-pagination-active' : ''}`}
                          disabled={searching || page === currentPage}
                          onClick={() => void runSearch(page)}
                        >
                          {page}
                        </button>
                      )
                    )}
                    <button
                      className="secondary-button"
                      disabled={searching || currentPage >= totalPages}
                      onClick={() => void runSearch(currentPage + 1)}
                    >
                      ›
                    </button>
                  </nav>
                )
              })()}
          </>
        )}
      </section>

      <section className="mods-section">
        <h3>{t.mods.ownHeading}</h3>
        <button className="secondary-button" onClick={() => void handleAdd()} disabled={busy}>
          {t.mods.addMod}
        </button>
        <ul className="mods-list">
          {customMods.map((mod) => (
            <li key={mod.fileName} className="mods-row">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  className="toggle-switch"
                  checked={mod.enabled}
                  disabled={busy}
                  onChange={(e) => void handleToggle(mod.fileName, e.target.checked)}
                />
                {mod.fileName}
              </label>
              <button className="link-button" onClick={() => void handleRemove(mod.fileName)} disabled={busy}>
                {t.common.remove}
              </button>
            </li>
          ))}
          {customMods.length === 0 && <li className="mods-empty">{t.mods.noneAdded}</li>}
        </ul>
      </section>
    </div>
  )
}
