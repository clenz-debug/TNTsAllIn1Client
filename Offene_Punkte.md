# Offene Punkte (ohne 3D-Modelle)

Stand 2026-10-02. Zusammengetragen aus `Aktuelle_Phase.md`, `Projekt_Roadmap.md` und `Ideen_für_den_client.md`.
Stand der Veröffentlichung: 0.1.7; was danach gebaut wurde, steht unter 2a. Erledigtes steht nicht mehr hier, sondern in `Aktuelle_Phase.md`.

## 1. Noch nicht umgesetzt
- [ ] Eigene Mod auf weitere Minecraft-Versionen portieren (vor allem Vollversionen)
- [ ] Legacy-Versionen (vor 1.14). Regel seit 2026-10-02: ab 1.14 Fabric, darunter ohne Mod-Loader (kein Forge), keine Loader-Auswahl pro Instanz. Ziel sind langfristig alle Minecraft-Versionen, pro alter Hauptversion nur die letzte Unterversion
  - [x] 1.7.10, 1.8.9, 1.9.4, 1.10.2, 1.11.2, 1.12.2, 1.13.2 starten als reines Minecraft: gebaut, Launcher von dir bestätigt (2026-10-02, „passt soweit“)
  - [ ] 1.6.4 und älter: brauchen noch das alte Asset-Format und das Session-Argument
  - [x] Eigener Einstieg für unseren Mod, Machbarkeitstest `mod/1.8.9/`: gebaut und von dir bestätigt (2026-10-02, „TNT's All-In-1 Client“ steht im Hauptmenü von 1.8.9)
  - [x] Namenstabelle: `mod/1.8.9/` nutzt Legacy Yarn (Legacy Fabric, gemeinfrei/CC0, deckt alle sieben Legacy-Versionen ab; Feather von Ornithe wäre die Alternative). Mixin-Zielangaben werden beim Bauen mitübersetzt, und `gradlew build` prüft jedes Mixin ohne Spielfenster gegen das echte Spiel (2026-10-02)
  - [x] 1.8.9: FPS-Anzeige gebaut und von dir bestätigt (2026-10-02)
  - [x] 1.8.9: Mod-Menü gebaut und von dir bestätigt (2026-10-02, „passt so“): Knopf „Client Mods“ im Hauptmenü unter „Optionen“ und im Pausenmenü, Taste „Mod-Menü öffnen“ in der Steuerung, Einstellungen in `config/tntsallin1client.json`, Texte deutsch/englisch
  - [x] 1.8.9: Farbwähler (Farbfeld, Farbton-Regler, Rot/Grün/Blau/Hex) und „Optionen“-Knopf im Mod-Menü gebaut und von dir bestätigt (2026-10-02, „jo passt“), erste Nutzung: Textfarbe der FPS-Anzeige. Farbwähler und Grundlage der Einstellungsseiten (`ClientScreen`) sind für weitere Features wiederverwendbar
  - [x] 1.8.9: HUD-Editor gebaut und von dir bestätigt (2026-10-02, „ok passt“): „HUD verschieben / skalieren“ im Mod-Menü unter „Sonstiges“, Box ziehen zum Verschieben, Ecke ziehen zum Skalieren, „Alle Positionen zurücksetzen“. Jedes HUD-Element (`HudElement`) ist damit automatisch verschieb- und skalierbar
  - [x] 1.8.9: Koordinaten-HUD, Latenz-Anzeige und Uhrzeit gebaut (2026-10-02) und von dir bestätigt (2026-10-03, „sind bestätigt und passen“), jeweils mit Farbe, Einstellungsseite und im HUD-Editor verschiebbar
  - [x] Fehler in allen Versionen behoben (2026-10-02, 1.21.11, 26.1.2, 26.3 und 1.8.9): War das Koordinaten-HUD an, aber alle seine Teile aus, gab es im HUD-Editor keine Box zum Verschieben – jetzt erscheint eine Box in der Größe des Namens. Von dir getestet und bestätigt (2026-10-02)
  - [ ] Offene Frage: Rüstungsstatus (gebündelt) mit allen Slots aus hat in den Fabric-Versionen ebenfalls keine Box im HUD-Editor – genauso beheben?
  - [x] 1.8.9: Tastenanzeige (Keystrokes) gebaut und von dir bestätigt (2026-10-03, „passt“): W/A/S/D, Shift, Space, LMB/RMB, Sprint, Drop, standardmäßig unten rechts, im HUD-Editor verschiebbar; Optionen mit „Aktiv-Farbe“, „Tasten-Text-Farbe“ (je eigene Seite mit Farbwähler) und „Tasten konfigurieren“ (einzelne Tasten per Klick aus/an). Auf deinen Wunsch ist die Aktiv-Farbe standardmäßig schwarz statt grün – in allen Versionen (1.8.9, 1.21.11, 26.1.2, 26.3); gilt nur für neue Configs, eine schon gespeicherte Farbe bleibt
  - [x] 1.8.9: Scrollen im Mod-Menü gebaut und von dir bestätigt (2026-10-03, „jo klappt“, zusammen mit Zoom): Mausrad und ziehbarer Balken rechts neben den Zeilen, Titel und „Fertig“ bleiben stehen
  - [x] 1.8.9: Zoom gebaut und von dir bestätigt (2026-10-03, „jo klappt“): Taste halten zoomt (standardmäßig unbelegt, einstellbar in den Zoom-Optionen oder in der Steuerung, auch Maustasten), Mausrad ändert dabei die Zoomstufe statt des Hotbar-Slots (wird gemerkt), Maus wird beim Zoomen verlangsamt (Regler „Zoom-Sensitivität“, 5–100 %, Standard 40 %). Neuer Menü-Abschnitt „Sicht & Rendering“. Die Spiel-Einstellungen (Sichtfeld, Mausempfindlichkeit) werden dabei nie verändert
  - [x] 1.8.9: Rüstungs- & Werkzeug-Status gebaut und von dir bestätigt (2026-10-03, „ok passt“): Helm, Brustpanzer, Hose, Schuhe, Haupthand (Nebenhand gibt es in 1.8.9 nicht) mit Haltbarkeit bzw. Stapelgröße, Item-Bild und Name; alle Einstellungen der Fabric-Versionen (Symbol-Position, Text-Ausrichtung, maximale Haltbarkeit, feste Farbe/Verlauf, gebündelt/einzeln, Richtung, Wachstum umkehren, Slots an/aus und Reihenfolge), im HUD-Editor verschiebbar. Dafür scrollen jetzt auch die Optionen-Seiten (gleicher Baustein wie im Mod-Menü, das darauf umgestellt wurde)
  - [x] 1.8.9: Eigenes Fadenkreuz gebaut und von dir bestätigt (2026-10-03, „jawohl passt“): ersetzt das Fadenkreuz des Spiels; 14 Vorlagen oder selbst gezeichnet (9×9-Raster), Größe 1–6, „GUI-Skalierung ignorieren“, eigene Farbe, dazu getrennt schaltbar andere Farbe und andere Form beim Zielen auf ein Mob. Menüzeile unter „Sicht & Rendering“. Anders als in den Fabric-Versionen hat die Farbe eine eigene Seite („Farbe...“), und die Form-Vorlage wechselt per Klick nur vorwärts
  - [x] 1.8.9: Fullbright gebaut und von dir bestätigt (2026-10-03, „fullbright passt“): Schalter im Mod-Menü unter „Sicht & Rendering“ (keine eigene Optionen-Seite, wie in den Fabric-Versionen); die Helligkeits-Einstellung des Spiels bleibt unverändert
  - [x] 1.8.9: Suche im Mod-Menü gebaut und von dir bestätigt (2026-10-03, „ja passt alles“): Suchfeld unter dem Titel („Suchen...“), filtert die Zeilen beim Tippen nach ihrem Namen, Abschnitte ohne Treffer verschwinden samt Überschrift, der Suchtext bleibt nach einem Besuch auf einer Optionen-Seite stehen
  - [ ] 1.8.9, noch nicht dabei: weitere Features
  - [ ] Vor einem Release klären: LaunchWrapper hat bei Mojang keine erklärte Lizenz (der Launcher lädt es von Mojangs Server, wir liefern es nicht mit); Mixin ist MIT und steckt in unserer Jar (Lizenztext liegt bei)
  - [ ] Features für die Legacy-Versionen: kleiner Kern, Neuschreiben statt Portierung
  - [ ] Prüfen, ob Fabric-Instanzen 1.14–1.16.5 gegen Log4Shell geschützt sind – der Launcher wendet den Filter bisher nur bei den Legacy-Versionen an
- [ ] Linux und macOS


## 2. Noch von dir zu prüfen (mit 0.1.7 schon veröffentlicht)
- [ ] Logo am Nametag: Test mit anderen Client-Nutzern

## 2a. Bestätigt, wartet auf das nächste Release
- [ ] Nächstes Release (nur mit deinem OK): Mod-Version hochsetzen, die drei `TNT-Dark-Mode-<version>.zip` neu hochladen und ihre Manifest-Einträge hochziehen. Enthält bisher: dunkles Löschen-Feld im Dark Mode, die drei Symbol-Buttons im 26.3-Client-Design, Freunde-Menü im Hauptmenü samt Tour-Schritt - alles von dir bestätigt (2026-10-02)

## 3. Vor einer öffentlichen Veröffentlichung
Derzeit nichts offen. Code-Signing steht unter „4. Eventuell“.

## 4. Eventuell
Ruht seit 2026-10-01, bis du dir Beratung geholt und es mit dem Besitzer des Servers abgesprochen hast – nichts voreilig tun. Bis dahin keine Bewerbung bei SignPath, kein Vertrag und keine Änderung an `PRIVACY.md`.

- [ ] Code-Signing: der Installer ist unsigniert und zeigt deshalb die SmartScreen-Warnung „Unbekannter Herausgeber“. Überlegung: zuerst kostenlos über SignPath Foundation versuchen (Herausgeber wäre „SignPath Foundation“), sonst Certum Open Source (49 €/Jahr, echter Name im Installer). Noch offen:
  - [ ] `PRIVACY.md` gegenlesen (du und der Server-Besitzer)
  - [ ] Auf signpath.org/apply bewerben
  - [ ] Nach Zusage: Signieren in den Workflow einbauen und den Release-Ablauf im Runbook umstellen (Release-Build kommt dann von GitHub statt vom eigenen PC)
- [ ] Auftragsverarbeitungsvertrag (AVV) mit dem Besitzer des Servers hinter nxlc.de: Entwurf liegt als Dokument vor (nicht im Repo, weil Namen und Anschriften hineinkommen). Dazu gehört auch, dass `PRIVACY.md` noch keinen Verantwortlichen mit Anschrift nennt
