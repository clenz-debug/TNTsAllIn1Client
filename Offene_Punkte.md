# Offene Punkte (ohne 3D-Modelle)

Stand 2026-09-27. Zusammengetragen aus `Aktuelle_Phase.md`, `Projekt_Roadmap.md` und `Ideen_für_den_client.md`.
Alles Gebaute ist live bestätigt und mit 0.1.4 veröffentlicht.

## 1. Noch nicht umgesetzt
- [ ] Eigene Mod auf weitere Minecraft-Versionen portieren (vor allem Vollversionen)
- [ ] Versionen vor 1.14 (z.B. 1.8.9 für PvP): der Launcher startet alles über Fabric, das geht erst ab 1.14 – bräuchte Start ohne Mod-Loader oder Legacy Fabric plus das alte Startformat
- [ ] Linux und macOS
- [ ] Logo am Nametag, damit man andere Client-Nutzer erkennt – gebaut; offen: Backend-Update auf nxlc.de (nur mit deinem OK) und Test mit anderen Client-Nutzern
- [x] Wegpunkte: Richtungspfeil am Bildschirmrand für Wegpunkte außerhalb des Sichtfelds – live bestätigt (beide Versionen), kommt ins nächste Update
- [x] Wegpunkte: Scrollen im Bearbeiten-, Erstellen- und Optionen-Screen – live bestätigt, kommt ins nächste Update

## 2. Vor einer öffentlichen Veröffentlichung
- [x] Launcher beendet sich beim Schließen komplett – das Spiel wurde hart mitbeendet; jetzt läuft der Launcher im Hintergrund weiter (Tray), live bestätigt, kommt ins nächste Update
- [ ] Lizenzen gegenlesen: Sodium (PolyForm Shield), Default Dark Mode und 3D Skin Layers (beide nicht-kommerziell)
- [ ] Code-Signing: der Installer ist unsigniert und zeigt deshalb die SmartScreen-Warnung „Unbekannter Herausgeber“ (Zertifikat kostet Geld – Entscheidungsfrage)

- [x] Shulkerbox-Anzeige: Vanilla-Inhaltsliste immer weg, solange das Feature an ist; während die Vorschau offen ist, kein Vanilla-Tooltip – live bestätigt (beide Versionen), kommt ins nächste Update

- [x] Freecam: Items droppen und in die Offhand nehmen gesperrt (Q/F in der Welt, Q/F/Rausklicken/Offhand-Slot/Schild-Shiftklick im Inventar, Wegwerfen im Kreativ-Inventar) – live bestätigt (beide Versionen), kommt ins nächste Update

- [x] Neues Feature „Kein Nebel“ mit Optionen (Sichtweite/Wetter, Wasser, Lava, Pulverschnee; Blindheit/Dunkelheit bleiben) – live bestätigt (beide Versionen), kommt ins nächste Update

## 3. Doku aufräumen
Veraltete Vermerke, die woanders schon als erledigt stehen:
- `Ideen_für_den_client.md`: Modrinth-Suche, Shulkerbox-Anzeige, Armor-Status-Maximalgröße und die Skin-Editor-Punkte stehen dort noch als „noch nicht live getestet“, sind laut `Aktuelle_Phase.md` aber bestätigt. Das Farbschema (Einstellungen-Screen mit Themes) ist gebaut, hat dort aber kein [check].
- `Projekt_Roadmap.md`, Phase 9: „Noch offen: Installation beim Bekannten“ – die Tests mit dem Bekannten (inkl. Installation) sind darunter alle abgehakt.
- `Aktuelle_Phase.md`: Phase 9 steht oben noch als „in Arbeit, kein echtes GitHub-Release“, obwohl v0.1.0 bis 0.1.4 veröffentlicht sind.
