# Custom Minecraft-Client + Launcher — Projekt-Roadmap

## Kontext

Ziel ist ein eigener Minecraft: Java Edition Client (vergleichbar mit Lunar Client, Badlion Client oder Feather/Dawn Client) inklusive eigenem Desktop-Launcher. Der Client soll die Performance verbessern (ohne dass In-Game-Einstellungen angepasst werden müssen) und eine wachsende Liste gewünschter Features bieten (siehe `Ideen_für_den_client.md`). Es handelt sich um ein Hobbyprojekt ohne Zeitdruck, das voraussichtlich Wochen bis Monate in der Freizeit in Anspruch nehmen wird — entsprechend ist die Roadmap bewusst in kleine, motivierende Meilensteine aufgeteilt statt auf ein großes Ganzes hin geplant.

**Hinweis zur Weiterarbeit:** Neue Arbeitssitzungen (auch spätere, frische Konversationen) können direkt auf diese Datei verweisen, statt den bisherigen Chatverlauf neu zu laden — spart Tokens. Sinnvoll ist, in der jeweiligen Konversation kurz zu sagen, welche Phase gerade dran ist bzw. was bereits umgesetzt wurde.

**Getroffene Grundsatzentscheidungen** (im Gespräch geklärt, gelten als gesetzt):
- **Basis:** Fabric-Mod (Fabric Loader + Fabric API), bewusst nicht Forge und kein komplett eigener Loader.
- **Performance:** Bestehende, bewährte Open-Source-Optimierungsmods bündeln (Sodium, Lithium u.ä.), keine eigene Rendering-/Logic-Engine von Grund auf.
- **Launcher-Technologie:** Electron (TS/JS, Renderer idealerweise mit React).
- **Erfahrungsstand:** Größtenteils Neuland (Java-Modding, Fabric/Mixins, Electron/Frontend, Backend/Auth) — die Roadmap ist entsprechend als Lernpfad mit früh sichtbaren Erfolgen aufgebaut, nicht als Big-Bang-Design.

## Architektur / Repo-Struktur

Ein Monorepo für den Anfang — ein Ort, eine Historie, einfache Cross-Referenzen:

```
TNTs_Client_Projekt/
├── mod/            Fabric-Client-Mod: Java + Gradle + Fabric Loom — ein eigener Ordner pro
│   │                 unterstützter Minecraft-Version (siehe mod-bundle-release-runbook.md)
│   ├── 1.21.11/    build.gradle.kts / gradle.properties (Pins: MC-Version, Mappings, Loader, Fabric API)
│   │   └── src/main/java/<pkg>/      <ClientName>Mod.java, mixin/, hud/, inventory/, menu/
│   └── 26.1.2/     eigenständiges Gradle-Projekt, gleicher Aufbau wie oben
├── launcher/       Electron + TypeScript (Renderer: React)
│   └── src/main/{auth,launch,mods,ipc}/, preload/, renderer/
├── backend/        (erst Phase 8, optional) Freunde-/Presence-Service, Node/TS
├── docs/           Notizen, Entscheidungen
├── designs/        Mockups, Referenz-Screenshots, Branding (Logo/Farben) für Launcher + Ingame-UI
└── Ideen_für_den_client.md   (bestehend — als lebendes Backlog weiterführen)
```

**Zusammenspiel:** `mod/` (Gradle) und `launcher/` (npm/TS) sind komplett getrennte Build-Welten — das ist normal, nicht angleichen. Der Launcher ist im Kern ein Prozess-Supervisor: er lädt die richtigen Jars in ein Instanzverzeichnis (`mods/`, `config/`, `saves/`, Libraries, Assets), baut die Java-Kommandozeile (Classpath, JVM-Args, Game-Args wie `--accessToken`, `--uuid`, `--username`) und startet `java` als Kindprozess — Auth-Daten fließen rein über Startargumente, keine spezielle IPC nötig. Eine Laufzeit-Verbindung Launcher↔Mod (z.B. Freundesliste als In-Game-Toast) ist **erst relevant, wenn Phase 8 existiert** — dann reicht anfangs sogar eine simple JSON-Datei im Instanzverzeichnis, die der Mod pollt, statt gleich ein Socket-Protokoll zu bauen.

## Phasenplan

Reihenfolge-Prinzip: so schnell wie möglich etwas **sichtbar Funktionierendes** erzeugen (Phasen 1–4), das motiviert. Rechtliches/Lizenzen/Social-Backend kommen bewusst später, da sie primär die *Veröffentlichung* betreffen, nicht das Lernen/Bauen.

**Phase 0 — Setup & den einen langsamen externen Abhängigkeit anstoßen**
JDK 21 (Temurin/Adoptium), Node.js LTS, Git, IntelliJ IDEA Community installieren; `git init`. Azure AD (Entra ID) **Public-Client-App** registrieren (kein Client-Secret, Loopback-Redirect-URI, Tenant `consumers`, Scope `XboxLive.signin`) und **sofort** den Minecraft-API-Freischaltungsantrag stellen: `aka.ms/mce-reviewappid`. Das ist der einzige Schritt mit unkontrollierbarer Vorlaufzeit — deshalb ganz an den Anfang, ohne auf die Antwort zu warten.
*Fertig, wenn:* Tools laufen, Azure-App existiert, Antrag ist abgeschickt.

**Phase 1 — "Hello World"-Fabric-Mod**
Mit dem offiziellen Fabric Template Mod Generator (fabricmc.net/develop/template) für die aktuelle Stable-Version scaffolden, in IntelliJ öffnen, `runClient` starten. Gradle/`fabric.mod.json`/`ModInitializer` grob verstehen, `genSources` für lesbaren Vanilla-Code laufen lassen.
*Fertig, wenn:* Dev-Client startet, eigener Log-Eintrag/Chat-Nachricht beim Welt-Beitritt, Breakpoint im eigenen Code funktioniert.

**Phase 2 — Erstes Mixin**
Eine winzige, sichtbare Änderung (z.B. Log-Zeile in eine Render-Methode injizieren) um den Mixin-Workflow einmal komplett durchzuspielen, bevor etwas "Echtes" gebaut wird.
*Fertig, wenn:* Änderung sichtbar ist und du eine Mixin-Injection in eigenen Worten erklären könntest.

**Phase 3 — Electron-Launcher-MVP: echter Microsoft-Login + Vanilla-Start**
Auth-Kette (aktueller Stand, verifiziert): (1) Microsoft OAuth2 (Authorization-Code-Flow mit Loopback-Redirect) gegen Tenant `consumers`, Scope `XboxLive.signin`; (2) Xbox-Live-Auth (`user.auth.xboxlive.com`) → XBL-Token + User-Hash; (3) XSTS (`xsts.auth.xboxlive.com`, Relying Party `rp://api.minecraftservices.com/`); (4) `api.minecraftservices.com/authentication/login_with_xbox` → Minecraft-Access-Token (**hier greift der 403, solange die Azure-App nicht freigeschaltet ist**); (5) Entitlement- (`entitlements/mcstore`) und Profil-Check (`minecraft/profile`). Danach: Version-Manifest laden, Client-Jar/Libraries/Assets herunterladen (SHA-1 gegen Manifest prüfen), Classpath/JVM-Args bauen, `java` als Kindprozess starten, Log-Ausgabe in eine einfache Ansicht.
*Praxis-Tipp:* Während auf die Freischaltung gewartet wird, alles außer Schritt 4/5 mit Platzhalter-Profildaten bauen und testen — nicht blockieren lassen.
*Fertig, wenn:* "Play" führt nach echtem Microsoft-Login zum Download und Start von Vanilla-Minecraft im richtigen Profil.

**Phase 4 — Fabric-Start**
Launcher installiert Fabric Loader für die gewählte Version (über `meta.fabricmc.net`) und startet mit eigenem Mod + gebündelten Optimierungsmods im `mods/`-Ordner (anfangs manuelles Kopieren reicht).
*Fertig, wenn:* Start erfolgt über Fabric, eigener Mod-Log-Eintrag erscheint, Sodium/Lithium sichtbar aktiv (F3 zeigt Sodium-Renderer, bessere Frametimes).

**Phase 5 — Eigene Features, leicht → schwer**
Als unabhängige, einzeln umschaltbare Meilensteine:
- **5a. Koordinaten + Kompass (HUD)** — am einfachsten, reine Fabric-API (`HudRenderCallback`), keine Mixins.
- **5b. Material-Zähler oben rechts** — leicht–moderat, Inventory-Zugriff + HUD-Render, erst hartcodiert für ein Item, dann verallgemeinern.
- **5c. Inventar-Schnellsortierung** — moderat, aber die trickreichste der "leichten" Aufgaben: muss über denselben Slot-Klick-/`ScreenHandler`-Mechanismus wie Vanilla-Drag&Drop laufen, sonst Desync im Multiplayer/Survival — hier bewusst Zeit für Netzwerk-Korrektheit einplanen, nicht nur für die Sortierlogik.
- **5d. F3/Shift+F3-Verbesserungen** — moderat–schwer, mixt direkt in die Vanilla-Debug-HUD-Klasse, die sich zwischen Versionen überdurchschnittlich oft ändert — Wartungsaufwand bei Versionswechseln einplanen.
- **5e. Eigenes Ingame-Menü** — moderat, eigene `Screen`-Subklasse, kein Mixin nötig (außer bei Eingriff ins Vanilla-Pause-Menü selbst).
- **5f. 3D-/verbundene Texturen (Glas etc.)** — am schwersten, bewusst als Stretch-Goal zuletzt: greift tief in die Block-Rendering-Pipeline ein; existierende CTM-Implementierungen als Vorbild/Abhängigkeit recherchieren statt komplett neu erfinden.

Zweiter Durchgang, ergänzt aus `Ideen_für_den_client.md` (persönliche Ideen, die beim ersten Durchgang noch nicht berücksichtigt waren), wieder leicht → schwer:
- **5g. FPS-Anzeige (HUD)** — trivial, eigenes `HudElement` analog zu 5a, keine Mixins.
- **5h. Zoom (gehaltene Taste reduziert FOV)** — trivial, reine Keybind-/FOV-Logik über die Fabric-API, kein Mixin nötig.
- **5i. Custom Crosshair mit Farbauswahl** — leicht, HUD-Rendering + eigener Options-Screen wie bei den bestehenden Features.
- **5j. Fullbright + Lightlevel-Overlay** — leicht–moderat, baut auf der in 5d schon vorhandenen Lichtlevel-Logik auf, diesmal als dauerhaftes Overlay statt F3-Zeile.
- **5k. Shulkerbox-Inhaltsanzeige per Taste** — moderat, Item-Components/NBT lesen und als eigenes Overlay rendern, rein lokal, kein Netzwerk-Risiko.
- **5l. Anpassbare Hitbox-Farbe** — moderat, Mixin in den Vanilla-Hitbox-Debug-Renderer nötig, vom Umfang vergleichbar mit 5d.
- **5m. Keystrokes-Anzeige** (mehr als WASD/Maus/Shift/Space) — moderat, HUD + Key-Input-Polling.
- **5n. Screenshot-Toast mit "Open"/"Copy"-Buttons** — moderat, zuerst prüfen ob Fabric einen Hook für "Screenshot gespeichert" bietet oder ob ein Mixin in `Screenshot`/`ScreenshotRecorder` nötig ist.
- **5o. Itemphysics** — moderat–schwer, nach 5f-Muster einen bestehenden Mod (z.B. "Item Physic") als Abhängigkeit bündeln statt selbst nachzubauen.
- **5p. Echte 3D-Blockmodelle** — kein Mod-Code nötig (reine Vanilla-Resourcepack-Funktionalität, Sodium rendert Custom-Block-Modelle mit zusätzlicher Geometrie nativ); stattdessen ein passendes Resourcepack recherchieren und als eigene, unveränderte Datei bündeln (nicht in den Mod-Jar einbetten — bewusst getrennt gehalten, siehe Lizenz-Hinweis unten).
- **5q. Anpassbare Blockumriss-Farbe** — leicht, direkte Erweiterung von 5l (gleiches Mixin-Muster, gleicher `ColorPickerPanel`), diesmal für den Vanilla-Block-Auswahl-Umriss statt der F3+B-Hitbox.
- **5r. Dunkles Inventar** — ursprünglich per Mixin-Overlay versucht (Panel per `AbstractContainerScreen#renderBackground` abdunkeln), nach mehreren Live-Test-Runden (Kasten-Artefakte an Sprite-Ecken, verdunkelte Icons, dunkel-auf-dunkel-Text) auf Nutzerwunsch komplett verworfen und stattdessen wie 5f/5p gelöst: ein fertiges "Dark Inventory"-Resourcepack recherchiert und gebündelt (**[Default Dark Mode](https://github.com/nebuIr/Default-Dark-Mode)** von nebulr) statt die Texturen zur Laufzeit selbst umzufärben.
- **5s. 3D-Skin-Layer für alle Spieler** — kein Mod-Code nötig, wie schon bei 5p: die zweite Skin-Ebene (Hut, Jacke, Ärmel, Hose) wird vanilla nur als flache, leicht nach außen versetzte 2D-Ebene gerendert statt echter Geometrie mit Tiefe. **[3D Skin Layers](https://modrinth.com/mod/3dskinlayers)** (tr7zw) ist dafür der de-facto Standard-Mod (65+ Mio. Downloads), bestätigt Sodium-kompatibel (eigener Workaround im Mod selbst), rein clientseitig, gilt für jeden Spieler mit sichtbarer zweiter Skin-Ebene, nicht nur den eigenen Charakter.

*Fertig, wenn:* jedes Feature stabil läuft, einzeln konfigurierbar ist und sowohl im Dev-Client als auch in einer über den Launcher gestarteten Instanz funktioniert.

**Phase 6 — Launcher-UX: Versionsauswahl & Mod-Verwaltung**
Versions-Picker (Release/Snapshot, aus Mojang-Manifest + Fabric-Support-Daten), persistente Instanz-Konfiguration (z.B. `electron-store` oder kleine SQLite), Update-Check gegen ein selbst gehostetes Manifest (z.B. JSON in einem GitHub-Repo). Mods-Screen mit zwei Ebenen: (a) die gebündelten Mods (eigener Client-Mod, Sodium, Lithium) an/ausschalten, (b) **beliebige zusätzliche Fabric-Mods laden** — technisch unaufwändig, da Fabric-Mods einfache Jar-Dateien sind, die Fabric Loader aus dem `mods/`-Ordner der Instanz einliest: der Launcher muss im Kern nur diesen Ordner verwalten (Jar per Drag&Drop hinzufügen, einzeln aktivieren/deaktivieren z.B. über Umbenennen/Unterordner-Konvention, entfernen) plus eine simple Prüfung, ob die passende Fabric-API-Version vorhanden ist. Eine spätere, komfortablere Ausbaustufe wäre eine Mod-Suche direkt im Launcher über die öffentliche Modrinth-API — kein Muss für die erste Version.
*Fertig, wenn:* neue Instanz per Dropdown erstellbar, gebündelte Mods umschaltbar, zusätzliche eigene Fabric-Mod-Jars hinzufügbar/entfernbar, Start ohne manuelle Dateioperationen im Dateisystem.

**Phase 7 — Skin- & Cape-Editor**
Reihenfolge wichtig: (1) aktuellen Skin/Cape laden (`GET minecraft/profile`) und **Upload eines vorhandenen PNGs** zuerst umsetzen (erfordert schon die volle API-Anbindung, sofort nützlich); (2) kuratiertes Auswahlfenster/Galerie danach; (3) echter Pixel-Editor zuletzt (eigenständiges Feature, blockiert nichts anderes). Für die 3D-Vorschau eine bestehende Skin-Viewer-Bibliothek nutzen statt selbst zu rendern.
*Hinweis:* Upload-Endpoint ist multipart/form-data; laut Community-Berichten kann übermäßiges Fehler-Retry zu temporären Account-Sperren führen — Debounce/Fehlerbehandlung einplanen.
*Fertig, wenn:* PNG-Auswahl aktualisiert den echten Minecraft-Profil-Skin, sichtbar beim nächsten Spielstart.

**Phase 7b — Custom Cosmetic Capes (eigener Punkt, nicht Teil von Phase 7)** 🔧 gebaut, Live-Test offen (2026-09-24)
Phase 7 deckt nur Mojangs eigenes Skin/Cape-System ab (Anzeige + Skin-Upload) - Capes werden dort nur angezeigt, nie verwaltet. Eigener Wunsch: echte, hochauflösende Custom-Capes wie bei Lunar/Badlion, direkt im Launcher hochladbar, sichtbar für andere Spieler mit passendem Mod. Rendering-Seite steht: das gebündelte, bereits existierende **[Cape Provider](https://modrinth.com/mod/cape-provider)**-Mod (LGPL-2.1-or-later) übernimmt das per HTTP-GET gegen eine `fabric.mod.json`-Konfiguration - kein eigener Mixin-Code. Upload-seitiger Code (Launcher-UI, IPC, PNG-Validierung) ist ebenfalls fertig gebaut, `npm run typecheck`/`npm run build` grün - siehe `Aktuelle_Phase.md`s "Custom Capes"-Abschnitt.
**Umsetzung (2026-09-24):** Hosting auf dem eigenen Server des Bekannten (Debian, Apache, `nxlc.de`) statt Backblaze B2. Aktive Capes liegen öffentlich unter `https://nxlc.de/tntcapes/<uuid>.png` (genau die Adresse, die Cape Provider abfragt); Hochladen/Entfernen über einen kleinen Node-Dienst (`backend/`, hinter `https://nxlc.de/app/`), der das Minecraft-Token des Spielers bei Mojang prüft und nur ins eigene Cape schreiben lässt - kein Geheimschlüssel mehr im Launcher. Regeln: PNG 2:1 von 64x32 bis 2048x1024, höchstens 5 MB. Die Cape-Sammlung bleibt lokal auf dem PC (wie die Skins), nur das aktive Cape liegt auf dem Server. Offen: echter Upload-Test mit einem Account und Prüfung, dass HD-Capes im Spiel unverzerrt angezeigt werden. Phase 8 (Freunde) kann später denselben Server/Dienst nutzen.
*Fertig, wenn:* ein eigenes Cape-PNG im Launcher hochgeladen werden kann und bei einem zweiten Spieler mit installiertem Cape-Provider-Mod korrekt (nicht verzerrt) sichtbar ist.

**Phase 8 — Freunde-/Social-System (bewusst später, optional, eigenes Mini-Projekt)** — siehe Phase 7b oben: beide zusammen ans Ende geschoben, gleicher Infrastruktur-Bedarf
Mojang/Microsoft bieten **keine** Freundes-API — das bedeutet einen eigenen Backend-Service zu entwerfen und zu hosten. Minimal-Architektur: kleines Node/TS-Backend (gleiche Sprache wie der Launcher), Accounts über Minecraft-UUID verknüpft und **serverseitig verifiziert** (nie eine client-behauptete UUID blind vertrauen), Datenbank (SQLite oder Free-Tier Postgres wie Supabase/Railway), Friend-Request/Accept/Remove + WebSocket-Presence, Freunde-UI im Launcher.
*Fertig, wenn:* zwei Testaccounts sich hinzufügen und in nahezu Echtzeit Online/Offline- bzw. "spielt auf X"-Status sehen.

**Phase 8b — Zusammen spielen per Einladung (geplant 2026-09-25, baut auf Phase 8 auf)**
Eigener Wunsch: in die Einzelspieler-Welt eines Freundes kommt man **nur mit Einladung**, nicht über eine offene Adresse.
- **Ablauf:** Host klickt im Pausenmenü "Freunde einladen" → wählt Freunde → Einladung läuft über unser Backend (`nxlc.de`, derselbe Dienst wie Capes/Freunde) an genau diesen Freund → der bekommt eine Benachrichtigung im Launcher bzw. als Toast im Spiel mit Annehmen/Ablehnen → bei Annehmen verbindet Launcher/Mod automatisch, ohne dass eine Adresse abgetippt wird.
- **Erreichbarkeit der Welt:** über das gebündelte **e4mc** (MIT, Modrinth, gibt es für 1.21.11 und 26.1.2) - "Im LAN öffnen" tunnelt die Welt über e4mcs Relay-Server und vergibt eine Adresse `xyz.e4mc.link`. Später optional ein eigenes Relay auf `nxlc.de` (e4mc hat dafür schon Einstellungen `brokerUrl`/`relayHost`) - aber nur mit OK des Server-Besitzers, da dann der gesamte Spielverkehr über seine Leitung läuft.
- **Nur Eingeladene kommen rein, zwei Schichten:** (1) die e4mc-Adresse wird nirgends angezeigt, das Backend gibt sie nur an Eingeladene weiter; (2) die eigentliche Sperre: unsere Mod prüft auf dem Host bei jedem Beitritt die UUID und wirft jeden raus, der nicht eingeladen ist ("Du wurdest nicht eingeladen"). Die UUID ist nicht fälschbar, weil eine geöffnete Welt im Online-Modus läuft (Mojang prüft die Identität).
- **Einladungen** laufen ab (z.B. nach 10 Minuten oder wenn der Host die Welt schließt), der Host kann sie zurückziehen und Spieler rauswerfen.
- **Server statt Welt:** spielt der Host auf einem öffentlichen Server, ist eine Einladung nur "komm auch auf server.de" (Start per `--quickPlayMultiplayer` bzw. direkt verbinden) - ohne Sperre, da entscheidet der Server.
- **e4mc-Prüfung (2026-09-25, Version 6.2.1 für 26.1.2):** e4mc speichert die Adresse **nirgends** abrufbar - sie steht nur im Log und in einer Chatnachricht (Übersetzungsschlüssel `text.e4mc_minecraft.domainAssigned`, Adresse als Argument bzw. im Kopieren-Klick). Plan: unsere Mod hängt sich an Vanillas `ChatComponent.addMessage`, erkennt diese Nachricht am Schlüssel, liest die Adresse heraus und blendet die Nachricht aus (damit die Adresse geheim bleibt). Hängt nur an Vanilla + einem Übersetzungsschlüssel, kein Mixin in e4mcs Code - ändert e4mc den Schlüssel, geht nur das Einladen nicht mehr (kein Absturz). `text.e4mc_minecraft.closeServer` zeigt an, wann die Welt nicht mehr erreichbar ist. e4mcs "PoisonPill"-Warnung prüft nur die Download-Quelle der Jar unter Windows (Modrinth ist erlaubt) - fürs Bündeln kein Problem. e4mc bringt selbst Whitelist-/Ban-Befehle für LAN-Welten zurück, unsere eigene UUID-Prüfung ist davon unabhängig.
*Fertig, wenn:* Account A lädt Account B in seine Welt ein, B kommt per Klick rein, ein nicht eingeladener Account C wird trotz bekannter Adresse abgewiesen.

**Phase 9 — Packaging, Distribution, Auto-Update**
Launcher: `electron-builder` (NSIS/dmg/AppImage) + `electron-updater` gegen z.B. GitHub Releases. Mod-Updates: Launcher prüft ein selbst kontrolliertes Manifest und lädt Mod-Jar + gepinnte Sodium/Lithium-Versionen pro Minecraft-Version neu — im Prinzip ein eigener kleiner "App Store" für Jars. Unsignierte Windows-Installer lösen SmartScreen-Warnungen aus — bei Bedarf einplanen.
*Fertig, wenn:* ein Freund einen einzelnen Installer herunterlädt, sich einloggt und ohne manuelle Dateioperationen eine funktionierende, gemoddete Instanz bekommt.

## Lizenz/rechtliche Punkte (vor öffentlichem Release prüfen, kein Blocker fürs Entwickeln)

- **Sodium**: PolyForm Shield License 1.0.0 — Bündeln in einem vollwertigen Client ist ausdrücklich erlaubt (ohne Credit/Erlaubnis), außer man baut ein direkt konkurrierendes Rendering-Mod-Produkt. Vor Public Release selbst den Lizenztext lesen.
- **Lithium**: LGPL-3.0-only — unmodifiziertes Bündeln unproblematisch (Notices mitliefern), eigener Mod muss nicht offengelegt werden; nur eigene Änderungen an Lithium selbst müssten LGPL bleiben.
- **Default Dark Mode (Resourcepack, Phase 5r)**: CC-BY-NC-SA-4.0 (Creative Commons, nicht-kommerziell, Weitergabe unter gleichen Bedingungen, Namensnennung) — anders als die bisherigen Code-Lizenzen (PolyForm/LGPL) die erste Creative-Commons-Lizenz im Projekt. Unverändert als separate Datei in `launcher/resourcepacks-bundle/` ausgeliefert, nicht in den Mod-Jar eingebettet. Nicht-kommerziell passt zum aktuellen Hobbyprojekt-Status; **vor einer eventuellen kommerziellen/monetarisierten Veröffentlichung nochmal explizit gegenprüfen** (NC-Klausel), und Namensnennung (nebulr, github.com/nebuIr/Default-Dark-Mode) in den geplanten "Third-Party Licenses/Credits"-Screen aufnehmen.
- **Bushy Vegetation (Resourcepack, Phase 5p-Nachtrag)**: BSD-3-Clause, ebenfalls "poqbox". Deckt Ranken (Overworld-Ranken **und** bestätigt Weeping/Twisting Vines im Nether, per Screenshot verifiziert) plus als Nebeneffekt Gras/Farne/Blumen/Süßbeerenbusch bushig ab.
- **3D Bushy Bushie (Resourcepack, Phase 5p-Nachtrag)**: Apache-2.0 (ebenfalls sehr permissiv). Deckt den neuen "Bush"-Block sowie Süßbeeren-/Firefly-Bush ab — einzige gefundene Option speziell für den "Bush"-Block, noch recht neu/wenig verbreitet (92 Downloads), Qualität also weniger breit erprobt als die anderen Bundles.
- **Mushrooms Plus (Resourcepack, Phase 5p-Nachtrag)**: MIT.
- **Vanilla Spinning Stonecutter (3D) (Resourcepack, Phase 5p-Nachtrag)**: MIT. Ergänzt eine echte Dreh-Animation (per reinem Vanilla-Mechanismus, kein Mod nötig).
- **Vanilla Tweaks-Sammelpaket (Resourcepack, Phase 5p-Nachtrag)**: eigene, individuell auf vanillatweaks.net zusammengestellte Auswahl (Amethyst, Sculk Vein, Glow Lichen, u.a.), kein fester Modrinth-Release. Laut vanillatweaks.net/terms/: Bündeln erlaubt, aber **nicht unmodifiziert** (die Seite verlangt explizit "modify/enhance" statt reiner 1:1-Weitergabe), Namensnennung Pflicht ("Vanilla Tweaks: https://vanillatweaks.net/", auf "allen Haupt-Veröffentlichungsplattformen" plus idealerweise eigene `credits.txt`), **keine Monetarisierung** der Weitergabe (kein Spenden-/Bezahl-Zugang). Für den aktuellen, nicht-öffentlichen Hobby-Stand unkritisch; **vor einem öffentlichen Release nochmal genau gegenprüfen** - anders als die bisherigen Bundles hier ausdrücklich die Frage klären, ob eine reine Nutzerauswahl schon als "modifiziert genug" zählt, oder ob eine noch eigenständigere Anpassung nötig wäre. (Blocky 3D Pointed Dripstone, das ursprünglich für Dripstone gebündelt war, danach wieder entfernt - vom Vanilla-Tweaks-Sammelpaket vollständig abgedeckt, exakt gleicher Modellumfang.)
- **3D Skin Layers (Mod, Phase 5s)**: eigene "tr7zw Protective License" (LicenseRef, kein SPDX-Standard) - im Kern MIT-artig (Namensnennung Pflicht, Copyright-Hinweis muss erhalten bleiben), zusätzlich aber eine explizite Nicht-kommerziell-Klausel ("darf nicht für kommerziellen Vorteil oder Bezahlung genutzt werden") - ähnlich streng wie die Vanilla-Tweaks-Klausel oben, aber auf einen echten Mod statt ein Resourcepack bezogen. Unverändert als Jar in `launcher/mods-bundle/` (gleiches Muster wie Sodium/Lithium/Continuity), **vor einer eventuellen kommerziellen Veröffentlichung nochmal explizit gegenprüfen**.
- Jedes weitere gebündelte Mod einzeln prüfen — "Open Source" heißt nicht automatisch "frei bündelbar" (MIT/CC0/GPL/LGPL/All-Rights-Reserved sind alle im Ökosystem vertreten).
- **"Third-Party Licenses/Credits"-Screen** ✅ umgesetzt (vorgezogen, ursprünglich für später geplant) - `mod/src/main/java/com/tntsallin1client/menu/CreditsScreen.java` (Mod-Menü, per `ContainerObjectSelectionList`-Zeile) und `launcher/src/renderer/src/screens/CreditsScreen.tsx` (Launcher, hinter einem "Credits"-Button im Play-Screen-Header) - beide listen dieselben zwölf Einträge (Fabric Loader/API, Sodium, Lithium, Continuity, alle Phase-5p/5r-Resourcepacks), jeder Eintrag öffnet den Link (Mod: `ConfirmLinkScreen`, Launcher: neuer `shell:open-external`-IPC-Kanal). Beide Listen müssen von Hand synchron gehalten werden, wenn ein Bundle dazukommt/wegfällt - siehe Kommentar in beiden Dateien.
- **Mojang Usage Guidelines** (minecraft.net/en-us/usage-guidelines): keine Mojang-eigenen Spieldateien redistributieren (jeder Nutzer lädt über sein eigenes Entitlement selbst — wie beim Vanilla-Launcher), keine Andeutung offizieller Mojang/Microsoft-Unterstützung, bei Monetarisierung auf "kein unfairer Gameplay-Vorteil" achten — Guidelines ändern sich, zum gegebenen Zeitpunkt erneut selbst lesen.
- Server-Kompatibilität (z.B. Hypixel): rein performance-/kosmetische Client-Bundles sind laut deren Policy erlaubt (Präzedenzfall Lunar/Badlion) — alle bisherigen Feature-Ideen liegen klar in dieser Spur.

## Lernressourcen

- **Fabric-Doku**: docs.fabricmc.net, Fabric Wiki (wiki.fabricmc.net, gute Mixin-Beispiele), Template Mod Generator (fabricmc.net/develop/template), `FabricMC/fabric-example-mod`.
- **Fabric Discord** (offizieller Link nur über fabricmc.net — es gibt Fake-Server).
- **Mixin**: `SpongePowered/Mixin`-GitHub-Wiki (kanonisch), `mixin-wiki.readthedocs.io` (community, einsteigerfreundlicher).
- **Auth/Protokoll-Referenz**: wiki.vg wurde am 30.11.2024 abgeschaltet, Inhalte leben in **minecraft.wiki** weiter (Community, nicht offiziell, aber aktuell beste Quelle) — Seiten "Microsoft Authentication" und "Mojang API".
- **Referenz-Launcher zum Reinlesen**: **Voxelum/x-minecraft-launcher (XMCL)** — aktiv gepflegt, Electron+Vue+TS, mit wiederverwendbaren `@xmcl/*`-Paketen für Auth/Install/Launch — wahrscheinlich die beste "echten, aktuellen Code lesen"-Referenz für genau diesen Stack. **Prism Launcher** (C++/Qt, nicht Electron) für die Logik (Auth-Flow, Instanz-Modell) unabhängig vom UI-Framework.
- **Sprachwahl Mod**: mit reinem **Java** starten, nicht Kotlin — fast alle Fabric-Tutorials sind Java; `fabric-language-kotlin` erlaubt späteren Umstieg, falls gewünscht.
- **Electron**: electron.build-Doku (`electron-builder`/`electron-updater` als aktueller Standard).

## Risiken / offene Punkte

1. Azure-App-Freischaltungsprozess/-dauer ist nicht zuverlässig dokumentiert (Formular `aka.ms/mce-reviewappid` vs. Hinweise auf ID@Xbox-Programm in Community-Quellen) — früh anstoßen, nicht darauf blockieren.
2. Mixin-/Mapping-Bruch bei jedem Minecraft-Versionssprung (besonders Debug-HUD, Phase 5d) — bewusst lange auf einer Version bleiben ist eine legitime Hobby-Projekt-Entscheidung.
3. Sodium/Lithium-Updates müssen aktiv mitverfolgt und neu gepinnt werden.
4. Lizenz-Auslegung bei Sodiums "konkurriert nicht mit"-Klausel ist ein Ermessensspielraum, nicht bright-line — im Zweifel im Fabric-/CaffeineMC-Umfeld nachfragen.
5. Electron+JVM-Prozessverwaltung (Crash-Erkennung, OOM, Log-Handling, Neustart nach Mod-Änderung) braucht echtes Design, nicht nur `spawn()`.
6. **Nicht verifizierbar recherchiert**: Die aktuelle Minecraft-Versionsnummerierung war zum Recherchezeitpunkt uneindeutig (klassisches `1.21.x`-Schema vs. Hinweise auf ein neueres jahresbasiertes Schema wie `26.1`) — beim Start von Phase 1 direkt auf fabricmc.net die aktuell unterstützte Stable-Version nachsehen statt Versionsnummern aus dieser Planung zu übernehmen.

## Nächste konkrete Schritte

1. `git init` im Projektverzeichnis.
2. JDK 21, Node.js LTS, Git, IntelliJ IDEA Community installieren.
3. Azure-AD-App registrieren + Freischaltungsantrag (`aka.ms/mce-reviewappid`) abschicken.
4. Parallel: Fabric Template Mod Generator nutzen, `runClient` zum Laufen bringen (Phase 1).



persöhnliche liste was fehlt:
- ingame mods auf andere versionen porten (wichtig sind vor allem die vollversionen)
- kann man offline verwenden? → [check] gebaut und live bestätigt (2026-09-25). Beim ersten Test fror das Spiel am Ladebalken ein: die Discord-Anzeige wartete im Spiel-Takt auf Discords Antwort, die offline nie kommt - Discord-IPC läuft jetzt auf einem eigenen Thread und ist bei Offline-Starts ganz aus. ohne Internet startet der Launcher mit dem gespeicherten Profil statt dem Login (nur bei echtem Netzwerkfehler, nicht wenn Microsoft/Mojang die Anmeldung ablehnt; nach 15 s ohne Antwort gilt es als offline), zeigt einen Offline-Hinweis mit "Erneut verbinden", und Instanzen laden unabhängig von der Online-Versionsliste. Start-Metadaten (Mojang-Versionsliste/-Details, Asset-Index, Fabric-Meta, Java-Manifest) werden bei jedem Online-Abruf in `meta-cache/` gespeichert und offline direkt daraus gelesen (`offline.ts`); Spieldateien überspringt der Downloader ohnehin, wenn sie schon da sind. Eigener Skin und eigenes Cape (unseres vom Cape-Server, sonst das aktive Mojang-Cape) werden bei jedem Online-Start lokal gesichert und offline von der Mod für den eigenen Spieler eingesetzt (`offlineProfile.ts`, `OfflineProfile.java` + `SkinManagerMixin`, alte 64x32-Skins werden wie in Vanilla umgewandelt). Ursprüngliche Idee: Idee: ohne Internet mit dem zuletzt gespeicherten Profil (Name/UUID/Skin) starten statt Login-Screen, Online-Abfragen beim Start (Mojang-Versionsliste, Fabric-Meta, Java-Runtime, Modrinth) überspringen wenn schon alles installiert ist. Geht dann nur Einzelspieler/LAN, keine Online-Mode-Server; einmal online eingeloggt muss man gewesen sein (kein Offline-Modus ohne gekauften Account). Eigenes Cape offline aus der lokalen Cape-Sammlung anzeigen (online kommt es vom Cape-Server) - braucht dafür eine kleine Ausweichlösung in unserer Mod
- start der 1.16.5 klappt nicht → [check] Fix gebaut (2026-09-24), live bestätigt (2026-09-25), 1.16.5 startet: Versionen 1.14–1.18.2 liefern ihre LWJGL-DLLs als natives-Pakete, die der Launcher jetzt herunterlädt und vor jedem Start in `natives/` der Instanz entpackt (`nativesExtractor.ts`)
- Idee für später: Versionen vor 1.14 (z.B. 1.8.9 für PvP) unterstützen. Aktuell nicht auswählbar, weil der Launcher alles über Fabric startet und Fabric erst ab 1.14 geht. Nötig wäre: Start ohne Mod-Loader oder über Legacy Fabric (1.8.9–1.13.2), Unterstützung für das alte Startformat `minecraftArguments` (vor 1.13); LWJGL-2-DLLs entpackt der natives-Fix oben schon mit. Unsere Mod-Features gäbe es dort erst nach einer Portierung
- welten uploaden können → [check] gebaut und live bestätigt (2026-09-25): im Welten-Bildschirm "Weltordner hochladen…" und "Welt-ZIP hochladen…" (Mehrfachauswahl; ein ganzer saves-Ordner übernimmt alle Welten darin; Dialog startet im saves-Ordner des normalen Minecraft-Launchers). Doppelte Namen bekommen "(2)", `session.lock` wird nicht mitkopiert, ZIPs werden per Prüfsumme geprüft und ein abgebrochener Import wieder entfernt (`worldImport.ts`). Dazu "Entfernen" pro Welt mit Bestätigung - verschiebt in den Windows-Papierkorb statt endgültig zu löschen 