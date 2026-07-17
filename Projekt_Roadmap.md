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
├── mod/            Fabric-Client-Mod: Java + Gradle + Fabric Loom
│   ├── build.gradle.kts / gradle.properties   (Pins: MC-Version, Yarn-Mappings, Loader, Fabric API)
│   └── src/main/java/<pkg>/          <ClientName>Mod.java, mixin/, hud/, inventory/, menu/
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
*Fertig, wenn:* jedes Feature stabil läuft, einzeln konfigurierbar ist und sowohl im Dev-Client als auch in einer über den Launcher gestarteten Instanz funktioniert.

**Phase 6 — Launcher-UX: Versionsauswahl & Mod-Verwaltung**
Versions-Picker (Release/Snapshot, aus Mojang-Manifest + Fabric-Support-Daten), persistente Instanz-Konfiguration (z.B. `electron-store` oder kleine SQLite), Update-Check gegen ein selbst gehostetes Manifest (z.B. JSON in einem GitHub-Repo). Mods-Screen mit zwei Ebenen: (a) die gebündelten Mods (eigener Client-Mod, Sodium, Lithium) an/ausschalten, (b) **beliebige zusätzliche Fabric-Mods laden** — technisch unaufwändig, da Fabric-Mods einfache Jar-Dateien sind, die Fabric Loader aus dem `mods/`-Ordner der Instanz einliest: der Launcher muss im Kern nur diesen Ordner verwalten (Jar per Drag&Drop hinzufügen, einzeln aktivieren/deaktivieren z.B. über Umbenennen/Unterordner-Konvention, entfernen) plus eine simple Prüfung, ob die passende Fabric-API-Version vorhanden ist. Eine spätere, komfortablere Ausbaustufe wäre eine Mod-Suche direkt im Launcher über die öffentliche Modrinth-API — kein Muss für die erste Version.
*Fertig, wenn:* neue Instanz per Dropdown erstellbar, gebündelte Mods umschaltbar, zusätzliche eigene Fabric-Mod-Jars hinzufügbar/entfernbar, Start ohne manuelle Dateioperationen im Dateisystem.

**Phase 7 — Skin- & Cape-Editor**
Reihenfolge wichtig: (1) aktuellen Skin/Cape laden (`GET minecraft/profile`) und **Upload eines vorhandenen PNGs** zuerst umsetzen (erfordert schon die volle API-Anbindung, sofort nützlich); (2) kuratiertes Auswahlfenster/Galerie danach; (3) echter Pixel-Editor zuletzt (eigenständiges Feature, blockiert nichts anderes). Für die 3D-Vorschau eine bestehende Skin-Viewer-Bibliothek nutzen statt selbst zu rendern.
*Hinweis:* Upload-Endpoint ist multipart/form-data; laut Community-Berichten kann übermäßiges Fehler-Retry zu temporären Account-Sperren führen — Debounce/Fehlerbehandlung einplanen.
*Fertig, wenn:* PNG-Auswahl aktualisiert den echten Minecraft-Profil-Skin, sichtbar beim nächsten Spielstart.

**Phase 8 — Freunde-/Social-System (bewusst später, optional, eigenes Mini-Projekt)**
Mojang/Microsoft bieten **keine** Freundes-API — das bedeutet einen eigenen Backend-Service zu entwerfen und zu hosten. Minimal-Architektur: kleines Node/TS-Backend (gleiche Sprache wie der Launcher), Accounts über Minecraft-UUID verknüpft und **serverseitig verifiziert** (nie eine client-behauptete UUID blind vertrauen), Datenbank (SQLite oder Free-Tier Postgres wie Supabase/Railway), Friend-Request/Accept/Remove + WebSocket-Presence, Freunde-UI im Launcher.
*Fertig, wenn:* zwei Testaccounts sich hinzufügen und in nahezu Echtzeit Online/Offline- bzw. "spielt auf X"-Status sehen.

**Phase 9 — Packaging, Distribution, Auto-Update**
Launcher: `electron-builder` (NSIS/dmg/AppImage) + `electron-updater` gegen z.B. GitHub Releases. Mod-Updates: Launcher prüft ein selbst kontrolliertes Manifest und lädt Mod-Jar + gepinnte Sodium/Lithium-Versionen pro Minecraft-Version neu — im Prinzip ein eigener kleiner "App Store" für Jars. Unsignierte Windows-Installer lösen SmartScreen-Warnungen aus — bei Bedarf einplanen.
*Fertig, wenn:* ein Freund einen einzelnen Installer herunterlädt, sich einloggt und ohne manuelle Dateioperationen eine funktionierende, gemoddete Instanz bekommt.

## Lizenz/rechtliche Punkte (vor öffentlichem Release prüfen, kein Blocker fürs Entwickeln)

- **Sodium**: PolyForm Shield License 1.0.0 — Bündeln in einem vollwertigen Client ist ausdrücklich erlaubt (ohne Credit/Erlaubnis), außer man baut ein direkt konkurrierendes Rendering-Mod-Produkt. Vor Public Release selbst den Lizenztext lesen.
- **Lithium**: LGPL-3.0-only — unmodifiziertes Bündeln unproblematisch (Notices mitliefern), eigener Mod muss nicht offengelegt werden; nur eigene Änderungen an Lithium selbst müssten LGPL bleiben.
- Jedes weitere gebündelte Mod einzeln prüfen — "Open Source" heißt nicht automatisch "frei bündelbar" (MIT/CC0/GPL/LGPL/All-Rights-Reserved sind alle im Ökosystem vertreten).
- Ein "Third-Party Licenses/Credits"-Screen in Mod und Launcher einplanen.
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
