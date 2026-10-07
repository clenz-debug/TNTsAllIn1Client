# Offene Punkte (ohne 3D-Modelle)

Stand 2026-10-04. Zusammengetragen aus `Aktuelle_Phase.md`, `Projekt_Roadmap.md` und `Ideen_für_den_client.md`.
Stand der Veröffentlichung: 0.1.8 (2026-10-04); was danach gebaut wird, kommt unter 2a. Erledigtes steht nicht mehr hier, sondern in `Aktuelle_Phase.md`.

## 1. Noch nicht umgesetzt
- [ ] Eigene Mod auf weitere Minecraft-Versionen bringen. Regel seit 2026-10-02: ab 1.14 Fabric, darunter („Legacy“) ohne Mod-Loader mit eigenem Einstieg, kein Forge; pro alter Hauptversion nur die letzte Unterversion
  - [ ] Legacy-Versionen neben 1.8.9: 1.7.10, 1.9.4, 1.10.2, 1.11.2, 1.12.2 und 1.13.2 starten bisher als reines Minecraft ohne unsere Mod
  - [ ] 1.6.4 und älter: im Launcher noch nicht startbar (brauchen das alte Asset-Format und das Session-Argument)
  - [ ] Fabric-Versionen zwischen 1.14 und 1.21.10: starten mit Fabric, aber ohne unsere Mod
  - [ ] 1.14.4-Port (begonnen 2026-10-07, `mod/1.14.4/`: Fabric, Mojang-Namen, Java 8). Reihenfolge:
    - [x] Grundgerüst: baut und lädt – Start im Launcher von dir bestätigt (2026-10-07)
    - [x] Fundament – von dir bestätigt (2026-10-07): Einstellungen, Taste fürs Mod-Menü, Mod-Menü (Listenansicht mit Suche) mit Optionsmenüs samt Farbwähler, Knopf „Client Mods“ im Pause- und Titelmenü, Zurücksetzen (einzeln und alle), Sprachdateien
    - [x] HUD – von dir bestätigt (2026-10-07): Koordinaten, FPS, Latenz, Uhr, Keystrokes, Rüstungs-Status, Item-Zähler, HUD-Editor
    - [x] Rendering – von dir bestätigt (2026-10-07): Zoom, Freecam (mit sichtbarem Körper in F5), Fadenkreuz, Fullbright, Kein Nebel, Partikel-Filter, Spawn-Overlay, Hitbox- und Blockumriss-Farbe, Itemphysics, Wegpunkte
    - [x] Inventar – von dir bestätigt (2026-10-07/08): Schnellsortieren, Shulker-Vorschau, Screenshot-Nachricht, angepinnte Rezepte
      - [x] Gemeldet 2026-10-07 (überlappende Trankeffekt-Einträge neben dem Inventar bei mehr als 5 Effekten): kein Fehler der Mod – 1.14.4 staucht die Liste selbst so zusammen (im Bytecode geprüft, 2026-10-08)
    - [ ] Zu prüfen in 1.21.11, 26.1.2 und 26.3: funktioniert „[Kopieren]“ in der Screenshot-Nachricht? Das Spiel sperrt dort Javas Zwischenablage für Bilder (Headless-Modus); falls nicht, den PowerShell-Weg aus 1.14.4 nachziehen
    - [x] Client-Design (Titelbildschirm, Pausenmenü, Kartenmenü, Schrift, Knöpfe/Regler/Textfelder im Design) – von dir im Spiel bestätigt 2026-10-07
    - [x] Tour im Spiel inkl. Freunde-Schritte – von dir bestätigt 2026-10-07 (Design-Wechsel nach Start im Client-Design nachgebessert)
    - [x] Freunde-Menü (Knopf auf Titelbildschirm und im Pausenmenü, Freunde-Bildschirm, Einladungs-Hinweis, Aktivität für den Launcher) – von dir bestätigt 2026-10-07; Umfang wie 1.8.9: Einladungen nur ablehnbar, in die eigene Welt einladen geht nicht (e4mc gibt es erst ab 1.17)
    - [x] Client-Logo am Namensschild von Client-Nutzern – am eigenen Namen von dir bestätigt 2026-10-07; bei anderen Spielern erst mit zweitem Client-Nutzer prüfbar (wie in den anderen Versionen, einer der letzten Schritte vor 1.0)
    - [x] Discord-Aktivität (mit Optionen) – von dir bestätigt 2026-10-07; wie in 1.8.9 nur unter Windows
    - [x] Capes (Client-Capes mit Schalter, eigener Skin/Cape im Offline-Modus) – von dir pauschal bestätigt 2026-10-08 („was wir gemacht hatten passt“)
      - [x] Elytra bei Client-Cape (gemeldet 2026-10-07: einfarbig graue Elytra): in 1.14.4 behält die Elytra jetzt ihr eigenes Aussehen, wenn der Elytra-Bereich des Capes nur eine Farbe hat (so füllt ihn der Cape-Konverter) – von dir im Spiel gesehen 2026-10-07 (Vanilla-Elytra)
      - [x] 1.21.11, 26.1.2 und 26.3: dort gibt die mitgelieferte Cape-Provider-Mod der Elytra jedes 2:1-Cape-Bild (im Bytecode geprüft) – dieselbe Regel wie in 1.14.4 eingebaut 2026-10-07 (`CapeProviderElytraMixin`), von dir pauschal bestätigt 2026-10-08
    - [x] 3D-Skin-Layer (mit Optionen: Teile, Tiefe, Entfernung; 3D-Ärmel am Arm in der Ich-Ansicht) – von dir pauschal bestätigt 2026-10-08
      - [x] Tiefe der 3D-Skin-Layer lässt sich jetzt ab 10 % statt ab 25 % einstellen – in allen fünf Versionen geändert, von dir bestätigt 2026-10-08
    - [ ] Eigene Ressourcenpakete für 1.14.4 und Drittanbieter-Mods klären (welche der gebündelten Mods es für 1.14.4 überhaupt gibt), danach Manifest-Eintrag und Release
- [ ] Eingabefelder in den Fabric-Versionen stehen im Client-Design noch in der Spielschrift (in 1.8.9 in der Client-Schrift) – eigener Umbau, von dir noch nicht entschieden
- [ ] Linux und macOS

## 2. Noch von dir zu prüfen (mit 0.1.8 schon veröffentlicht)
Derzeit nichts offen. Der installierte Launcher 0.1.8 und 1.8.9 mit den Client-Features sind von dir getestet und bestätigt (2026-10-04).

## 2a. Bestätigt, wartet auf das nächste Release
- [x] Partikel-Filter: jeder Partikel einzeln an- und ausschaltbar (alle vier Mod-Versionen), von dir bestätigt am 2026-10-07
- [x] Freecam in 1.8.9: sichtbarer Körper wie in den neuen Versionen (in F5, eigener Skin, ohne Cape) statt unsichtbarer Kamera, von dir bestätigt am 2026-10-07
- [x] Freecam: Spieler bleibt nach einem Treffer nicht mehr rot und zuckend (alle vier Mod-Versionen); Freecam-Körper ohne Cape (1.21.11, 26.1.2, 26.3), von dir bestätigt am 2026-10-07

## 2b. Vor der 1.0 zu testen
Die 1.0 ist die Version, in der alles umgesetzt ist, was du beim Client haben wolltest – sie steht noch nicht an. Diese Tests sind einer der letzten Schritte vor ihrer Veröffentlichung (dein Vorschlag vom 2026-10-03).
- [ ] Multiplayer-Test mit einem zweiten Client-Nutzer auf demselben Server, beides in einem Durchgang:
  - [ ] Capes (1.21.11, 26.1.2, 26.3): sehen beide gegenseitig ihre Capes?
  - [ ] Logo am Nametag (1.21.11, 26.1.2, 26.3): sehen beide das Client-Logo am Namen des anderen, stehend und geduckt, in den richtigen Farben?

## 3. Vor einer öffentlichen Veröffentlichung
Derzeit nichts offen. Code-Signing steht unter „4. Eventuell“.

## 4. Eventuell
Ruht seit 2026-10-01, bis du dir Beratung geholt und es mit dem Besitzer des Servers abgesprochen hast – nichts voreilig tun. Bis dahin keine Bewerbung bei SignPath, kein Vertrag und keine Änderung an `PRIVACY.md`.

- [ ] Code-Signing: der Installer ist unsigniert und zeigt deshalb die SmartScreen-Warnung „Unbekannter Herausgeber“. Überlegung: zuerst kostenlos über SignPath Foundation versuchen (Herausgeber wäre „SignPath Foundation“), sonst Certum Open Source (49 €/Jahr, echter Name im Installer). Noch offen:
  - [ ] `PRIVACY.md` gegenlesen (du und der Server-Besitzer)
  - [ ] Auf signpath.org/apply bewerben
  - [ ] Nach Zusage: Signieren in den Workflow einbauen und den Release-Ablauf im Runbook umstellen (Release-Build kommt dann von GitHub statt vom eigenen PC)
- [ ] Auftragsverarbeitungsvertrag (AVV) mit dem Besitzer des Servers hinter nxlc.de: Entwurf liegt als Dokument vor (nicht im Repo, weil Namen und Anschriften hineinkommen). Dazu gehört auch, dass `PRIVACY.md` noch keinen Verantwortlichen mit Anschrift nennt
