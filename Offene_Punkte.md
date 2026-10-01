# Offene Punkte (ohne 3D-Modelle)

Stand 2026-10-01. Zusammengetragen aus `Aktuelle_Phase.md`, `Projekt_Roadmap.md` und `Ideen_für_den_client.md`.
Alles Gebaute ist live bestätigt und mit 0.1.5 veröffentlicht (Ausnahmen stehen beim jeweiligen Punkt).

## 1. Noch nicht umgesetzt
- [ ] Eigene Mod auf weitere Minecraft-Versionen portieren (vor allem Vollversionen)
- [ ] Versionen vor 1.14 (z.B. 1.8.9 für PvP): der Launcher startet alles über Fabric, das geht erst ab 1.14 – bräuchte Start ohne Mod-Loader oder Legacy Fabric plus das alte Startformat
- [ ] Linux und macOS
- [ ] Logo am Nametag, damit man andere Client-Nutzer erkennt – mit 0.1.5 ausgeliefert; offen: Backend-Update auf nxlc.de (nur mit deinem OK), erst danach erscheinen Logos, und Test mit anderen Client-Nutzern
- [ ] Eigenes Dark-Mode-Texturepack statt Default Dark Mode (CC-BY-NC-SA) – später; nötig, bevor es eine Spendenmöglichkeit gibt (NC-Klausel), und damit Ports auf neue Versionen nicht an einem fremden Pack hängen

## 2. Vor einer öffentlichen Veröffentlichung
- [ ] Code-Signing: der Installer ist unsigniert und zeigt deshalb die SmartScreen-Warnung „Unbekannter Herausgeber“. Entscheidung 2026-10-01: zuerst kostenlos über SignPath Foundation versuchen (Herausgeber wäre „SignPath Foundation“), sonst Certum Open Source (49 €/Jahr, echter Name im Installer). Stand der Bewerbung:
  - [x] Auf GitHub: `README.md` (mit Abschnitt „Code signing policy“), `PRIVACY.md`, `.github/workflows/build-windows.yml`
  - [x] Schalter „Freunde und Online-Status“ im Freunde-Screen und Datenschutz-Hinweis als Seite im Installer (verlangt SignPath, wenn Daten automatisch an einen Server gehen) – live bestätigt, mit 0.1.5 veröffentlicht
  - [ ] Build-Workflow einmal auf GitHub laufen lassen (noch nie gelaufen)
  - [ ] `PRIVACY.md` gegenlesen (du und der Server-Besitzer)
  - [ ] Zwei-Faktor-Anmeldung bei GitHub einschalten
  - [ ] Auf signpath.org/apply bewerben
  - [ ] Nach Zusage: Signieren in den Workflow einbauen und den Release-Ablauf im Runbook umstellen (Release-Build kommt dann von GitHub statt vom eigenen PC)
