# TNT's All-In-1 Client

A Minecraft: Java Edition client for Windows: a launcher plus a Fabric mod that bundle comfort
features in one place - skin editor, custom capes, a friends system, HUD elements, waypoints and
more - so you don't have to collect them from a dozen separate mods.

This is a hobby project by one developer. It is not affiliated with or endorsed by Mojang or
Microsoft. You need your own Minecraft: Java Edition account to play.

German project notes: [Projekt_Roadmap.md](Projekt_Roadmap.md) (English:
[Projekt_Roadmap.en.md](Projekt_Roadmap.en.md)).

## Download

[Download the installer for Windows](https://github.com/clenz-debug/TNTsAllIn1Client/releases/latest/download/tntsallin1client-setup.exe)
(always the newest release). All releases: [Releases](https://github.com/clenz-debug/TNTsAllIn1Client/releases).

The installer sets up the launcher only. The game itself, Fabric and the bundled mods are downloaded
by the launcher from their original sources (Mojang, FabricMC, Modrinth) the first time you start a
Minecraft version. The launcher updates itself from this repository's releases.

To remove it again, use "Apps" in the Windows settings or the uninstaller in the installation
folder. Your instances and worlds in `%APPDATA%\tntsallin1client` stay unless you delete that folder
yourself.

## What it does

**Launcher**

- Sign in with your Microsoft account, or play offline once you have signed in before
- Instances: several separate game folders per Minecraft version, each with its own mods
- Search and install mods from Modrinth, add your own mod files
- Skin editor with a pixel editor, a skin library and 3D preview; upload skins to your account
- Custom capes that other players using this client can see
- Friends: add friends by Minecraft name, see who is online, join their server, invite them into
  your singleplayer world
- Take over settings, mods, resource packs and worlds from another installed client
- Color themes, German and English, a guided tour for first-time users

**In-game mod** (currently for Minecraft 1.21.11 and 26.1.2; other versions start without it)

- HUD: coordinates, FPS, ping, keystrokes, armor and tool status, item counter - all movable
- Zoom, freecam, custom crosshair, fullbright, no fog, mob spawn overlay
- Waypoints with beams, labels and direction arrows
- 3D skin layers, 3D block models, connected textures, dark mode
- Inventory helpers: quick sort, shulker box preview, pinned recipes
- Custom hitbox and block outline colors, screenshot copy, Discord Rich Presence (opt-in)

Every feature of the mod can be switched on and off in its own menu.

## Privacy

What the launcher and the mod send over the network, and to whom, is described in
[PRIVACY.md](PRIVACY.md).

## Code signing policy

Free code signing provided by [SignPath.io](https://about.signpath.io), certificate by
[SignPath Foundation](https://signpath.org).

- Committers and reviewers: [clenz-debug](https://github.com/clenz-debug)
- Approvers: [clenz-debug](https://github.com/clenz-debug)

Release builds are made from this repository by GitHub Actions
([build-windows.yml](.github/workflows/build-windows.yml)). Privacy policy: [PRIVACY.md](PRIVACY.md).

## Building from source

The repository holds three independent parts:

| Path | What | Build |
|---|---|---|
| `mod/1.21.11/` | Fabric mod for Minecraft 1.21.11 (Java 21) | `./gradlew build` |
| `mod/26.1.2/` | Fabric mod for Minecraft 26.1.2 (Java 25) | `./gradlew build` |
| `launcher/` | Electron launcher (Node.js) | `npm ci`, then `npm run dev` or `npm run package:win` |
| `backend/` | Small Node.js service for capes and friends | `npm ci`, then `npm run build` |

`npm run package:win` builds the installer into `launcher/dist/` and expects both mods to be built
first, because it packs their jars.

## License

TNT's All-In-1 Client is licensed under the [GNU General Public License v3.0](LICENSE).

The mods and resource packs the launcher downloads (Fabric API, Sodium, Lithium, Continuity, Cape
Provider, e4mc, Default Dark Mode, Bushy Vegetation) are separate works under their own licenses and
are not part of this repository or the installer. Their license texts are listed in
[launcher/third-party-licenses](launcher/third-party-licenses/README.txt). Please report problems
with them here, not to their developers.
