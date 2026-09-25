import { useEffect, useState } from 'react'
import { ConfirmDialog } from '../ConfirmDialog'
import { Dropdown } from '../Dropdown'
import { formatError } from '../formatError'
import { useTranslations } from '../i18n/LanguageContext'
import { PlayerHeadIcon } from '../PlayerHeadIcon'
import type { FriendActivity, FriendEntry, FriendsState, FriendsStatus, WorldInvite } from '../../../shared/types'

/** PlayScreen's `handleJoin` - resolves to an error text, or null once the join is on its way. */
type JoinHandler = (address: string, invite: WorldInvite | null) => Promise<string | null>

interface Props {
  state: FriendsState
  /** The game is running - "join" then hands over to the mod instead of starting the game. */
  gameRunning: boolean
  onJoin: JoinHandler
  onClose: () => void
}

/**
 * One world invitation (Phase 8b) with join/decline - shown on the play screen above everything
 * else and in the Friends screen, so it's seen wherever the player is.
 */
export function InviteBanner({ invite, onJoin }: { invite: WorldInvite; onJoin: JoinHandler }) {
  const t = useTranslations()
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function join(): Promise<void> {
    setBusy(true)
    setError(await onJoin(invite.address, invite))
    setBusy(false)
  }

  async function decline(): Promise<void> {
    setBusy(true)
    try {
      await window.api.dismissInvite(invite.from.uuid)
    } catch (err) {
      setError(formatError(err, t))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="update-banner">
      <span>
        {t.friends.inviteText(invite.from.name, invite.version)}
        {error && <span className="error"> {error}</span>}
      </span>
      <div className="header-actions">
        <button className="link-button" disabled={busy} onClick={() => void join()}>
          {t.friends.join}
        </button>
        <button className="link-button" disabled={busy} onClick={() => void decline()}>
          {t.friends.decline}
        </button>
      </div>
    </div>
  )
}

const STATUS_OPTIONS: FriendsStatus[] = ['online', 'away', 'dnd', 'invisible']

/**
 * Friends (Phase 8, own user request): own status (Discord-style online/away/do-not-disturb/
 * invisible) and "hide server", adding friends by Minecraft name, open requests both ways, and the
 * friends list with live presence. All data comes from `main/friends/friendsService.ts`, which
 * refreshes it every ~20 s and pushes it here through PlayScreen's `state`.
 */
export function FriendsScreen({ state, gameRunning, onJoin, onClose }: Props) {
  const t = useTranslations()
  const [name, setName] = useState('')
  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [status, setStatus] = useState<string | null>(null)
  const [pendingRemoval, setPendingRemoval] = useState<FriendEntry | null>(null)
  const [skins, setSkins] = useState<Record<string, string | null>>({})

  const overview = state.overview
  const friends = overview?.friends ?? []

  // Head icons, fetched once per player (main caches them too).
  useEffect(() => {
    const players = [...friends, ...(overview?.incoming ?? []), ...(overview?.outgoing ?? [])]
    const missing = players.filter((player) => !(player.uuid in skins))
    if (missing.length === 0) return
    setSkins((prev) => ({ ...prev, ...Object.fromEntries(missing.map((player) => [player.uuid, null])) }))
    for (const player of missing) {
      void window.api.getFriendSkin(player.uuid).then((skin) => setSkins((prev) => ({ ...prev, [player.uuid]: skin })))
    }
  }, [overview])

  async function run<T>(work: () => Promise<T>, success?: (result: T) => string | null): Promise<void> {
    setBusy(true)
    setActionError(null)
    setStatus(null)
    try {
      const result = await work()
      setStatus(success?.(result) ?? null)
    } catch (err) {
      setActionError(formatError(err, t))
    } finally {
      setBusy(false)
    }
  }

  function handleAdd(): void {
    const target = name.trim()
    if (!target) return
    void run(
      async () => {
        const next = await window.api.sendFriendRequest(target)
        setName('')
        return next
      },
      // If they had already asked us, the backend made us friends right away.
      (next) =>
        next.overview?.friends.some((friend) => friend.name.toLowerCase() === target.toLowerCase())
          ? t.friends.nowFriends(target)
          : t.friends.requestSent(target)
    )
  }

  function activityText(activity: FriendActivity | null): string | null {
    if (!activity) return null
    if (activity.kind === 'multiplayer') return activity.server ? t.friends.activity.server(activity.server) : t.friends.activity.multiplayer
    return t.friends.activity[activity.kind]
  }

  return (
    <div className="worlds-screen friends-screen">
      <header>
        <strong>{t.friends.title}</strong>
        <button className="link-button" onClick={onClose}>
          {t.common.back}
        </button>
      </header>

      {(overview?.invites ?? []).map((invite) => (
        <InviteBanner key={invite.from.uuid} invite={invite} onJoin={onJoin} />
      ))}

      {state.error && <span className="error">{t.errors.friends[state.error as keyof typeof t.errors.friends] ?? t.errors.friends.unknown}</span>}
      {actionError && <span className="error">{actionError}</span>}
      {status && <span className="status">{status}</span>}

      <section className="mods-section">
        <h3>{t.friends.ownHeading}</h3>
        <div className="friends-prefs">
          <label className="checkbox-label">
            {t.friends.statusLabel}
            <Dropdown
              value={state.prefs.status}
              onChange={(value) => void window.api.setFriendsPrefs({ ...state.prefs, status: value as FriendsStatus })}
              options={STATUS_OPTIONS.map((option) => ({ value: option, label: t.friends.ownStatus[option] }))}
            />
          </label>
          <label className="checkbox-label">
            <input
              type="checkbox"
              className="toggle-switch"
              checked={state.prefs.hideServer}
              onChange={(e) => void window.api.setFriendsPrefs({ ...state.prefs, hideServer: e.target.checked })}
            />
            {t.friends.hideServer}
          </label>
        </div>
      </section>

      <section className="mods-section">
        <h3>{t.friends.addHeading}</h3>
        <p className="version-warning">{t.friends.addInfo}</p>
        <div className="mods-search-row">
          <input
            className="skin-editor-name-input"
            value={name}
            maxLength={16}
            placeholder={t.friends.addPlaceholder}
            onChange={(e) => setName(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleAdd()
            }}
          />
          <button className="secondary-button" onClick={handleAdd} disabled={busy || !name.trim()}>
            {t.friends.addButton}
          </button>
        </div>
      </section>

      {overview && (overview.incoming.length > 0 || overview.outgoing.length > 0) && (
        <section className="mods-section">
          <h3>{t.friends.requestsHeading}</h3>
          <ul className="mods-list">
            {overview.incoming.map((player) => (
              <li key={player.uuid} className="world-row">
                <PlayerHeadIcon textureDataUri={skins[player.uuid] ?? null} size={32} />
                <div className="world-info">
                  <strong>{player.name}</strong>
                  <span className="friend-activity">{t.friends.incoming}</span>
                </div>
                <div className="header-actions">
                  <button
                    className="link-button"
                    disabled={busy}
                    onClick={() => void run(() => window.api.acceptFriendRequest(player.uuid), () => t.friends.nowFriends(player.name))}
                  >
                    {t.friends.accept}
                  </button>
                  <button className="link-button" disabled={busy} onClick={() => void run(() => window.api.removeFriendRequest(player.uuid))}>
                    {t.friends.decline}
                  </button>
                </div>
              </li>
            ))}
            {overview.outgoing.map((player) => (
              <li key={player.uuid} className="world-row">
                <PlayerHeadIcon textureDataUri={skins[player.uuid] ?? null} size={32} />
                <div className="world-info">
                  <strong>{player.name}</strong>
                  <span className="friend-activity">{t.friends.outgoing}</span>
                </div>
                <div className="header-actions">
                  <button className="link-button" disabled={busy} onClick={() => void run(() => window.api.removeFriendRequest(player.uuid))}>
                    {t.friends.withdraw}
                  </button>
                </div>
              </li>
            ))}
          </ul>
        </section>
      )}

      <section className="mods-section">
        <h3>{t.friends.friendsHeading(friends.length)}</h3>
        <ul className="mods-list">
          {!overview && !state.error && <li className="mods-empty">{t.common.loading}</li>}
          {overview && friends.length === 0 && <li className="mods-empty">{t.friends.empty}</li>}
          {friends.map((friend) => {
            const activity = activityText(friend.activity)
            return (
              <li key={friend.uuid} className={`world-row${friend.status === 'offline' ? ' friend-offline' : ''}`}>
                <PlayerHeadIcon textureDataUri={skins[friend.uuid] ?? null} size={32} />
                <div className="world-info">
                  <strong>{friend.name}</strong>
                  <span className="friend-activity">
                    <span className={`friend-status-dot friend-status-${friend.status}`} aria-hidden="true" />
                    {t.friends.visibleStatus[friend.status]}
                    {activity && ` · ${activity}`}
                  </span>
                </div>
                <div className="header-actions">
                  {friend.activity?.kind === 'multiplayer' && friend.activity.server && (
                    <button
                      className="link-button"
                      disabled={busy}
                      title={gameRunning ? t.friends.joinInGameHint : undefined}
                      onClick={() => {
                        const server = friend.activity?.server
                        if (server) void run(() => onJoin(server, null).then((message) => (message ? Promise.reject(new Error(message)) : null)))
                      }}
                    >
                      {t.friends.join}
                    </button>
                  )}
                  <button className="link-button" disabled={busy} onClick={() => setPendingRemoval(friend)}>
                    {t.common.remove}
                  </button>
                </div>
              </li>
            )
          })}
        </ul>
      </section>

      {pendingRemoval && (
        <ConfirmDialog
          message={t.friends.confirmRemove(pendingRemoval.name)}
          confirmLabel={t.common.remove}
          cancelLabel={t.common.cancel}
          busy={busy}
          onConfirm={() =>
            void run(async () => {
              await window.api.removeFriend(pendingRemoval.uuid)
              setPendingRemoval(null)
            })
          }
          onCancel={() => setPendingRemoval(null)}
        />
      )}
    </div>
  )
}
