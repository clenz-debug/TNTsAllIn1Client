# Custom Minecraft Client + Launcher — Project Roadmap

## Context

The goal is a custom Minecraft: Java Edition client (comparable to Lunar Client, Badlion Client, or Feather/Dawn Client) including its own desktop launcher. The client aims to improve performance (without requiring in-game settings to be tweaked) and offer a growing list of desired features (see `Ideen_für_den_client.md` (this is in german)). This is a hobby project with no time pressure, expected to take weeks to months of spare time — the roadmap is therefore deliberately broken into small, motivating milestones instead of being planned around one big deliverable.

**Note for continuing work:** New work sessions (including later, fresh conversations) can point directly at this file instead of reloading the previous chat history — saves tokens. It's useful to briefly state in the conversation which phase is currently active and what has already been implemented.

**Fundamental decisions made** (clarified in discussion, considered settled):
- **Base:** Fabric mod (Fabric Loader + Fabric API), deliberately not Forge and not a fully custom loader.
- **Performance:** Bundle existing, proven open-source optimization mods (Sodium, Lithium, etc.), no custom rendering/logic engine built from scratch.
- **Launcher technology:** Electron (TS/JS, renderer ideally with React).
- **Experience level:** Mostly new territory (Java modding, Fabric/Mixins, Electron/frontend, backend/auth) — the roadmap is accordingly built as a learning path with early visible wins, not as a big-bang design.

## Architecture / Repo Structure

A monorepo for the start — one place, one history, easy cross-references:

```
TNTs_Client_Projekt/
├── mod/            Fabric client mod: Java + Gradle + Fabric Loom
│   ├── build.gradle.kts / gradle.properties   (Pins: MC version, Yarn mappings, Loader, Fabric API)
│   └── src/main/java/<pkg>/          <ClientName>Mod.java, mixin/, hud/, inventory/, menu/
├── launcher/       Electron + TypeScript (Renderer: React)
│   └── src/main/{auth,launch,mods,ipc}/, preload/, renderer/
├── backend/        (Phase 8 only, optional) Friends/presence service, Node/TS
├── docs/           Notes, decisions
├── designs/        Mockups, reference screenshots, branding (logo/colors) for launcher + in-game UI
└── Ideen_für_den_client.md   (existing — continue as a living backlog)
```

**How the pieces interact:** `mod/` (Gradle) and `launcher/` (npm/TS) are completely separate build worlds — that's normal, don't try to unify them. At its core, the launcher is a process supervisor: it lays the right jars into an instance directory (`mods/`, `config/`, `saves/`, libraries, assets), builds the Java command line (classpath, JVM args, game args like `--accessToken`, `--uuid`, `--username`), and starts `java` as a child process — auth data flows in purely via startup arguments, no special IPC needed. A runtime connection between launcher and mod (e.g., friends list as an in-game toast) is **only relevant once Phase 8 exists** — even then, a simple JSON file in the instance directory that the mod polls is enough to start with, rather than building a socket protocol right away.

## Phase Plan

Ordering principle: produce something **visibly working** as fast as possible (Phases 1–4) to stay motivated. Legal/licensing/social backend deliberately come later, since they primarily concern *publishing*, not learning/building.

**Phase 0 — Setup & kick off the one slow external dependency**
Install JDK 21 (Temurin/Adoptium), Node.js LTS, Git, IntelliJ IDEA Community; `git init`. Register an Azure AD (Entra ID) **public client app** (no client secret, loopback redirect URI, tenant `consumers`, scope `XboxLive.signin`) and **immediately** submit the Minecraft API allowlist request: `aka.ms/mce-reviewappid`. This is the only step with an uncontrollable lead time — hence right at the start, without waiting for the response.
*Done when:* tools are working, the Azure app exists, the request has been submitted.

**Phase 1 — "Hello World" Fabric mod**
Scaffold with the official Fabric Template Mod Generator (fabricmc.net/develop/template) for the current stable version, open in IntelliJ, run `runClient`. Get a rough understanding of Gradle/`fabric.mod.json`/`ModInitializer`, run `genSources` for readable vanilla code.
*Done when:* the dev client starts, your own log entry/chat message appears on world join, a breakpoint in your own code works.

**Phase 2 — First mixin**
A tiny, visible change (e.g., inject a log line into a render method) to play through the mixin workflow once completely before building anything "real."
*Done when:* the change is visible and you could explain a mixin injection in your own words.

**Phase 3 — Electron launcher MVP: real Microsoft login + vanilla launch**
Auth chain (current, verified state): (1) Microsoft OAuth2 (authorization code flow with loopback redirect) against tenant `consumers`, scope `XboxLive.signin`; (2) Xbox Live auth (`user.auth.xboxlive.com`) → XBL token + user hash; (3) XSTS (`xsts.auth.xboxlive.com`, relying party `rp://api.minecraftservices.com/`); (4) `api.minecraftservices.com/authentication/login_with_xbox` → Minecraft access token (**this is where the 403 hits as long as the Azure app isn't allowlisted**); (5) entitlement (`entitlements/mcstore`) and profile check (`minecraft/profile`). After that: load the version manifest, download client jar/libraries/assets (verify SHA-1 against the manifest), build classpath/JVM args, start `java` as a child process, stream log output into a simple view.
*Practical tip:* while waiting for the allowlisting, build and test everything except steps 4/5 with placeholder profile data — don't let it block you.
*Done when:* "Play" leads to a download and launch of vanilla Minecraft under the correct profile after a real Microsoft login.

**Phase 4 — Fabric launch**
Launcher installs Fabric Loader for the selected version (via `meta.fabricmc.net`) and launches with the custom mod plus bundled optimization mods in the `mods/` folder (manual copying is fine to start).
*Done when:* launch happens via Fabric, the custom mod's log entry appears, Sodium/Lithium are visibly active (F3 shows the Sodium renderer, better frame times).

**Phase 5 — Custom features, easy → hard**
As independent, individually toggleable milestones:
- **5a. Coordinates + compass (HUD)** — the simplest, pure Fabric API (`HudRenderCallback`), no mixins.
- **5b. Material counter, top right** — easy–moderate, inventory access + HUD rendering, hardcoded for one item first, then generalize.
- **5c. Inventory quick-sort** — moderate, but the trickiest of the "easy" tasks: must go through the same slot-click/`ScreenHandler` mechanism as vanilla drag & drop, otherwise desync in multiplayer/survival — deliberately budget time for network correctness here, not just the sorting logic.
- **5d. F3/Shift+F3 improvements** — moderate–hard, mixes directly into the vanilla debug HUD class, which changes unusually often between versions — plan for maintenance effort on version bumps.
- **5e. Custom in-game menu** — moderate, own `Screen` subclass, no mixin needed (unless hooking into the vanilla pause menu itself).
- **5f. 3D/connected textures (glass, etc.)** — the hardest, deliberately saved as a stretch goal for last: reaches deep into the block rendering pipeline; research existing CTM implementations as a reference/dependency instead of reinventing it from scratch.

Second pass, added from `Ideen_für_den_client.md` (personal ideas not yet considered in the first pass), again easy → hard:
- **5g. FPS counter (HUD)** — trivial, own `HudElement` analogous to 5a, no mixins.
- **5h. Zoom (held key reduces FOV)** — trivial, pure keybind/FOV logic via the Fabric API, no mixin needed.
- **5i. Custom crosshair with color picker** — easy, HUD rendering + its own options screen like the existing features.
- **5j. Fullbright + light-level overlay** — easy–moderate, builds on the light-level logic already present in 5d, this time as a persistent overlay instead of an F3 line.
- **5k. Shulker box content preview on keypress** — moderate, read item components/NBT and render as a custom overlay, purely local, no network risk.
- **5l. Adjustable hitbox color** — moderate, needs a mixin into the vanilla hitbox debug renderer, comparable in scope to 5d.
- **5m. Keystrokes display** (more than the classic WASD/mouse/shift/space) — moderate, HUD + key input polling.
- **5n. Screenshot toast with "Open"/"Copy" buttons** — moderate, first check whether Fabric offers a hook for "screenshot saved" or whether a mixin into `Screenshot`/`ScreenshotRecorder` is needed.
- **5o. Item physics** — moderate–hard, following the 5f pattern: bundle an existing mod (e.g. "Item Physic") as a dependency instead of rebuilding it from scratch.
- **5p. Real 3D block models** — no mod code needed (pure vanilla resource pack functionality; Sodium natively renders custom block models with extra geometry); instead research a fitting resource pack and bundle it as its own, unmodified file (not embedded in the mod jar — kept deliberately separate, see license note below).
- **5q. Customizable block outline color** — easy, direct extension of 5l (same mixin pattern, same `ColorPickerPanel`), this time for the vanilla block-targeting outline instead of the F3+B hitbox.
- **5r. Dark inventory** — originally attempted via a mixin overlay (darkening the panel through `AbstractContainerScreen#renderBackground`); after several rounds of live-test feedback (hard-edged box artifacts at sprite corners, darkened icons, dark-on-dark text) scrapped entirely at the user's request and solved the same way as 5f/5p instead: bundled an existing "Dark Inventory" resource pack (**[Default Dark Mode](https://github.com/nebuIr/Default-Dark-Mode)** by nebulr) rather than recoloring textures at runtime.

*Done when:* every feature runs stably, is individually configurable, and works both in the dev client and in an instance started via the launcher.

**Phase 6 — Launcher UX: version selection & mod management**
Version picker (release/snapshot, from the Mojang manifest + Fabric support data), persistent instance configuration (e.g., `electron-store` or small SQLite), update check against a self-hosted manifest (e.g., JSON in a GitHub repo). Mods screen with two tiers: (a) toggle bundled mods (own client mod, Sodium, Lithium) on/off, (b) **load arbitrary additional Fabric mods** — technically low-effort, since Fabric mods are simple jar files that Fabric Loader reads from the instance's `mods/` folder: the launcher fundamentally just needs to manage that folder (add jars via drag & drop, enable/disable individually e.g. via a rename/subfolder convention, remove) plus a simple check for whether a matching Fabric API version is present. A later, more comfortable extension would be a mod search directly in the launcher via the public Modrinth API — not a must-have for the first version.
*Done when:* a new instance can be created via dropdown, bundled mods can be toggled, additional custom Fabric mod jars can be added/removed, launch works without manual file operations in the filesystem.

**Phase 7 — Skin & cape editor**
Order matters: (1) load the current skin/cape (`GET minecraft/profile`) and implement **uploading an existing PNG** first (already requires the full API integration, immediately useful); (2) a curated selection window/gallery afterward; (3) a real pixel editor last (a standalone feature, doesn't block anything else). Use an existing skin-viewer library for the 3D preview instead of rendering it yourself.
*Note:* the upload endpoint is multipart/form-data; according to community reports, excessive error retries can lead to temporary account suspensions — plan for debouncing/error handling.
*Done when:* selecting a PNG updates the real Minecraft profile skin, visible on the next game launch.

**Phase 8 — Friends/social system (deliberately later, optional, its own mini-project)**
Mojang/Microsoft provide **no** friends API — meaning a custom backend service has to be designed and hosted. Minimal architecture: a small Node/TS backend (same language as the launcher), accounts linked via Minecraft UUID and **verified server-side** (never blindly trust a client-claimed UUID), a database (SQLite or a free-tier Postgres like Supabase/Railway), friend request/accept/remove + WebSocket presence, a friends UI in the launcher.
*Done when:* two test accounts can add each other and see near-real-time online/offline resp. "playing on X" status.

**Phase 9 — Packaging, distribution, auto-update**
Launcher: `electron-builder` (NSIS/dmg/AppImage) + `electron-updater` against, e.g., GitHub Releases. Mod updates: the launcher checks a self-controlled manifest and re-downloads the mod jar plus pinned Sodium/Lithium versions per Minecraft version — essentially its own small "app store" for jars. Unsigned Windows installers trigger SmartScreen warnings — plan for this if needed.
*Done when:* a friend downloads a single installer, logs in, and gets a working, modded instance without any manual file operations.

## License / Legal Points (check before public release, not a blocker for development)

- **Sodium**: PolyForm Shield License 1.0.0 — bundling it in a full-featured client is explicitly allowed (without credit/permission), except if you build a directly competing rendering-mod product. Read the license text yourself before public release.
- **Lithium**: LGPL-3.0-only — unmodified bundling is unproblematic (include notices), your own mod doesn't need to be open-sourced; only your own changes to Lithium itself would have to stay LGPL.
- **3D Default (resource pack, Phase 5p)**: GPL-3.0-only — unlike Sodium (PolyForm Shield) and Lithium/Continuity (both LGPL), this is the first **full** copyleft GPL rather than LGPL. Deliberately **not** embedded in the mod jar (no `registerBuiltinResourcePack`), but shipped as an unmodified, separate file alongside the mod (`launcher/resourcepacks-bundle/`) to stay within "mere aggregation" (GPLv3 §5) rather than risk a derivative/combined work with the differently-licensed mod code. Double-check explicitly before public release, ideally with someone GPL-savvy — higher risk than the previous bundles.
- **Default Dark Mode (resource pack, Phase 5r)**: CC-BY-NC-SA-4.0 (Creative Commons, non-commercial, share-alike, attribution required) — the first Creative Commons license in the project, unlike the code licenses above (PolyForm/LGPL/GPL). Same treatment as 3D Default: shipped unmodified as a separate file in `launcher/resourcepacks-bundle/`, not embedded in the mod jar. Non-commercial fits the project's current hobby status; **re-check explicitly before any commercial/monetized release** (the NC clause), and credit the author (nebulr, github.com/nebuIr/Default-Dark-Mode) in the planned "Third-Party Licenses/Credits" screen.
- **Blocky 3D Pointed Dripstone (resource pack, Phase 5p follow-up)**: BSD-3-Clause — the least restrictive bundle so far (permissive, allows commercial use too, only requires keeping the copyright notice + license text on redistribution, both automatically satisfied since it ships unmodified as its own file). Fills the gap 3D Default leaves for pointed dripstone (verified: no overlapping/conflicting model files between the two packs). By "poqbox" (author of the "Blocky Replacements" series) — also credit in the planned credits screen.
- **Bushy Vegetation (resource pack, Phase 5p follow-up)**: BSD-3-Clause, also by "poqbox". Covers vines (overworld vines **and** confirmed weeping/twisting vines in the Nether, verified via screenshot) plus, as a side effect, bushier grass/ferns/flowers/sweet berry bush - no overlap with any 3D Default model file (3D Default only touches vine-related textures there, not models).
- **3D Bushy Bushie (resource pack, Phase 5p follow-up)**: Apache-2.0 (also very permissive). Covers the new "Bush" block plus sweet berry/firefly bush - the only match found specifically for the "Bush" block, still fairly new/low-traction (92 downloads), so less battle-tested than the other bundles.
- **Mushrooms Plus (resource pack, Phase 5p follow-up)**: MIT. Replaces 3D Default's own mushroom/fungus model (which the user didn't like) - **load order matters**: needs to sit above 3D Default in the in-game resource pack list, or 3D Default's model wins.
- **Vanilla Spinning Stonecutter (3D) (resource pack, Phase 5p follow-up)**: MIT. 3D Default already ships its own static 3D model for the stonecutter blade - this pack overrides it (same load-order note as above) and additionally adds a real spin animation (via plain vanilla mechanics, no mod needed) that 3D Default doesn't have.
- Check every additional bundled mod individually — "open source" doesn't automatically mean "freely bundleable" (MIT/CC0/GPL/LGPL/All-Rights-Reserved are all present in the ecosystem).
- Plan for a "Third-Party Licenses/Credits" screen in both the mod and the launcher.
- **Mojang Usage Guidelines** (minecraft.net/en-us/usage-guidelines): don't redistribute Mojang's own game files (every user downloads them via their own entitlement — just like with the vanilla launcher), don't imply official Mojang/Microsoft endorsement, watch for "no unfair gameplay advantage" if monetizing — guidelines change, re-read them yourself at the relevant point in time.
- Server compatibility (e.g., Hypixel): purely performance/cosmetic client bundles are allowed under their policy (precedent: Lunar/Badlion) — all feature ideas so far are clearly within that lane.

## Learning Resources

- **Fabric docs**: docs.fabricmc.net, Fabric Wiki (wiki.fabricmc.net, good mixin examples), Template Mod Generator (fabricmc.net/develop/template), `FabricMC/fabric-example-mod`.
- **Fabric Discord** (official link only via fabricmc.net — fake servers exist).
- **Mixin**: `SpongePowered/Mixin` GitHub wiki (canonical), `mixin-wiki.readthedocs.io` (community, more beginner-friendly).
- **Auth/protocol reference**: wiki.vg was shut down on 2024-11-30, its content lives on in **minecraft.wiki** (community, unofficial, but currently the best source) — see the "Microsoft Authentication" and "Mojang API" pages.
- **Reference launcher to read**: **Voxelum/x-minecraft-launcher (XMCL)** — actively maintained, Electron+Vue+TS, with reusable `@xmcl/*` packages for auth/install/launch — probably the best "read real, current code" reference for exactly this stack. **Prism Launcher** (C++/Qt, not Electron) for the logic (auth flow, instance model) independent of the UI framework.
- **Mod language choice**: start with plain **Java**, not Kotlin — almost all Fabric tutorials are in Java; `fabric-language-kotlin` allows switching later if desired.
- **Electron**: electron.build docs (`electron-builder`/`electron-updater` as the current standard).

## Risks / Open Points

1. The Azure app allowlisting process/duration isn't reliably documented (form `aka.ms/mce-reviewappid` vs. references to the ID@Xbox program in community sources) — kick it off early, don't block on it.
2. Mixin/mapping breakage on every Minecraft version bump (especially the debug HUD, Phase 5d) — deliberately staying on one version for a long time is a legitimate hobby-project decision.
3. Sodium/Lithium updates must be actively tracked and re-pinned.
4. License interpretation of Sodium's "doesn't compete with" clause is a matter of judgment, not a bright line — when in doubt, ask in the Fabric/CaffeineMC community.
5. Electron+JVM process management (crash detection, OOM, log handling, restart after mod changes) needs real design, not just `spawn()`.
6. **Not independently verified**: the current Minecraft version numbering was ambiguous at research time (the classic `1.21.x` scheme vs. hints of a newer year-based scheme like `26.1`) — when starting Phase 1, check fabricmc.net directly for the currently supported stable version instead of relying on version numbers from this plan.

## Next Concrete Steps

1. `git init` in the project directory.
2. Install JDK 21, Node.js LTS, Git, IntelliJ IDEA Community.
3. Register the Azure AD app + submit the allowlist request (`aka.ms/mce-reviewappid`).
4. In parallel: use the Fabric Template Mod Generator, get `runClient` working (Phase 1).
