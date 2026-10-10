import { deleteCape } from './capes.js'
import { banCapes, capeBans, findPlayer, unbanCapes } from './friends.js'

/**
 * Cape moderation by hand, run on the server next to the service (it shares the service's database,
 * no restart needed):
 *
 *   node dist/admin.js cape-remove <name or uuid>           - delete that player's active cape
 *   node dist/admin.js cape-ban <name or uuid> <reason...>  - delete it and refuse further uploads
 *   node dist/admin.js cape-unban <name or uuid>
 *   node dist/admin.js cape-bans                            - list the banned accounts
 *
 * A player is found by name only if they have used the client; a UUID (with or without dashes)
 * always works.
 */

function resolve(nameOrUuid: string | undefined): { uuid: string; label: string } {
  if (!nameOrUuid) throw new Error('missing player name or UUID')
  const player = findPlayer(nameOrUuid)
  if (player) return { uuid: player.uuid, label: `${player.name} (${player.uuid})` }
  const hex = nameOrUuid.replace(/-/g, '').toLowerCase()
  if (/^[0-9a-f]{32}$/.test(hex)) return { uuid: hex, label: hex }
  throw new Error(`no player called "${nameOrUuid}" has used the client - pass the UUID instead`)
}

async function main(): Promise<void> {
  const [command, target, ...rest] = process.argv.slice(2)
  switch (command) {
    case 'cape-remove': {
      const player = resolve(target)
      await deleteCape(player.uuid)
      console.log(`Removed the cape of ${player.label}.`)
      return
    }
    case 'cape-ban': {
      const player = resolve(target)
      const reason = rest.join(' ').trim()
      if (!reason) throw new Error('a ban needs a reason')
      banCapes(player.uuid, reason)
      await deleteCape(player.uuid)
      console.log(`Removed the cape of ${player.label} and banned the account from uploading: ${reason}`)
      return
    }
    case 'cape-unban': {
      const player = resolve(target)
      unbanCapes(player.uuid)
      console.log(`${player.label} may upload capes again.`)
      return
    }
    case 'cape-bans': {
      const bans = capeBans()
      if (bans.length === 0) console.log('No account is banned.')
      for (const ban of bans) {
        console.log(`${new Date(ban.created).toISOString().slice(0, 10)}  ${ban.uuid}  ${ban.name ?? '-'}  ${ban.reason}`)
      }
      return
    }
    default:
      throw new Error('usage: admin.js cape-remove|cape-ban|cape-unban|cape-bans [name or uuid] [reason]')
  }
}

main().catch((error: unknown) => {
  console.error(error instanceof Error ? error.message : error)
  process.exitCode = 1
})
