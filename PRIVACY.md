# Privacy

This page lists what TNT's All-In-1 Client (the launcher and the in-game mod) sends over the
network, to whom, and why. There is no analytics, no advertising, no crash reporting and no usage
statistics.

You can remove your data from the project's server yourself: in the launcher, open "Friends" and
choose "Delete my data from the server". Questions:
[open an issue](https://github.com/clenz-debug/TNTsAllIn1Client/issues).

## The project's own server (nxlc.de)

Custom capes, the friends system and the client logo next to player names run on a small server
operated for this project. Its source code is in [backend/](backend/).

The launcher talks to it only while you are signed in with your Microsoft account - never in
offline mode. You can switch the automatic part off: in the launcher, open "Friends" and turn off
"Friends and online status". The launcher then contacts the server only for your own custom cape (showing,
uploading or removing it in "Skin & Capes").

Your Minecraft access token is not sent to this server. To prove who you are, the launcher uses the
same check a Minecraft server uses when you join it: the server hands out a random one-time number,
the launcher tells Mojang "this account joins server <number>", and the server asks Mojang whether
your account did. The server then gives the launcher a session key of its own, which is valid for
12 hours on this server only, is kept in memory there and is never written to disk. Because of
this check, an account that is not allowed to play multiplayer cannot use friends or upload a cape.
Launcher versions up to 0.14.1 worked differently: they sent the access token itself, which the
server showed to Mojang once and did not store.

What the server stores about you:

| Data | When | Kept |
|---|---|---|
| Minecraft UUID and current player name | From your first sign-in with the launcher | Until you delete your data |
| Friends status (online, away, do not disturb, invisible), the "hide server" setting and the time of your last contact | While the launcher runs, renewed every 20 seconds | Overwritten each time |
| What you are doing: launcher, main menu, singleplayer or multiplayer. The server address of a multiplayer game is included only while "hide server" is off and your status is not "invisible" | While the launcher runs | Overwritten each time |
| The colors of your launcher theme (for the logo other players see next to your name) | While the launcher runs | Overwritten each time |
| Friendships and open friend requests | When you send or accept a request | Until you remove them |
| The players you have blocked | When you block someone | Until you unblock them |
| Invitations into your world: the temporary address of the world and its Minecraft version | When you invite a friend | 10 minutes at most |
| Your active custom cape (a PNG image, reduced to the picture itself - text and other metadata in the file are dropped) | When you upload one | Until you remove it |
| If you report another player's cape: your UUID, that player's UUID and name, what you wrote, and a copy of the cape as it was at that moment | When you report a cape | Until the project's moderators have decided about the report |
| If a cape of yours broke the rules and your account was barred from uploading: your UUID and the reason | When that happens | Until the bar is lifted - also after you delete your data |

Who can see it:

- Your friends see your status and what you are doing. With "hide server" on they see that you are
  in multiplayer, but not where. With the status "invisible" they see you as offline. In both cases
  the server address is not sent to the server at all (launcher versions up to 0.14.1 sent it, and
  the server did not pass it on).
- Reported capes are seen by the project's moderators only, in their launcher: the reported cape,
  the player's name and what the reporters wrote - not who reported. To report a cape the launcher
  asks Mojang for the UUID behind the name you entered.
- Players you have blocked are visible to you alone. A blocked player is not told; a friend request
  from them is answered as if you had never used the client.
- Your custom cape is public: it is stored under your UUID, and the game of every player using this
  client downloads it when they meet you. In the same way your game asks the server for the cape of
  each player you meet, by their UUID (bundled Cape Provider mod, can be switched off in the
  launcher's Mods screen).
- Anyone signed in with this client can ask the server whether a given player UUID has ever used
  the client, and gets that player's logo colors. The client does this for the players in the tab
  list of the server you are on, which means their UUIDs are sent to the server too. Online status
  is never part of that answer.

Like any web server, it sees your IP address with each request and may keep standard access logs.
Cape uploads and removals are logged with player name and UUID, deleting your data with your UUID.

"Delete my data from the server" removes your UUID and name, status, friendships, requests, blocked
players and your active cape. It also switches "Friends and online status" off - switching it back
on registers you again, with an empty friends list.

## Other services

These are contacted because a feature needs them. Each one is run by someone else under its own
privacy terms.

| Service | Why | What is sent |
|---|---|---|
| Microsoft and Xbox Live | Signing in | The sign-in itself happens in your browser on Microsoft's page; the launcher receives and stores the resulting tokens on your PC |
| Mojang (Minecraft services) | Profile, skins and capes, skin upload, friends' player heads, game files, Java runtime, signing in to the project's server | Your access token for your own profile and skin, and together with the one-time number described above when signing in to the project's server; otherwise plain downloads |
| FabricMC | Installing the Fabric loader | Plain downloads |
| Modrinth | Downloading the bundled mods and resource packs, searching and installing mods | Your search text, plain downloads; each request names this launcher and its version |
| GitHub | Launcher updates and the list of bundled mod versions | Plain downloads |
| e4mc | Inviting a friend into your singleplayer world routes that world through e4mc's relay servers | The game's network traffic for that world, only while it is open through an invitation |
| Discord | Rich Presence, off by default | Talks to the Discord app on your own PC only; what Discord shows is chosen in the mod's options |
| Minecraft servers you join | Playing | Whatever Minecraft itself sends |

## What stays on your PC

Sign-in tokens, launcher settings, instances, worlds, the skin and cape library and all mod settings
are stored in `%APPDATA%\tntsallin1client` (or the storage location you picked) and are not uploaded
anywhere.

The sign-in tokens are stored encrypted by Windows for your user account, so the file is of no use
when copied to another PC or read by another Windows user. That does not protect against a program
running under your own account. "Sign out" deletes the tokens. Your player name, UUID and skin stay
on the PC so that you can still play offline; nobody can sign in as you with them.
