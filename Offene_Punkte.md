# Offene Punkte (ohne 3D-Modelle)

Stand 2026-09-26. Zusammengetragen aus `Aktuelle_Phase.md`, `Projekt_Roadmap.md` und `Ideen_für_den_client.md`.
Punkte unter 1. sind gebaut, aber in der Doku nicht als live bestätigt vermerkt – einiges davon hast du
vielleicht längst benutzt, dann einfach abhaken.

## 1. Gebaut, aber noch nicht live bestätigt

### Mod
- [ ] Ein/Aus-Schalter als erste Zeile in allen 16 Feature-Optionsmenüs (Koordinaten, Item Counter, FPS, Keystrokes, Zoom, Fadenkreuz, Wegpunkte, …)
- [ ] Shulkerbox-Vorschau im Lunar-Stil: Rahmen, Titelleiste und Raster komplett in der Box-Farbe (drei Helligkeitsstufen)
- [ ] Wegpunkte: Liste nach Dimension in Abschnitte gegliedert (Oberwelt, Nether, Ende)
- [ ] Wegpunkte: Marker-Würfel einzeln abschaltbar und „in der Nähe ausblenden“ (verblasst zwischen 10 und 3 Blöcken)
- [ ] Wegpunkte: Farbe schon beim Erstellen wählbar
- [ ] Wegpunkte pro Welt getrennt – nach dem Fix (vorher teilten sich alle Einzelspielerwelten eine Liste) nicht erneut geprüft
- [ ] 26.1.2: Die Mod läuft (beim ersten Release getestet), ein Durchgang Feature für Feature ist aber nicht dokumentiert. Der 3D-Skin-Layers-Schalter im Mod-Menü ist dort ungeprüft; sein Optionen-Button ist absichtlich aus, bis es einen echten 26.x-Build von 3D Skin Layers gibt

### Launcher
- [ ] „Von anderem Client übernehmen…“ im Instanzen-Screen (Einstellungen und eigene Mods aus einem anderen Minecraft-Ordner)
- [ ] Instanzen: Umbenennen direkt in der Zeile, und Auswählen schließt den Screen nicht mehr – nach dem Fix nicht erneut bestätigt

## 2. Noch nicht umgesetzt
- [ ] Eigene Mod auf weitere Minecraft-Versionen portieren (vor allem Vollversionen)
- [ ] Versionen vor 1.14 (z.B. 1.8.9 für PvP): der Launcher startet alles über Fabric, das geht erst ab 1.14 – bräuchte Start ohne Mod-Loader oder Legacy Fabric plus das alte Startformat
- [ ] Linux und macOS
- [ ] Logo am Nametag, damit man andere Client-Nutzer erkennt (letzte Zeile der Ideen-Datei)
- [ ] Wegpunkte: Richtungspfeil am Bildschirmrand für Wegpunkte außerhalb des Sichtfelds; Scrollen im Bearbeiten-Screen (nur nötig, falls er bei kleinem Fenster abgeschnitten wird)
- [ ] Finales Launcher-Redesign (bewusst ans Ende geschoben, bis alle Launcher-Features stehen)
- [ ] Optional: eigenes e4mc-Relay auf nxlc.de statt e4mcs Servern (nur mit OK des Server-Besitzers, dann läuft der Spielverkehr über seine Leitung)

## 3. Vor einer öffentlichen Veröffentlichung
- [ ] Launcher beendet sich beim Schließen komplett – prüfen, ob ein laufendes Spiel das überlebt
- [ ] Lizenzen gegenlesen: Sodium (PolyForm Shield), Default Dark Mode und 3D Skin Layers (beide nicht-kommerziell)
- [ ] Code-Signing: der Installer ist unsigniert und zeigt deshalb die SmartScreen-Warnung „Unbekannter Herausgeber“ (Zertifikat kostet Geld – Entscheidungsfrage)

## 4. Doku aufräumen
Veraltete Vermerke, die woanders schon als erledigt stehen:
- `Ideen_für_den_client.md`: Modrinth-Suche, Shulkerbox-Anzeige, Armor-Status-Maximalgröße und die Skin-Editor-Punkte stehen dort noch als „noch nicht live getestet“, sind laut `Aktuelle_Phase.md` aber bestätigt. Das Farbschema (Einstellungen-Screen mit Themes) ist gebaut, hat dort aber kein [check].
- `Projekt_Roadmap.md`, Phase 9: „Noch offen: Installation beim Bekannten“ – die Tests mit dem Bekannten (inkl. Installation) sind darunter alle abgehakt.
- `Aktuelle_Phase.md`: Phase 9 steht oben noch als „in Arbeit, kein echtes GitHub-Release“, obwohl v0.1.0 bis 0.1.3 veröffentlicht sind.
