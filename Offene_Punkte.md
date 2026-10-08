# Offene Punkte (ohne 3D-Modelle)

Stand 2026-10-08. Zusammengetragen aus `Aktuelle_Phase.md`, `Projekt_Roadmap.md` und `Ideen_für_den_client.md`.
Stand der Veröffentlichung: 0.1.9 (2026-10-08); was danach gebaut wird, kommt unter 2a. Erledigtes steht nicht mehr hier, sondern in `Aktuelle_Phase.md`.

## 1. Noch nicht umgesetzt
- [ ] Eigene Mod auf weitere Minecraft-Versionen bringen. Regel seit 2026-10-02: ab 1.14 Fabric, darunter („Legacy“) ohne Mod-Loader mit eigenem Einstieg, kein Forge; pro alter Hauptversion nur die letzte Unterversion. Die vollständige Liste aller Versionen mit ihrem Stand steht in `Minecraft_Versionen.md`
  - [ ] Legacy-Versionen neben 1.8.9: 1.7.10, 1.9.4, 1.10.2, 1.11.2, 1.12.2 und 1.13.2 starten bisher als reines Minecraft ohne unsere Mod
  - [ ] 1.6.4 und älter: im Launcher noch nicht startbar (brauchen das alte Asset-Format und das Session-Argument)
  - [ ] Fabric-Versionen zwischen 1.15 und 1.21.10: starten mit Fabric, aber ohne unsere Mod (1.14.4 hat sie seit 0.1.9; 1.21.1 ist gebaut, siehe 1a)
- [ ] Eingabefelder in den Fabric-Versionen stehen im Client-Design noch in der Spielschrift (in 1.8.9 in der Client-Schrift) – eigener Umbau, von dir noch nicht entschieden
- [ ] Linux und macOS

## 1a. 1.21.1-Port (gebaut 2026-10-08, noch nie im Spiel gestartet, nicht veröffentlicht)
`mod/1.21.1/` baut, die Mixin-Ziele sind gegen das Spiel geprüft, Pakete und Fremd-Mods liegen im Dev-Bundle. Zum Testen: `npm run dev`, eine 1.21.1-Instanz anlegen und starten. Was in 1.21.1 anders gebaut ist als in 1.21.11, steht in `Aktuelle_Phase.md` - diese Teile sind die wahrscheinlichsten Fehlerquellen:
- [ ] Startet die Instanz überhaupt (Titelbildschirm im Client-Design, Mod-Menü, Kartenmenü, Optionsmenüs)?
- [ ] HUD: alle Elemente samt HUD-Editor; F3-Schnellinfo und Systeminfo-Seite (F3+K)
- [ ] Welt: Wegpunkte (Strahl, Rahmen, Schild), Spawn-Overlay, Hitbox- und Blockumriss-Farbe, Kein Nebel (Sichtweite, Wasser, Lava, Pulverschnee), Itemphysics, Freecam
- [ ] 3D-Skin-Layer (auch der Ärmel in der Ich-Ansicht, auch nach einem Skin-Wechsel), Client-Capes, Offline-Skin
- [ ] Inventar: Schnellsortieren, Shulker-Vorschau, angepinnte Rezepte, Screenshot-Nachricht mit „[Kopieren]“
- [ ] Ressourcenpakete: Dark Mode, 3D-Blöcke mit Büschen und Bushy Vegetation, „3D-Items in Inventar & Hand“ (aus = flache Items in Inventar und Hand), Hängeschild-Ketten
- [ ] Freunde, Welt-Einladung über e4mc, Discord-Aktivität, Tour im Spiel
- [x] Karten im Kartenmenü und der Schalter „Verbundene Texturen“ (nach dem ersten Test korrigiert, von dir am 2026-10-08 bestätigt)
- [ ] Helligkeit mit Fullbright: Fullbright einschalten, Spiel beenden, neu starten und Fullbright ausschalten - die eigene Helligkeit muss noch dieselbe sein, und im Log darf kein „Error saving option Brightness“ mehr stehen. Gilt genauso für 1.21.11 (dort seit 2026-10-08 derselbe Schutz wie in 26.x; der lokale 1.21.11-Jar ist damit neuer als der veröffentlichte 0.1.9)
- [ ] Danach, nur mit deinem OK: Manifest-Eintrag `versions.1.21.1` und Release

## 2. Noch von dir zu prüfen (mit 0.1.9 schon veröffentlicht)
Derzeit nichts offen. Das Auto-Update auf 0.1.9 und eine 1.14.4-Instanz im installierten Launcher sind von dir getestet und bestätigt (2026-10-08).

## 2a. Bestätigt, wartet auf das nächste Release
Derzeit nichts.

## 2b. Vor der 1.0 zu testen
Die 1.0 ist die Version, in der alles umgesetzt ist, was du beim Client haben wolltest – sie steht noch nicht an. Diese Tests sind einer der letzten Schritte vor ihrer Veröffentlichung (dein Vorschlag vom 2026-10-03).
- [ ] Multiplayer-Test mit einem zweiten Client-Nutzer auf demselben Server, beides in einem Durchgang:
  - [ ] Capes (1.14.4, 1.21.11, 26.1.2, 26.3): sehen beide gegenseitig ihre Capes?
  - [ ] Logo am Nametag (1.14.4, 1.21.11, 26.1.2, 26.3): sehen beide das Client-Logo am Namen des anderen, stehend und geduckt, in den richtigen Farben?

## 3. Vor einer öffentlichen Veröffentlichung
Derzeit nichts offen. Code-Signing steht unter „4. Eventuell“.

## 4. Eventuell
Ruht seit 2026-10-01, bis du dir Beratung geholt und es mit dem Besitzer des Servers abgesprochen hast – nichts voreilig tun. Bis dahin keine Bewerbung bei SignPath, kein Vertrag und keine Änderung an `PRIVACY.md`.

- [ ] Code-Signing: der Installer ist unsigniert und zeigt deshalb die SmartScreen-Warnung „Unbekannter Herausgeber“. Überlegung: zuerst kostenlos über SignPath Foundation versuchen (Herausgeber wäre „SignPath Foundation“), sonst Certum Open Source (49 €/Jahr, echter Name im Installer). Noch offen:
  - [ ] `PRIVACY.md` gegenlesen (du und der Server-Besitzer)
  - [ ] Auf signpath.org/apply bewerben
  - [ ] Nach Zusage: Signieren in den Workflow einbauen und den Release-Ablauf im Runbook umstellen (Release-Build kommt dann von GitHub statt vom eigenen PC)
- [ ] Auftragsverarbeitungsvertrag (AVV) mit dem Besitzer des Servers hinter nxlc.de: Entwurf liegt als Dokument vor (nicht im Repo, weil Namen und Anschriften hineinkommen). Dazu gehört auch, dass `PRIVACY.md` noch keinen Verantwortlichen mit Anschrift nennt
