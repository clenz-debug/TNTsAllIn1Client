# Offene Punkte (ohne 3D-Modelle)

Stand 2026-10-01. Zusammengetragen aus `Aktuelle_Phase.md`, `Projekt_Roadmap.md` und `Ideen_für_den_client.md`.
Veröffentlicht ist 0.1.4. Die abgehakten Punkte unten sind seitdem gebaut und live bestätigt, sie kommen ins nächste Update.

## 1. Noch nicht umgesetzt
- [ ] Eigene Mod auf weitere Minecraft-Versionen portieren (vor allem Vollversionen)
- [ ] Versionen vor 1.14 (z.B. 1.8.9 für PvP): der Launcher startet alles über Fabric, das geht erst ab 1.14 – bräuchte Start ohne Mod-Loader oder Legacy Fabric plus das alte Startformat
- [ ] Linux und macOS
- [ ] Logo am Nametag, damit man andere Client-Nutzer erkennt – gebaut; offen: Backend-Update auf nxlc.de (nur mit deinem OK) und Test mit anderen Client-Nutzern
- [x] Wegpunkte: Richtungspfeil am Bildschirmrand für Wegpunkte außerhalb des Sichtfelds – live bestätigt (beide Versionen), kommt ins nächste Update
- [x] Wegpunkte: Scrollen im Bearbeiten-, Erstellen- und Optionen-Screen – live bestätigt, kommt ins nächste Update

## 2. Vor einer öffentlichen Veröffentlichung
- [x] Launcher beendet sich beim Schließen komplett – das Spiel wurde hart mitbeendet; jetzt läuft der Launcher im Hintergrund weiter (Tray), live bestätigt, kommt ins nächste Update
- [x] Lizenzen gegenlesen: Sodium (PolyForm Shield), Default Dark Mode und 3D Skin Layers (beide nicht-kommerziell) – eigene Lizenz GPL-3.0 gesetzt, Lizenztexte liegen im Installer (`launcher/third-party-licenses/`). Sodium: JellySquid hat es erlaubt, wenn der Launcher es von Modrinth lädt → seit 2026-10-01 enthält der Installer keine fremden Mods/Packs mehr, der Launcher lädt sie von Modrinth (mit Hinweis im Mods-Screen) – mit lokal gebautem Installer live bestätigt (beide Versionen starten), kommt ins nächste Update. 3D Skin Layers ist durch die eigene Umsetzung ersetzt (nächster Punkt), Default Dark Mode steht als eigener Punkt darunter
- [ ] Eigenes Dark-Mode-Texturepack statt Default Dark Mode (CC-BY-NC-SA) – später; nötig, bevor es eine Spendenmöglichkeit gibt (NC-Klausel), und damit Ports auf neue Versionen nicht an einem fremden Pack hängen
- [x] Eigene 3D-Skin-Layers statt der Mod von tr7zw (Options-Screen mit Teilen, Tiefe und Distanz; Standard-Tiefe 30 %) – live bestätigt (beide Versionen), kommt ins nächste Update. Lizenztext von 3D Skin Layers ist entfernt
- [x] `3dskinlayers` aus `mod-bundle-manifest.json` genommen (lokal, mit deinem OK) – wer die Mod lieber will, lädt sie selbst. Der Launcher räumt den alten Jar beim nächsten „Aktualisieren“ aus dem Bundle
- [ ] Zum Release beachten: das Manifest erst zusammen mit der neuen eigenen Mod pushen (`ownMod` hochsetzen) – die veröffentlichte Mod 0.1.4 verlangt 3D Skin Layers noch
- [ ] Code-Signing: der Installer ist unsigniert und zeigt deshalb die SmartScreen-Warnung „Unbekannter Herausgeber“. Entscheidung 2026-10-01: zuerst kostenlos über SignPath Foundation versuchen (Herausgeber wäre „SignPath Foundation“), sonst Certum Open Source (49 €/Jahr, echter Name im Installer). Stand der Bewerbung:
  - [x] Entwürfe lokal: `README.md` (mit Abschnitt „Code signing policy“), `PRIVACY.md`, `.github/workflows/build-windows.yml` (Build auf GitHub, noch nie gelaufen)
  - [x] Schalter „Freunde und Online-Status“ im Freunde-Screen und Datenschutz-Hinweis als Seite im Installer gebaut (verlangt SignPath, wenn Daten automatisch an einen Server gehen) – noch nicht live getestet
  - [ ] Update 0.1.5 veröffentlichen – vorbereitet (Versionen, Mods, Manifest, lokaler Installer in `launcher/dist/`); erst nach deinem Test des lokalen Installers. Die Entwürfe kommen mit dem Push von `main` auf GitHub
  - [ ] `PRIVACY.md` gegenlesen (du und der Server-Besitzer)
  - [ ] Zwei-Faktor-Anmeldung bei GitHub einschalten
  - [ ] Build-Workflow einmal auf GitHub laufen lassen, dann auf signpath.org/apply bewerben
  - [ ] Nach Zusage: Signieren in den Workflow einbauen und den Release-Ablauf im Runbook umstellen (Release-Build kommt dann von GitHub statt vom eigenen PC)

- [x] Shulkerbox-Anzeige: Vanilla-Inhaltsliste immer weg, solange das Feature an ist; während die Vorschau offen ist, kein Vanilla-Tooltip – live bestätigt (beide Versionen), kommt ins nächste Update

- [x] Freecam: Items droppen und in die Offhand nehmen gesperrt (Q/F in der Welt, Q/F/Rausklicken/Offhand-Slot/Schild-Shiftklick im Inventar, Wegwerfen im Kreativ-Inventar) – live bestätigt (beide Versionen), kommt ins nächste Update

- [x] Neues Feature „Kein Nebel“ mit Optionen (Sichtweite/Wetter, Wasser, Lava, Pulverschnee; Blindheit/Dunkelheit bleiben) – live bestätigt (beide Versionen), kommt ins nächste Update

## 3. Doku aufräumen
- [x] Veraltete Vermerke bereinigt (2026-10-01): `Ideen_für_den_client.md` (alte „noch nicht live getestet“-Vermerke, [check] für Farbschema und Updates, Stand zum Schließen des Launchers), `Projekt_Roadmap.md` (Phase 9: Installation beim Bekannten) und `Aktuelle_Phase.md` (Phasen 7b, 8/8b und 9 oben als abgeschlossen)
