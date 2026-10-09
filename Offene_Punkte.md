# Offene Punkte (ohne 3D-Modelle)

Stand 2026-10-09. Zusammengetragen aus `Aktuelle_Phase.md`, `Projekt_Roadmap.md` und `Ideen_für_den_client.md`.
Stand der Veröffentlichung: 0.1.13 (2026-10-09); was danach gebaut wird, kommt unter 2a. Erledigtes steht nicht mehr hier, sondern in `Aktuelle_Phase.md`.

## 1. Noch nicht umgesetzt
- [ ] Eigene Mod auf weitere Minecraft-Versionen bringen. Regel seit 2026-10-02: ab 1.14 Fabric, darunter („Legacy“) ohne Mod-Loader mit eigenem Einstieg, kein Forge; pro alter Hauptversion nur die letzte Unterversion. Die vollständige Liste aller Versionen mit ihrem Stand steht in `Minecraft_Versionen.md`
  - [ ] Legacy-Versionen neben 1.8.9: 1.7.10, 1.9.4, 1.10.2, 1.11.2, 1.12.2 und 1.13.2 starten bisher als reines Minecraft ohne unsere Mod
  - [ ] 1.6.4 und älter: im Launcher noch nicht startbar (brauchen das alte Asset-Format und das Session-Argument)
  - [ ] Fabric-Versionen zwischen 1.15 und 1.21.10: starten mit Fabric, aber ohne unsere Mod (1.14.4 hat sie seit 0.1.9, 1.21.1 seit 0.1.10, 1.20.6 seit 0.1.11, 1.21.10 seit 0.1.12; 1.21.8 ist gebaut und bestätigt, siehe 1a)
- [ ] Eingabefelder in den Fabric-Versionen stehen im Client-Design noch in der Spielschrift (in 1.8.9 in der Client-Schrift) – eigener Umbau, von dir noch nicht entschieden
- [ ] Linux und macOS

## 1a. 1.21.8-Port: wartet auf Manifest und Release
Von dir am 2026-10-09 im Dev-Launcher getestet und bestätigt („passt alles inkl der änderungen die ich wollte“) - Einzelheiten in `Aktuelle_Phase.md`.
Committet und gepusht am 2026-10-09. Der Manifest-Eintrag geht nur zusammen mit einem Release: er verweist auf den Mod-Jar und die vier eigenen Pakete als Release-Dateien, die es erst dann gibt.
- [ ] Nur mit deinem OK: Release 0.1.14 mit Manifest-Eintrag `versions.1.21.8`

## 2. Noch von dir zu prüfen (schon veröffentlicht)
- [ ] Auto-Update des installierten Launchers auf 0.1.11
- [ ] Eine 1.20.6-Instanz im installierten Launcher (0.1.11): lädt er das Bundle übers Manifest (Mods und Ressourcenpakete), startet sie, verbindet Continuity die Texturen? Indium läuft seit 0.1.11 immer mit und darf im Mods-Screen nicht als Schalter auftauchen - das ist neuer Launcher-Code, den du noch nicht gesehen hast
- [ ] Eine 1.21.1-Instanz im installierten Launcher: lädt er das Bundle übers Manifest (Mods und Ressourcenpakete), startet sie, funktionieren die Schalter? Getestet war 1.21.1 bisher nur im Dev-Launcher mit lokal befülltem Bundle
- [ ] Hinweis im Launcher bei 1.21.2, 1.21.9, 26.1 und 26.1.1 (beim Anlegen einer Instanz und auf dem Startbildschirm) - gebaut, im Fenster noch von niemandem angesehen
- [ ] Helligkeit mit Fullbright in 1.21.1 und 1.21.11: Fullbright einschalten, Spiel beenden, neu starten und Fullbright ausschalten - die eigene Helligkeit muss noch dieselbe sein, im Log kein „Error saving option Brightness“ mehr

- [ ] Launcher-Hinweis auch bei 1.21.6 und 1.21.7 (verweist auf 1.21.8) - seit 0.1.11, im Fenster noch nicht angesehen

## 2a. Wartet auf das nächste Release
- [ ] Lösch-Abfragen im Client-Design (Wegpunkt löschen, alle Wegpunkte löschen, alle angepinnten Rezepte löschen) in 1.20.6, 1.21.1, 1.21.8, 1.21.10, 1.21.11, 26.1.2, 26.2 und 26.3 - gebaut, von dir bestätigt und committet am 2026-10-09. 1.8.9 und 1.14.4 hatten das schon. Die lokalen 0.1.13-Jars sind damit neuer als die veröffentlichten: Mod-Version beim nächsten Release hochsetzen. Unverändert im Spiel-Design: die Einladungs-Frage auf dem Titelbildschirm („Beitreten/Später“)

- [ ] Item-Rahmen (auch leuchtend) in der Hand mit 3D-Items: werden wie ein flaches Item gehalten statt wie ein Block - 3D-Blöcke-Paket für 1.14.4, 1.20.6, 1.21.1, 1.21.8, 1.21.10, 1.21.11, 26.1.2, 26.2 und 26.3 neu gebaut am 2026-10-09 (liegt im Dev-Bundle), von dir am selben Tag bestätigt, committet. Beim nächsten Release müssen alle neun `TNT-3D-Blocks-<version>.zip` neu hochgeladen und ihre Manifest-Einträge angepasst werden. Der Stolperdraht-Haken hängt ähnlich und wird weiter wie ein Block gehalten - sag Bescheid, falls der auch stört

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
