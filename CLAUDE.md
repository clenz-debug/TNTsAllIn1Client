# CLAUDE.md

Guidance for Claude Code sessions on this repo (local and cloud).

## Communication

- Always reply to the user in **German**.
- Code, code comments and commit messages stay in **English**, matching the existing repo.
- Commit messages: short, imperative, no prefix convention (see `git log`), e.g.
  `Fix Discord presence never connecting in fresh instances`.

## Project

**TNT's All-In-1 Client** - a Minecraft client (Fabric mod) plus its own Electron launcher.
The name is settled; the project's identity is comfort features bundled in one place
(skin editor, custom capes, friends system, ...), not just performance. Don't suggest renaming.

| Path | What |
|---|---|
| `mod/<mcVersion>/` | Fabric mod, one **independent** Gradle/Loom project per Minecraft version (`1.14.4` = Java 8, `1.20.6`, `1.21.1`, `1.21.8`, `1.21.10` and `1.21.11` = Java 21, `26.1.2`, `26.2` and `26.3` = Java 25). Exception: legacy versions (before 1.14, so far `1.8.9`) have no Fabric - plain Gradle, own entry through LaunchWrapper + Mixin, Legacy Yarn names |
| `launcher/` | Electron + React + TypeScript launcher (electron-vite); `src/main`, `src/preload`, `src/renderer`, `src/shared` |
| `backend/` | Small Node service for custom capes and the friends system, runs on a server the user does not own |
| `designs/` | Branding (logo SVG, color palette) |
| `mod-bundle-manifest.json` | Pins the bundled third-party mods per Minecraft version - **read live by installed launchers** |

Status and plans (German): `Aktuelle_Phase.md` (what is built and what the user has confirmed live),
`Projekt_Roadmap.md` (+ `.en.md`), `Ideen_für_den_client.md`, `mod-bundle-release-runbook.md`.
Check `Aktuelle_Phase.md` before assuming something is untested or mock-only - real Microsoft/Mojang
login, skin upload and multiplayer are live.

## Hard rules

- **Never change `mod-bundle-manifest.json` on `main` without the user's explicit OK.** Installed
  launchers fetch it from `raw.githubusercontent.com/.../main/`, so a push goes live for every player.
  Follow `mod-bundle-release-runbook.md` for manifest changes.
- **Never publish a release** (`npm run release:win`, GitHub releases) without the user's explicit OK.
- **Never deploy the backend** (`backend/deploy/deploy.sh`) or change anything on its server without
  the user's explicit OK - it is someone else's production machine.
- Only remove/move files exactly where asked. Removing something from a bundle does not mean removing
  the user's own copy in their game instance; mention other copies and ask.

## Mod (`mod/`)

- A version-independent fix must be applied by hand to **every** affected `mod/<version>/` - there is
  deliberately no multi-version tool (Stonecutter etc.).
- Menus and screens look the same in every version (user's standing rule): before building or changing
  one, read the same screen in the other versions and mirror its row order, grouping, button placement
  and wording. Deviate only where a version cannot do it, and say so.
- After mod changes, finish with `./gradlew build` (not just `compileJava`) in every touched version dir.
  The launcher copies the jar from `build/libs/`, so a stale jar means the user tests old code.
- After touching a mixin in a 1.21.x or 26.x version, run `python mod/check_mixins.py <version>` - it checks every mixin
  target against that version's Minecraft jar without starting the game.
- In a legacy version (`mod/1.8.9/`) that check is part of `./gradlew build` (`verifyMixins`): it loads
  every mixin target through our own entry against the real obfuscated game, no game window. Code there
  uses Legacy Yarn names (`TitleScreen`, not Mojang's), Java 8 language level, and nothing that touches
  game classes may sit in the `launch` package (LaunchWrapper keeps it out of the game's class loader).
- For tricky vanilla/Sodium behavior, verify via `javap` against the Loom-mapped jars instead of
  guessing - and trace the **full** call chain (including the method that consumes the value you just
  verified) before presenting a fix as the cause.
- The user does visual/in-game testing themselves. Build and check logs for mixin errors; don't drive
  the game window with simulated input or screenshots.

## Launcher (`launcher/`)

- Every new or changed UI string goes into **both** `src/renderer/src/i18n/de.ts` and `en.ts` in the
  same change, with a real English translation (`en.ts` is typed against `de.ts`). Use `useTranslations()`.
- Use the custom `Dropdown` component, never a native `<select>` (its popup ignores the theme).
- Take colors from the theme (`ThemeColors` / CSS vars in `theme.ts`), don't hardcode them.
- Checks: `npm run typecheck`, `npm run build`.
- The user usually runs `npm run dev`: only the renderer hot-reloads. After changes in `src/main/` or
  `src/preload/`, tell them to fully restart the launcher (new IPC otherwise fails with
  "window.api.X is not a function").

## Working with the user

- When they say "make it like on [site/app]", ask for a screenshot instead of guessing from a web fetch.
- Bash heredocs in the user's local environment mangle backslashes; write files containing regexes or
  Windows paths with the file-writing tool instead.

## Cloud sessions

- No Minecraft and no launcher window - write and build code, the user tests locally after pulling.
- Git-ignored folders are missing in a fresh clone: `launcher/mods-bundle/`,
  `launcher/resourcepacks-bundle/`, `launcher/own-mod/`, `mod/*/run/`.
- Gradle and npm need network access to download dependencies.
- Work on a branch; don't push to `main` unless the user asks.
