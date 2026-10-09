# Offene Punkte (ohne 3D-Modelle)

Stand 2026-10-09. Zusammengetragen aus `Aktuelle_Phase.md`, `Projekt_Roadmap.md` und `Ideen_für_den_client.md`.
Stand der Veröffentlichung: 0.1.12 (2026-10-09); was danach gebaut wird, kommt unter 2a. Erledigtes steht nicht mehr hier, sondern in `Aktuelle_Phase.md`.

## 1. Noch nicht umgesetzt
- [ ] Eigene Mod auf weitere Minecraft-Versionen bringen. Regel seit 2026-10-02: ab 1.14 Fabric, darunter („Legacy“) ohne Mod-Loader mit eigenem Einstieg, kein Forge; pro alter Hauptversion nur die letzte Unterversion. Die vollständige Liste aller Versionen mit ihrem Stand steht in `Minecraft_Versionen.md`
  - [ ] Legacy-Versionen neben 1.8.9: 1.7.10, 1.9.4, 1.10.2, 1.11.2, 1.12.2 und 1.13.2 starten bisher als reines Minecraft ohne unsere Mod
  - [ ] 1.6.4 und älter: im Launcher noch nicht startbar (brauchen das alte Asset-Format und das Session-Argument)
  - [ ] Fabric-Versionen zwischen 1.15 und 1.21.10: starten mit Fabric, aber ohne unsere Mod (1.14.4 hat sie seit 0.1.9, 1.21.1 seit 0.1.10, 1.20.6 seit 0.1.11, 1.21.10 seit 0.1.12)
- [ ] Eingabefelder in den Fabric-Versionen stehen im Client-Design noch in der Spielschrift (in 1.8.9 in der Client-Schrift) – eigener Umbau, von dir noch nicht entschieden
- [ ] Linux und macOS

## 2. Noch von dir zu prüfen (schon veröffentlicht)
- [ ] Auto-Update des installierten Launchers auf 0.1.11
- [ ] Eine 1.20.6-Instanz im installierten Launcher (0.1.11): lädt er das Bundle übers Manifest (Mods und Ressourcenpakete), startet sie, verbindet Continuity die Texturen? Indium läuft seit 0.1.11 immer mit und darf im Mods-Screen nicht als Schalter auftauchen - das ist neuer Launcher-Code, den du noch nicht gesehen hast
- [ ] Eine 1.21.1-Instanz im installierten Launcher: lädt er das Bundle übers Manifest (Mods und Ressourcenpakete), startet sie, funktionieren die Schalter? Getestet war 1.21.1 bisher nur im Dev-Launcher mit lokal befülltem Bundle
- [ ] Hinweis im Launcher bei 1.21.2, 1.21.9, 26.1 und 26.1.1 (beim Anlegen einer Instanz und auf dem Startbildschirm) - gebaut, im Fenster noch von niemandem angesehen
- [ ] Helligkeit mit Fullbright in 1.21.1 und 1.21.11: Fullbright einschalten, Spiel beenden, neu starten und Fullbright ausschalten - die eigene Helligkeit muss noch dieselbe sein, im Log kein „Error saving option Brightness“ mehr

- [ ] Auto-Update des installierten Launchers auf 0.1.12
- [ ] Eine 1.21.10-Instanz im installierten Launcher (0.1.12): lädt er das Bundle übers Manifest, startet sie? Getestet war 1.21.10 bisher nur im Dev-Launcher mit lokal befülltem Bundle
- [ ] Neue Instanz startet ohne unsere Ressourcenpakete (seit 0.1.12): neue Instanz anlegen und starten - Dark Mode, 3D-Blöcke, 3D-Büsche und Bushy Vegetation müssen aus sein; in bestehenden Instanzen bleibt die Auswahl, wie sie ist
- [ ] 1.21.9 mit Fabric startet (seit 0.1.12; 1.21.10 hast du gestartet, 1.21.9 hat denselben Fehler gehabt und denselben Fix)
- [ ] Launcher-Hinweis auch bei 1.21.6 und 1.21.7 (verweist auf 1.21.8) - seit 0.1.11, im Fenster noch nicht angesehen

## 2a. Wartet auf das nächste Release
Nichts - mit 0.1.12 ist alles Gebaute veröffentlicht.

## 2b. Vor der 1.0 zu testen
Die 1.0 ist die Version, in der alles umgesetzt ist, was du beim Client haben wolltest – sie steht noch nicht an. Diese Tests sind einer der letzten Schritte vor ihrer Veröffentlichung (dein Vorschlag vom 2026-10-03).
- [ ] Multiplayer-Test mit einem zweiten Client-Nutzer auf demselben Server, beides in einem Durchgang:
  - [ ] Capes (1.14.4, 1.21.1, 1.21.11, 26.1.2, 26.3): sehen beide gegenseitig ihre Capes?
  - [ ] Logo am Nametag (1.14.4, 1.21.1, 1.21.11, 26.1.2, 26.3): sehen beide das Client-Logo am Namen des anderen, stehend und geduckt, in den richtigen Farben?

## 3. Vor einer öffentlichen Veröffentlichung
Derzeit nichts offen. Code-Signing steht unter „4. Eventuell“.

## 4. Eventuell
Ruht seit 2026-10-01, bis du dir Beratung geholt und es mit dem Besitzer des Servers abgesprochen hast – nichts voreilig tun. Bis dahin keine Bewerbung bei SignPath, kein Vertrag und keine Änderung an `PRIVACY.md`.

- [ ] Code-Signing: der Installer ist unsigniert und zeigt deshalb die SmartScreen-Warnung „Unbekannter Herausgeber“. Überlegung: zuerst kostenlos über SignPath Foundation versuchen (Herausgeber wäre „SignPath Foundation“), sonst Certum Open Source (49 €/Jahr, echter Name im Installer). Noch offen:
  - [ ] `PRIVACY.md` gegenlesen (du und der Server-Besitzer)
  - [ ] Auf signpath.org/apply bewerben
  - [ ] Nach Zusage: Signieren in den Workflow einbauen und den Release-Ablauf im Runbook umstellen (Release-Build kommt dann von GitHub statt vom eigenen PC)
- [ ] Auftragsverarbeitungsvertrag (AVV) mit dem Besitzer des Servers hinter nxlc.de: Entwurf liegt als Dokument vor (nicht im Repo, weil Namen und Anschriften hineinkommen). Dazu gehört auch, dass `PRIVACY.md` noch keinen Verantwortlichen mit Anschrift nennt
