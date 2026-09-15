# Mod-Bundle-Release-Runbook

Manueller Ablauf für Änderungen an `mod-bundle-manifest.json`. Bewusst kein CI/Automatisierungs-
Skript für den Bau-/Veröffentlichungs-Schritt selbst (siehe `Projekt_Roadmap.md`) - nur das
*Konsumieren* des Manifests durch den Launcher ist automatisiert (`main/launch/modBundleUpdater.ts`),
das *Befüllen* bleibt Handarbeit.

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

1. **`mod/`-Pins umstellen:** `gradle.properties` (`minecraft_version`, `loader_version`,
   `fabric_api_version`), `build.gradle.kts` (die drei `modImplementation("maven.modrinth:...")`-
   Koordinaten für Continuity/3D Skin Layers/Sodium). `fabric.mod.json`s `"minecraft"`-Range
   braucht **keinen** manuellen Schritt mehr - wird jetzt automatisch aus `minecraft_version`
   generiert (`${minecraft_version}`-Templating in `processResources`).
2. **Lokal bauen und testen:** `./gradlew build`, dann im Launcher-Dev-Modus eine Instanz auf der
   neuen Version anlegen, `launcher/mods-bundle/<neueVersion>/` und
   `launcher/resourcepacks-bundle/<neueVersion>/` lokal befüllen, live gegenprüfen. Mixins können
   bei einem Versionssprung brechen (bekanntes, akzeptiertes Risiko, siehe
   `Projekt_Roadmap.md`s Risiken-Abschnitt) - kein Zeitdruck, lieber gründlich als schnell.
3. **Eigenen Mod-Jar veröffentlichen:**
   ```
   gh release upload <tag> mod/build/libs/tntsallin1client-<version>.jar
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
   `launcher/mods-bundle/<version>/`/`resourcepacks-bundle/<version>/`/`mod/build/libs/`-Inhalt vor
   dem nächsten `electron-builder`-Lauf sicherstellen. Sonst entfällt dieser Schritt komplett -
   Nutzer laden die neue Version automatisch beim ersten "Play" nach, ganz ohne neuen
   Launcher-Installer.
8. Isolierter Commit (nur `mod-bundle-manifest.json`, ggf. + `types.ts`/`electron-builder.yml` bei
   Schritt 7), pushen.
