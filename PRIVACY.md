# Privacy

This page lists what TNT's All-In-1 Client (the launcher and the in-game mod) sends over the
network, to whom, and why. There is no analytics, no advertising, no crash reporting and no usage
statistics.

Questions, or a request to have your data removed from the project's server:
[open an issue](https://github.com/clenz-debug/TNTsAllIn1Client/issues).

## The project's own server (nxlc.de)

Custom capes, the friends system and the client logo next to player names run on a small server
operated for this project. Its source code is in [backend/](backend/).

The launcher talks to it only while you are signed in with your Microsoft account - never in
offline mode. You can switch the automatic part off: in the launcher, open "Friends" and turn off
"Friends and online status". The launcher then contacts the server only for your own custom cape (showing,
uploading or removing it in "Skin & Capes"). To prove who you are, the launcher sends your Minecraft access token with each
request. The server uses it once to ask Mojang which player it belongs to and does not store it.

What the server stores about you:

| Data | When | Kept |
|---|---|---|
| Minecraft UUID and current player name | From your first sign-in with the launcher | Until removed on request |
| Friends status (online, away, do not disturb, invisible), the "hide server" setting and the time of your last contact | While the launcher runs, renewed every 20 seconds | Overwritten each time |
| What you are doing: launcher, main menu, singleplayer or multiplayer including the server address | While the launcher runs | Overwritten each time |
| The colors of your launcher theme (for the logo other players see next to your name) | While the launcher runs | Overwritten each time |
| Friendships and open friend requests | When you send or accept a request | Until you remove them |
| Invitations into your world: the temporary address of the world and its Minecraft version | When you invite a friend | 10 minutes at most |
| Your active custom cape (a PNG image) | When you upload one | Until you remove it |

Who can see it:

- Your friends see your status and what you are doing. With "hide server" on they see that you are
  in multiplayer, but not where. With the status "invisible" they see you as offline. The server
  address is still sent to the server in both cases, it is just not passed on.
- Your custom cape is public: it is stored under your UUID, and the game of every player using this
  client downloads it when they meet you. In the same way your game asks the server for the cape of
  each player you meet, by their UUID (bundled Cape Provider mod, can be switched off in the
  launcher's Mods screen).
- Anyone signed in with this client can ask the server whether a given player UUID has ever used
  the client, and gets that player's logo colors. The client does this for the players in the tab
  list of the server you are on, which means their UUIDs are sent to the server too. Online status
  is never part of that answer.

Like any web server, it sees your IP address with each request and may keep standard access logs.
Cape uploads and removals are logged with player name and UUID.

## Other services

These are contacted because a feature needs them. Each one is run by someone else under its own
privacy terms.

| Service | Why | What is sent |
|---|---|---|
| Microsoft and Xbox Live | Signing in | The sign-in itself happens in your browser on Microsoft's page; the launcher receives and stores the resulting tokens on your PC |
| Mojang (Minecraft services) | Profile, skins and capes, skin upload, friends' player heads, game files, Java runtime | Your access token for your own profile and skin; otherwise plain downloads |
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
