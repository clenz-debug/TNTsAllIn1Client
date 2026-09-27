# Offene Punkte (ohne 3D-Modelle)

Stand 2026-09-27. Zusammengetragen aus `Aktuelle_Phase.md`, `Projekt_Roadmap.md` und `Ideen_für_den_client.md`.
Alles Gebaute ist live bestätigt und mit 0.1.4 veröffentlicht.

## 1. Noch nicht umgesetzt
- [ ] Eigene Mod auf weitere Minecraft-Versionen portieren (vor allem Vollversionen)
- [ ] Versionen vor 1.14 (z.B. 1.8.9 für PvP): der Launcher startet alles über Fabric, das geht erst ab 1.14 – bräuchte Start ohne Mod-Loader oder Legacy Fabric plus das alte Startformat
- [ ] Linux und macOS
- [ ] Logo am Nametag, damit man andere Client-Nutzer erkennt (letzte Zeile der Ideen-Datei)
- [ ] Wegpunkte: Richtungspfeil am Bildschirmrand für Wegpunkte außerhalb des Sichtfelds; Scrollen im Bearbeiten-Screen (nur nötig, falls er bei kleinem Fenster abgeschnitten wird)
- [ ] Finales Launcher-Redesign (bewusst ans Ende geschoben, bis alle Launcher-Features stehen)
- [ ] Optional: eigenes e4mc-Relay auf nxlc.de statt e4mcs Servern (nur mit OK des Server-Besitzers, dann läuft der Spielverkehr über seine Leitung)

## 2. Vor einer öffentlichen Veröffentlichung
- [ ] Launcher beendet sich beim Schließen komplett – prüfen, ob ein laufendes Spiel das überlebt
- [ ] Lizenzen gegenlesen: Sodium (PolyForm Shield), Default Dark Mode und 3D Skin Layers (beide nicht-kommerziell)
- [ ] Code-Signing: der Installer ist unsigniert und zeigt deshalb die SmartScreen-Warnung „Unbekannter Herausgeber“ (Zertifikat kostet Geld – Entscheidungsfrage)

## 3. Doku aufräumen
Veraltete Vermerke, die woanders schon als erledigt stehen:
- `Ideen_für_den_client.md`: Modrinth-Suche, Shulkerbox-Anzeige, Armor-Status-Maximalgröße und die Skin-Editor-Punkte stehen dort noch als „noch nicht live getestet“, sind laut `Aktuelle_Phase.md` aber bestätigt. Das Farbschema (Einstellungen-Screen mit Themes) ist gebaut, hat dort aber kein [check].
- `Projekt_Roadmap.md`, Phase 9: „Noch offen: Installation beim Bekannten“ – die Tests mit dem Bekannten (inkl. Installation) sind darunter alle abgehakt.
- `Aktuelle_Phase.md`: Phase 9 steht oben noch als „in Arbeit, kein echtes GitHub-Release“, obwohl v0.1.0 bis 0.1.4 veröffentlicht sind.
