# Aktuelle Phase

**Phase 0 — Setup & externe Abhängigkeit anstoßen** ✅ abgeschlossen

**Phase 1 — "Hello World"-Fabric-Mod** ✅ abgeschlossen

**Phase 2 — Erstes Mixin** ✅ abgeschlossen

**Phase 3 — Electron-Launcher-MVP** 🔧 in Arbeit (blockiert auf Mojang-API-Freischaltung für den letzten Teil)

## Phase 3 — Zwischenstand
`launcher/` angelegt: Electron 43 + electron-vite 5 + TypeScript 7 + React 19, Ordnerstruktur wie in der Roadmap (`src/main/{auth,launch,ipc}/`, `src/preload/`, `src/renderer/`, plus `src/shared/` für Typen/IPC-Kanalnamen, die von allen drei Seiten geteilt werden). Bewusst keine zusätzlichen Abhängigkeiten wie `electron-store` oder MSAL — Auth-Kette ist handgeschriebenes `fetch` gegen die dokumentierten Endpunkte (Lernziel laut Roadmap), Token-Cache ist eine simple JSON-Datei in `userData`.

**Was echt funktioniert (unabhängig vom Mojang-Blocker):**
- Auth-Schritte 1–3 komplett real implementiert: Microsoft OAuth2 mit PKCE über einen Loopback-HTTP-Server auf einem ephemeren Port (passt zur in Azure registrierten bloßen `http://localhost`-Redirect-URI, die Microsoft laut Doku für Desktop-Clients gegen jeden Port matcht), System-Browser-Login via `shell.openExternal` (kein eingebettetes WebView, damit die App nie Microsoft-Zugangsdaten sieht), danach Xbox-Live-User-Auth und XSTS-Autorisierung (`xboxLive.ts`) inkl. Klartext-Fehlermeldungen für die bekannten XSTS-Fehlercodes (kein Xbox-Profil / Kinderkonto ohne Zustimmung).
- Komplette Download/Start-Pipeline real gebaut und **gegen die echte Mojang-API/CDN smoke-getestet** (Version-Manifest für 1.21.11 geladen, Libraries nach OS gefiltert, eine echte Library heruntergeladen und SHA-1-verifiziert, Re-Download bei vorhandener gültiger Datei korrekt übersprungen, Classpath + JVM-/Game-Args mit aufgelösten Platzhaltern gebaut, `java` auf dem PATH gefunden).
- Sicherheits-Defaults gesetzt: `contextIsolation: true`, `nodeIntegration: false`, `sandbox: true`, schmale `contextBridge`-API im Preload.

**Was noch mit Dev-Mock läuft (Schritte 4–5, `minecraftAuth.ts`):** `login_with_xbox` + Profil-Abruf werden echt versucht; schlagen sie fehl (aktuell 403, da Azure-App noch nicht freigeschaltet), greift ein klar markiertes Platzhalterprofil (`DevPlayer`, `isMock: true`), das in der UI sichtbar als "Dev-Mock-Profil"-Badge markiert wird. Sobald Mojang freischaltet, entfällt der Fallback-Zweig ersatzlos — kein sonstiger Code muss sich ändern.

**Bug beim ersten echten Login-Test gefunden und gefixt:** Erster Versuch von "Mit Microsoft anmelden" scheiterte mit `unauthorized_client: The client does not exist or is not enabled for consumers`. Durchgecheckt: "Supported account types" in Azure war korrekt auf "Nur persönliche Konten" gesetzt (also nicht der naheliegende Verdacht) — die tatsächliche Ursache war ein Tippfehler beim Abschreiben der Application (Client) ID in Phase 0: notiert war `6820dddd-8bc1-4fe6-ab9b-945a3a7d400a`, echt ist `6820dddb-...` (`b` statt `d` als achtes Zeichen). Fix: `launcher/src/main/config.ts` und die Notiz oben auf die korrekte ID korrigiert. Lehre: GUIDs beim Abtippen aus dem Portal immer per Copy-Button kopieren, nie von Hand/Screenshot abschreiben.

**Verifiziert:**
- `npm run typecheck` (main+preload und renderer getrennt) sauber ohne Fehler.
- `npm run build` (electron-vite, alle drei Targets) erfolgreich.
- Pipeline-Smoke-Test (temporäres Skript, nicht Teil des Repos) gegen echte Mojang-Endpunkte durchgelaufen: Manifest/Version-Detail, OS-Filterung, Download+SHA-1, Classpath/Args, `java`-Verfügbarkeit — alles ✅.
- **Nicht verifiziert von mir:** das eigentliche GUI-Fenster und der echte Microsoft-Login-Klick-Durchlauf — `npm run dev` lässt sich aus der Agent-Shell nicht bis zum sichtbaren Fenster durchstarten, weil dort `ELECTRON_RUN_AS_NODE=1` gesetzt ist (Electron läuft dann als reiner Node-Prozess ohne `app`/`BrowserWindow`-APIs, sehr wahrscheinlich eine bewusste Sandbox-Absicherung gegen automatisch aufpoppende GUI-Fenster). **Nächster Schritt für den Nutzer:** `npm run dev` in einem normalen Terminal (nicht über den Agenten) starten, Fenster sollte erscheinen, "Mit Microsoft anmelden" testen (Schritte 1–3 sind real) und optional "Skip Login (Dev-Mock)" für die Play-Pipeline.

## Phase 2 — Erledigt
- Mixin-Infrastruktur aufgesetzt: `src/main/resources/tntsallin1client.mixins.json` (Package `com.tntsallin1client.mixin`, `compatibilityLevel: JAVA_21`), in `fabric.mod.json` unter `"mixins"` registriert. Fabric Loom (1.17.16, über den `net.fabricmc.fabric-loom-remap`-Plugin-Alias) erkennt die Config automatisch und übernimmt Annotation-Processing/Refmap ohne zusätzliche `build.gradle.kts`-Änderungen.
- `mixin/GuiMixin.java`: `@Mixin(Gui.class)` mit `@Inject(method = "render", at = @At("TAIL"))` in `net.minecraft.client.gui.Gui#render(GuiGraphics, DeltaTracker)` — die zentrale HUD-Render-Methode. Zielsignatur vorher direkt aus dem von `genSources` erzeugten, gemappten Vanilla-Quellcode verifiziert (Mojang-Mappings, kein Blindraten).
  - Zeichnet einmal pro Frame den Text "TNT's All-In-1 Client (Mixin active)" oben links ins HUD (`guiGraphics.drawString(...)`).
  - Loggt genau einmal (statisches Flag) eine Bestätigungszeile beim ersten Feuern, statt bei jedem Frame zu spammen.
- **Bug während des Tests gefunden und gefixt:** Farbwert `0xFFFFFF` an `drawString` übergeben wurde als 32-Bit-ARGB zu `0x00FFFFFF` (Alpha = 0) — `GuiGraphics.drawString` verwirft den Aufruf still, wenn `ARGB.alpha(k) == 0` (Quelle: `GuiGraphics.java`, Zeile 253). Fix: `0xFFFFFFFF` (volles Alpha) verwendet. Lehre: Farbwerte für GuiGraphics-Zeichenmethoden immer als ARGB inkl. Alpha-Byte angeben, nicht als reines RGB.
- Verifiziert im Dev-Client (`runClient`, zwei Durchläufe — erster zeigte den Bug, zweiter nach dem Fix bestätigt):
  - Log-Zeile `[tntsallin1client] Phase 2 mixin fired: injected into Gui#render.` erscheint beim Weltbeitritt ✅
  - Text ist sichtbar im HUD oben links ✅ (visuell vom Nutzer bestätigt)
- Mixin-Injection in eigenen Worten: Der Fabric-Mixin-Annotation-Prozessor webt zur Kompilierzeit Bytecode aus `GuiMixin` in die vom Spiel geladene `Gui`-Klasse ein — `@At("TAIL")` platziert den injizierten Code kurz vor dem `return` der `render`-Methode, sodass er nach allem Vanilla-HUD-Rendering, aber noch im selben Methodenaufruf läuft. Kein Overriding/Ersetzen der Originalmethode, sondern additive Injection in bestehenden Bytecode.

## Phase 1 — Erledigt
- Versionsrecherche direkt bei fabricmc.net/Modrinth (nicht auf die Roadmap-Planzahlen verlassen, siehe Risikohinweis #6):
  - **Minecraft hat tatsächlich auf ein jahresbasiertes Versionsschema gewechselt** (aktuell stabil: `26.2`, davor `26.1.x`). Der klassische `1.21.x`-Strang läuft aber parallel weiter und ist ebenfalls aktuell stabil (`1.21.11`).
  - Entscheidung: **`1.21.11` statt `26.2`** für den Mod verwendet. Grund: FabricMC's eigenes Referenz-Template für `26.2` verlangt **JDK 25**, `1.21.11` läuft mit dem bereits installierten **JDK 21** (Phase 0). Fabric API, Loader (`0.19.3`) und Sodium sind für beide Versionen aktuell — ein späterer Umstieg auf `26.x` ist somit ein reiner Versions-Bump, kein Rewrite.
- `mod/`-Projekt angelegt (Fabric Loom, Kotlin DSL wie in der Roadmap festgelegt, offizielle Mojang-Mappings statt Yarn — Yarn hat für `26.x` noch keine Builds, für `1.21.11` wären sie zwar da, aber das Template selbst nutzt inzwischen `officialMojangMappings()`):
  - `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties` (MC `1.21.11`, Loader `0.19.3`, Loom `1.17-SNAPSHOT`, Fabric API `0.141.5+1.21.11`).
  - Gradle Wrapper (Gradle `9.5.1`) aus FabricMC's offiziellem `fabric-example-mod`-Repo (Branch `1.21.11`) übernommen.
  - `com.tntsallin1client.TNTsAllIn1ClientMod` — Client-Entrypoint, Logger-Zeile beim Start, Log + Chat-Nachricht via `ClientPlayConnectionEvents.JOIN` beim Welt-Beitritt.
  - Bewusst **kein** Mixin-Beispiel und **kein** separates `client`-Sourceset eingebaut — Mixins sind laut Roadmap erst Phase 2, und das Projekt ist ohnehin reines Client-Mod (kein Server-Entrypoint nötig).
- `./gradlew build` lokal erfolgreich (Mojang-Mappings-Setup, Kompilierung, Jar+Sources-Jar) — `tntsallin1client-0.1.0.jar` liegt in `mod/build/libs/`. `genSources`- und `runClient`-Tasks sind vorhanden und einsatzbereit.

- IntelliJ: "Trust Project" bestätigt, Gradle-Sync durchgelaufen, "Minecraft Development"-Plugin (minecraft-dev, Fabric/Mixin-Support) installiert.
- `genSources` gelaufen (Vineflower, 6622 Klassen dekompiliert) — lesbarer Vanilla-Code für Navigation/Debugging verfügbar.
- `runClient` im Debug-Modus gestartet, Singleplayer-Welt erstellt. **Alle drei "Fertig, wenn"-Kriterien bestätigt:**
  - Dev-Client startet ✅
  - Breakpoint funktioniert ✅ (griff zuerst bei Zeile 19, dem `onInitializeClient`-Start — `client` ist dort noch nicht im Scope, das ist erwartet, da erst innerhalb der Lambda ab Zeile 22 verfügbar; nach Resume normal weitergelaufen)
  - Log-Zeile + Chat-Nachricht beim Welt-Beitritt ✅ ("TNT's All-In-1 Client loaded." im Chat, `[tntsallin1client] Joined a world.` im Log)

## Erledigt
- `git init` im Projektverzeichnis, Commits erstellt.
- `.gitignore` für das geplante Monorepo (mod/, launcher/, backend/) angelegt.
- JDK 21 (Temurin/Adoptium) installiert — `java -version` → 21.0.11.
- Node.js LTS installiert — `node -v` → v24.18.0.
- IntelliJ IDEA Community Edition installiert.
- Git war bereits vorhanden (2.51.2).
- Azure AD (Entra ID) Public-Client-App registriert: `TNTsAllIn1Client`, "Nur persönliche Konten", Redirect-URI `http://localhost` (öffentlicher Client), "Öffentliche Clientflows zulassen" = Ja.
  - Application (Client) ID: `6820dddb-8bc1-4fe6-ab9b-945a3a7d400a` (in einer früheren Version dieser Notiz stand fälschlich `6820dddd...` — Tippfehler beim Abschreiben aus dem Portal, hat in Phase 3 den Microsoft-Login mit `unauthorized_client: The client does not exist` blockiert, siehe Phase-3-Abschnitt)
  - Verzeichnis-ID (Tenant): `a3dd876f-c607-4a29-a300-73b9e7130cbb`
- Minecraft-API-Freischaltungsantrag unter aka.ms/mce-reviewappid abgeschickt (2026-07-15). **Antwortzeit unklar — laut Roadmap-Risikohinweis nicht darauf blockieren, direkt mit Phase 1 weitermachen.**
- Öffentliches GitHub-Repo angelegt und gepusht: [github.com/clenz-debug/TNTsAllIn1Client](https://github.com/clenz-debug/TNTsAllIn1Client)
- Projekt-Roadmap zusätzlich auf Englisch verfügbar: `Projekt_Roadmap.en.md`.

*Fertig, wenn:* Tools laufen (✅), Azure-App existiert (✅), Antrag ist abgeschickt (✅).

## Blocker-Log (Azure-Login, gelöst)
Beim Registrieren der Azure-App gab es mehrere ineinandergreifende Probleme, bis alles lief:

1. **Tenant-Routing-Fehler** (`AADSTS50020`/`AADSTS16000`, Tenant "Microsoft Services"): Login/Hintergrund-Widgets versuchten wiederholt, einen Token gegen einen fremden Phantom-Tenant zu holen, in dem der Account kein Mitglied ist. Trat bei mehreren Accounts gleich auf.
2. **Inkognito-Fenster** löste einen neuen Fehler aus (`AADSTS50058`, silent sign-in), da Inkognito-Modus Third-Party-Cookies blockiert, die Azure Portal fürs Silent-SSO braucht — zurück zu normalem Fenster.
3. **Systemzeit war nie synchronisiert** (`w32tm`: "nicht synchronisiert", Quelle "Local CMOS Clock") — nach manueller Sync über Windows-Einstellungen lief der Login sauber durch.
4. **Root Cause**: Der Account hatte schlicht noch **kein eigenes Entra-ID-Verzeichnis** (App-Registrierungen-Seite zeigte das explizit an). Der "Mandanten verwalten"-Self-Service-Weg war gesperrt, da er ohne bestehendes Abo/Lizenz keine Tenant-Erstellung erlaubt.
5. **Lösung**: Kostenloser Azure-Free-Account-Signup (Telefon-/Kartenverifizierung, keine Kosten im Free-Tier) hat automatisch ein echtes Verzeichnis provisioniert — danach lief die App-Registrierung ohne Probleme durch.

## Nächster Schritt
1. **Nutzer:** `npm run dev` in `launcher/` in einem echten Terminal starten (nicht über den Agenten, siehe Hinweis oben), Fenster prüfen, echten Microsoft-Login (Schritte 1–3) einmal durchklicken, danach über "Skip Login (Dev-Mock)" die volle Download/Start-Pipeline mit Mock-Profil live beobachten (`java` sollte starten und typischerweise an der eigenen Minecraft-Session-Prüfung scheitern, da der Access-Token gefälscht ist — das ist erwartet).
2. Weiter auf Antwort zum Minecraft-API-Freischaltungsantrag warten (gestellt 2026-07-15, `aka.ms/mce-reviewappid`).
3. Sobald freigeschaltet: `completeMinecraftLogin` in `launcher/src/main/auth/minecraftAuth.ts` real ohne Mock-Fallback testen, Phase 3 als ✅ abgeschlossen markieren, dann Phase 4 (Fabric-Start) angehen.
