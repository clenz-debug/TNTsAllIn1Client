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
3. **Eigenen Mod-Jar veröffentlichen:**
   ```
   gh release upload <tag> mod/<version>/build/libs/tntsallin1client-<version>.jar
   ```
4. **Resourcepacks veröffentlichen:** jedes neue oder geänderte Resourcepack-Zip ebenfalls als
   Asset an dasselbe (oder ein neues) GitHub Release hängen. Ein unverändertes Resourcepack braucht
   **keinen** erneuten Upload - der neue Versions-Eintrag kann einfach denselben, schon
   vorhandenen Release-Asset-Link aus einer älteren Version wiederverwenden.
5. **SHA-1 je Datei berechnen** (eigener Mod-Jar + jedes neu hochgeladene Resourcepack):
   ```powershell
   Get-FileHash <datei> -Algorithm SHA1
   ```
   Der Hex-Digest wird `ownMod.sha1` bzw. das jeweilige `bundledResourcepacks[].sha1`.
6. **Neuen `versions.<version>`-Eintrag vervollständigen** in `mod-bundle-manifest.json`:
   `ownMod` (aus Schritt 3+5), `bundledMods` (aus Modrinth-Recherche, wie in Abschnitt A),
   `bundledResourcepacks` (aus Schritt 4+5, `name`/`version`/`url`/`sha1` je Pack).
7. **Optional, nur falls diese Version die neue primäre "Seed"-Version werden soll** (künftige
   Installer bündeln sie direkt statt sie erst bei Bedarf nachzuladen): `SEED_BUNDLE_MINECRAFT_VERSION`
   in `launcher/src/shared/types.ts` **und** die drei `extraResources`-Pfade in
   `launcher/electron-builder.yml` mit umstellen, dabei den lokalen
   `launcher/mods-bundle/<version>/`/`resourcepacks-bundle/<version>/`/`mod/<version>/build/libs/`-Inhalt vor
   dem nächsten `electron-builder`-Lauf sicherstellen. Sonst entfällt dieser Schritt komplett -
   Nutzer laden die neue Version automatisch beim ersten "Play" nach, ganz ohne neuen
   Launcher-Installer.
8. Isolierter Commit (nur `mod-bundle-manifest.json`, ggf. + `types.ts`/`electron-builder.yml` bei
   Schritt 7), pushen.
