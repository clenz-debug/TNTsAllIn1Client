# Mod-Bundle-Release-Runbook

Manueller Ablauf für Änderungen an `mod-bundle-manifest.json`. Bewusst kein CI/Automatisierungs-
Skript für den Bau-/Veröffentlichungs-Schritt selbst (siehe `Projekt_Roadmap.md`) - nur das
*Konsumieren* des Manifests durch den Launcher ist automatisiert (`main/launch/modBundleUpdater.ts`),
das *Befüllen* bleibt Handarbeit.

**`mod/`-Struktur:** ein eigenständiges Gradle/Loom-Projekt pro unterstützter Minecraft-Version unter
`mod/<version>/` (z.B. `mod/1.21.11/`, `mod/26.1.2/`), nicht ein einzelner Ordner mit wechselnden
Pins. Jede Version bleibt dadurch dauerhaft unabhängig baubar und bearbeitbar, ohne eine andere
Version durch Umstellen von Pins vorübergehend unbaubar zu machen. Ziel: **jede unterstützte
Vollversion bekommt unseren eigenen Mod**, angepasst an das, was in dieser Version technisch möglich
ist (ein Feature, das dort nicht existiert, muss nicht nachgebaut werden). Bei sehr alten
Vollversionen (vor 26.x) wird bewusst nur die stabilste/letzte Unterversion unterstützt (z.B. 1.7.10
statt der ganzen 1.7.x-Reihe), da diese Unterversionen historisch kaum API-Unterschiede hatten.
Versionsunabhängige Bugfixes müssen von Hand in jedem betroffenen `mod/<version>/`-Ordner nachgezogen
werden - bewusste Entscheidung gegen ein Multi-Version-Tool wie Stonecutter, weil neuere Minecraft-
Versionen (seit dem "Drops"-Modell ab 1.21.2, Okt. 2024) auch zwischen benachbarten Versionen oft
große Brüche haben, nicht nur bei einem seltenen Vollversions-Sprung wie früher.

## A) Bestehende Version aktualisieren (z.B. neuer Sodium-Build)

1. Neue Modrinth-Projekt-/Versions-ID nachschlagen (z.B. über `GET /v2/project/<slug>/version`).
2. `mod-bundle-manifest.json`: betroffenen Eintrag unter `versions.<mcVersion>.bundledMods` auf die
   neue `modrinthVersionId` setzen.
3. Isolierter Commit nur dieser einen Datei, pushen. **Kein Launcher-Rebuild nötig** - jeder
   bereits installierte Launcher prüft das Manifest selbst und lädt die neue Version beim nächsten
   Start/Klick auf "Aktualisieren".

## B) Neue Minecraft-Version freischalten

Wichtig: ein neuer `versions.<version>`-Eintrag wird **erst gepusht, wenn wirklich alles fertig
gebaut und getestet ist** - eigener Mod, alle gebündelten Fremd-Mods und alle Resourcepacks
zusammen. Kein Wettrennen zwischen "Mods sind schon da, Resourcepacks fehlen noch".

1. **Neuen `mod/<version>/`-Ordner anlegen:** vom Ordner der bisher ähnlichsten Version kopieren
   (z.B. `mod/26.1.2/` als Basis für einen weiteren 26.x-Drop) statt bei null anzufangen, dann
   `gradle.properties` (`minecraft_version`, `loader_version`, `java_version`, `fabric_api_version`),
   `build.gradle.kts` (die drei `implementation`/`compileOnly("maven.modrinth:...")`-Koordinaten für
   Continuity/3D Skin Layers/Sodium) und bei Bedarf `settings.gradle.kts` (Loom-Plugin-ID -
   `net.fabricmc.fabric-loom-remap` für noch obfuskierte Versionen bis 1.21.11,
   `net.fabricmc.fabric-loom` für 26.x+) auf die neue Version anpassen. `fabric.mod.json`s
   `"minecraft"`-Range braucht **keinen** manuellen Schritt - wird automatisch aus
   `minecraft_version` generiert (`${minecraft_version}`-Templating in `processResources`). Je nach
   Größe des API-Sprungs zur Basisversion kann das reine Config-Anpassung sein oder zusätzlich echte
   Quellcode-Migration brauchen (siehe `Aktuelle_Phase.md`s 26.1.2-Migrationsabschnitt als Vorlage,
   inkl. `javap` gegen den echten Ziel-Jar statt sich auf Doku zu verlassen).
2. **Lokal bauen und testen:** `./gradlew build` **in diesem `mod/<version>/`-Ordner** (mit der zur
   Version passenden JDK, siehe `java_version`), dann im Launcher-Dev-Modus eine Instanz auf der
   neuen Version anlegen, `launcher/mods-bundle/<neueVersion>/` und
   `launcher/resourcepacks-bundle/<neueVersion>/` lokal befüllen, live gegenprüfen. Mixins können
   bei einem Versionssprung brechen (bekanntes, akzeptiertes Risiko, siehe
   `Projekt_Roadmap.md`s Risiken-Abschnitt) - kein Zeitdruck, lieber gründlich als schnell.
3. **Eigenen Mod-Jar veröffentlichen** - als Asset an ein **Launcher-Release** (Abschnitt C) hängen,
   nie an ein eigenes "Bundle-Release": das neueste Release muss immer das mit `latest.yml` sein,
   sonst findet das Auto-Update des Launchers nichts. Name mit Minecraft-Version, damit sich die
   Jars der Versionen nicht überschreiben:
   ```
   Copy-Item mod/<mc>/build/libs/tntsallin1client-<modversion>.jar tntsallin1client-<modversion>-mc<mc>.jar
   gh release upload v<launcherversion> tntsallin1client-<modversion>-mc<mc>.jar
   ```
   Den Jar zwischen SHA-1-Berechnung (Schritt 5), Installer-Bau und Upload **nicht neu bauen** -
   ein neuer Build ändert die Prüfsumme.
4. **Resourcepacks:** zuerst die Lizenz prüfen (`Projekt_Roadmap.md`, "Lizenz/rechtliche Punkte").
   Liegt das Pack auf Modrinth, im Manifest **direkt dessen Modrinth-Download** eintragen statt es
   selbst hochzuladen (URL + SHA-1 per `GET https://api.modrinth.com/v2/version_file/<sha1>` aus
   der lokalen Datei ermitteln) - dann verbreiten nicht wir die Datei weiter. Nur eigene Packs
   selbst als Release-Asset hochladen. Vanilla Tweaks darf nicht unverändert weitergegeben werden
   und ist deshalb nicht im Release (siehe `electron-builder.yml`).
5. **SHA-1 je Datei berechnen** (eigener Mod-Jar + jedes neu hochgeladene Resourcepack):
   ```powershell
   Get-FileHash <datei> -Algorithm SHA1
   ```
   Der Hex-Digest wird `ownMod.sha1` bzw. das jeweilige `bundledResourcepacks[].sha1`.
6. **Neuen `versions.<version>`-Eintrag vervollständigen** in `mod-bundle-manifest.json`:
   `ownMod` (aus Schritt 3+5), `bundledMods` (aus Modrinth-Recherche, wie in Abschnitt A - **immer
   mit Fabric API und e4mc**: ohne Fabric API stürzt eine nur übers Manifest geladene Version ab,
   ohne e4mc gehen keine Welt-Einladungen), `bundledResourcepacks` (aus Schritt 4+5,
   `name`/`version`/`url`/`sha1` je Pack; `name` = Dateiname ohne `.zip`). Die Modrinth-IDs der
   lokal gebündelten Jars am zuverlässigsten per SHA-1 nachschlagen
   (`GET https://api.modrinth.com/v2/version_file/<sha1>`). e4mc **nur zusammen mit `ownMod`**
   eintragen - ohne unsere Mod wäre bei anderen jedes "Im LAN öffnen" öffentlich.
7. **Optional, nur falls diese Version fest in den Installer soll** (künftige Installer bündeln
   sie direkt statt sie erst bei Bedarf nachzuladen): sie zu `SEED_BUNDLE_MINECRAFT_VERSIONS`
   in `launcher/src/shared/types.ts` hinzufügen **und** die drei `extraResources`-Einträge in
   `launcher/electron-builder.yml` für sie ergänzen, dabei den lokalen
   `launcher/mods-bundle/<version>/`/`resourcepacks-bundle/<version>/`/`mod/<version>/build/libs/`-Inhalt vor
   dem nächsten `electron-builder`-Lauf sicherstellen. Sonst entfällt dieser Schritt komplett -
   Nutzer laden die neue Version automatisch beim ersten "Play" nach, ganz ohne neuen
   Launcher-Installer.
8. Isolierter Commit (nur `mod-bundle-manifest.json`, ggf. + `types.ts`/`electron-builder.yml` bei
   Schritt 7), pushen.

## C) Launcher-Release (neue Launcher-Version, Auto-Update)

Nur nötig, wenn sich **Launcher-Code** ändert (neue Funktionen, Fehlerbehebungen, oder eine
Minecraft-Version braucht etwas, das der Launcher noch nicht kann). Neue Mod-/Pack-Stände und neue
Minecraft-Versionen laufen über Abschnitt A/B ohne neuen Launcher. Installierte Launcher prüfen beim
Start die GitHub Releases (`electron-updater`, `launcher/src/main/autoUpdate.ts`), laden ein Update im
Hintergrund und bieten "Jetzt neu starten" an - niemand muss etwas von Hand neu installieren, auch
nicht beim Sprung auf 1.0.

**Einmalig:** GitHub CLI installieren (`winget install GitHub.cli`) und `gh auth login` (Browser).

1. `launcher/package.json`: `version` hochsetzen (z.B. `0.1.0` → `0.1.1`).
2. Beide Mods frisch bauen (`gradlew build` in `mod/1.21.11` und `mod/26.1.2`) - der Installer
   bündelt deren `build/libs` (`electron-builder.yml`, `extraResources`).
3. Soll das Release auch neue Mod-Jars ausliefern: SHA-1 berechnen und `mod-bundle-manifest.json`
   **vor** dem Installer-Bau anpassen (Abschnitt B, Schritte 3-6) - der Installer legt eine Kopie des
   Manifests als `seed-manifest.json` bei, damit ein frisch installierter Launcher seine gebündelten
   Stände als aktuell erkennt.
4. Installer bauen und veröffentlichen (im Ordner `launcher/`):
   ```powershell
   $env:GH_TOKEN = gh auth token
   npm run release:win
   ```
   Legt das Release `v<version>` mit Installer, `.blockmap` und `latest.yml` an.
5. Mod-Jars mit `gh release upload v<version> …` an **dasselbe** Release hängen (Abschnitt B, Schritt 3).
6. Erst jetzt `mod-bundle-manifest.json` committen und pushen - vorher zeigten seine Links ins Leere.

**Darf sich nie ändern**, sonst findet ein schon installierter Launcher sein Update nicht mehr und
müsste einmal von Hand neu installiert werden:
- die App-ID `com.tntsallin1client.launcher` (`electron-builder.yml`)
- Besitzer und Name des GitHub-Repos (`clenz-debug/TNTsAllIn1Client`) - vor einem Umzug erst ein
  Übergangs-Update veröffentlichen, das schon auf das neue Ziel zeigt
- die Installer-Art (NSIS)
- der App-Name `tntsallin1client` (`extraMetadata` in `electron-builder.yml`) - er bestimmt den
  Datenordner (`%APPDATA%\tntsallin1client`); ein anderer Name hieße: Login, Einstellungen und
  Instanzen scheinbar weg
- Releases immer als normales Release veröffentlichen (`releaseType: release`), nie als Entwurf oder
  Vorabversion - die sieht das Auto-Update nicht
- falls später signiert wird: ein einmal genutztes Zertifikat nicht gegen eines mit anderem
  Herausgebernamen tauschen

Der Installer ist unsigniert - Windows SmartScreen zeigt beim ersten Start "Unbekannter
Herausgeber" ("Weitere Informationen" → "Trotzdem ausführen").
