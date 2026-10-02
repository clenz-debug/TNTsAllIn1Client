# Offene Punkte (ohne 3D-Modelle)

Stand 2026-10-01. Zusammengetragen aus `Aktuelle_Phase.md`, `Projekt_Roadmap.md` und `Ideen_für_den_client.md`.
Alles Gebaute ist mit 0.1.6 veröffentlicht (Ausnahmen stehen beim jeweiligen Punkt).

## 1. Noch nicht umgesetzt
- [ ] Eigene Mod auf weitere Minecraft-Versionen portieren (vor allem Vollversionen)
- [ ] **26.3-Port beenden** (angefangen 2026-10-01). Stand:
  - [x] Machbarkeit geprüft: Minecraft 26.3 ist seit 2026-09-15 draußen (Java 25), alle Fremd-Mods gibt es auf Modrinth (Sodium noch als Alpha)
  - [x] `mod/26.3/` als Kopie von `mod/26.1.2/` angelegt und an die 26.3-API angepasst - `./gradlew build` grün
  - [x] Alle Mixin-Ziele gegen den 26.3-Jar geprüft (`python mod/check_mixins.py 26.3`: 103 Ziele, 0 Probleme)
  - [x] Eigene Resourcepacks für 26.3 gebaut (Pack-Format 97) und geprüft, dass jede Textur und jedes Modell darin in 26.3 existiert - liegen in `launcher/resourcepacks-bundle/26.3/`
  - [x] Fremd-Mods für 26.3 von Modrinth ins Dev-Bundle `launcher/mods-bundle/26.3/` geladen (Prüfsummen stimmen)
  - [x] **Du testest im Spiel:** 26.3-Instanz im Dev-Launcher angelegt, läuft seit 2026-10-01 (mehrere Testrunden, Meldungen stehen in `Aktuelle_Phase.md`)
  - [x] 26.3-Schilder: 3D-Ketten an Hängeschildern und die Schild-Items sind jetzt im 3D-Pack gebaut (statt im Mod-Code) - von dir im Spiel bestätigt (2026-10-02, „Schilder passen in allen 3 Versionen“). Grafikfehler an der Spitze unter einer Kette (die zwei schrägen Ketten kreuzten sich und steckten ineinander): Spitze am 2026-10-02 umgebaut, in 26.3 von dir bestätigt. Dieselbe Form ist in 1.21.11 und 26.1.2 nachgezogen (dort im Mod-Code) - ebenfalls von dir bestätigt. Dort von dir gemeldet (2026-10-02): die Ketten am Schild sind weiß - behoben (falsche Umriss-Farbe beim Zeichnen, trat nur auf, solange ein leuchtendes Wesen im Bild war), von dir im Spiel bestätigt (2026-10-02)
  - [x] 26.3: 3D-Ärmel am Arm in der Ego-Ansicht - Ursache gefunden und eingebaut (`FirstPersonSleeveMixin`), von dir im Spiel bestätigt (2026-10-02)
  - [x] Sortieren im Kreativmodus duplizierte das Item in der Hand und sortierte nicht - umgebaut (alle drei Versionen), von dir im Spiel bestätigt (2026-10-02)
  - [x] 26.3-Blöcke in 3D: Sulfur Spike, Poplar-Tür, -Falltür, -Boote (Item) und Red Shrub gebaut - von dir im Spiel bestätigt (2026-10-02). Shelf Mushroom und Straw Bed sind schon in Vanilla 3D, der Poplar-Setzling bleibt flach wie alle Setzlinge
  - [x] Flache Icons auch in der Hand (dein Wunsch 2026-10-02): ist „3D-Items" ausgeschaltet, zeigt die Hand jetzt dasselbe wie das Inventar - bei allen Items des Packs, auch bei Falltüren (die waren im ersten Stand noch 3D, von dir gemeldet). Auf dem Boden und im Rahmen bleibt alles 3D. Von dir im Spiel bestätigt (2026-10-02, Tür und Falltür). Schalter heißt jetzt „3D-Items in Inventar & Hand". Alle drei Versionen gebaut, Packs im Dev-Bundle. **Fürs Release:** die Flat-Icons-Packs für 1.21.11 und 26.1.2 sind damit neu (im Manifest steht noch v0.1.4) - beim nächsten Release hochladen und die beiden Einträge hochziehen (nur mit deinem OK)
  - [x] 26.3: Hintergrund im Esc-Menü mit Client-Design war nicht dunkel, man sah die Welt (von dir gemeldet 2026-10-02) - behoben, von dir im Spiel bestätigt (2026-10-02)
  - [ ] Release (nur mit deinem OK): Mod-Jar und Packs hochladen, `versions.26.3` ins Manifest - Runbook Abschnitt B
- [ ] Versionen vor 1.14 (z.B. 1.8.9 für PvP): der Launcher startet alles über Fabric, das geht erst ab 1.14 – bräuchte Start ohne Mod-Loader oder Legacy Fabric plus das alte Startformat
- [ ] Linux und macOS
- [x] Logo am Nametag, damit man andere Client-Nutzer erkennt – mit 0.1.5 ausgeliefert, Backend auf nxlc.de am 2026-10-01 aktualisiert, am eigenen Namensschild live gesehen; Achteck ohne Füllung mit 0.1.6 veröffentlicht. Offen: Test mit anderen Client-Nutzern
- [x] Eigenes Dark-Mode-Texturepack statt Default Dark Mode (CC-BY-NC-SA) – „TNT Dark Mode“ gebaut und im Spiel bestätigt (2026-10-01, beide Versionen), Default Dark Mode lokal komplett entfernt. **Noch nicht veröffentlicht** – kommt mit dem nächsten Release (nur mit deinem OK): Mod- und Launcher-Version auf 0.1.7, `TNT-Dark-Mode-<version>.zip` ans Release hängen, Manifest pushen

- [x] Desktop ion verschiebt sich nach jedem update das müsste gefixt werden
  - Ursache gefunden (2026-10-01): passiert nur, wenn der Installer von Hand über eine bestehende Installation gestartet wird - dann löscht er die Verknüpfung und legt sie neu an, und Windows setzt sie auf den ersten freien Platz. Beim Auto-Update im Launcher bleibt die Verknüpfung unangetastet (so bei 0.1.6). Das ist Verhalten von electron-builder, sobald der Installationsordner wählbar ist (`allowToChangeInstallationDirectory`). Spieler mit Auto-Update sind nicht betroffen. Entscheidung (2026-10-01): bleibt so, die Ordnerwahl im Installer wird dafür nicht aufgegeben

- [x] wenn frecam an und inv auf ist wird über der figur der nametag angezeigt (kann man das auschalten?)
  - Behoben (2026-10-01, beide Mod-Versionen gebaut): die eigene Figur im Inventar bekommt kein Namensschild mehr (`InventoryScreenMixin`). Das Schild über dem stehenden Körper in der Welt bleibt. Von dir im Spiel bestätigt (2026-10-01, beide Versionen). Noch nicht veröffentlicht – kommt mit dem nächsten Release

## 2. Vor einer öffentlichen Veröffentlichung
Derzeit nichts offen. Code-Signing steht unter „3. Eventuell“.

## 3. Eventuell
Ruht seit 2026-10-01, bis du dir Beratung geholt und es mit dem Besitzer des Servers abgesprochen hast – nichts voreilig tun. Bis dahin keine Bewerbung bei SignPath, kein Vertrag und keine Änderung an `PRIVACY.md`.

- [ ] Code-Signing: der Installer ist unsigniert und zeigt deshalb die SmartScreen-Warnung „Unbekannter Herausgeber“. Überlegung: zuerst kostenlos über SignPath Foundation versuchen (Herausgeber wäre „SignPath Foundation“), sonst Certum Open Source (49 €/Jahr, echter Name im Installer). Stand:
  - [x] Auf GitHub: `README.md` (mit Abschnitt „Code signing policy“), `PRIVACY.md`, `.github/workflows/build-windows.yml`
  - [x] Schalter „Freunde und Online-Status“ im Freunde-Screen und Datenschutz-Hinweis als Seite im Installer – live bestätigt, mit 0.1.5 veröffentlicht
  - [x] Build-Workflow auf GitHub gelaufen (beim ersten Versuch grün, rund 5 Minuten: beide Mods und der Installer)
  - [x] Zwei-Faktor-Anmeldung bei GitHub eingeschaltet (Authenticator-App, Wiederherstellungscodes gesichert)
  - [ ] `PRIVACY.md` gegenlesen (du und der Server-Besitzer)
  - [ ] Auf signpath.org/apply bewerben
  - [ ] Nach Zusage: Signieren in den Workflow einbauen und den Release-Ablauf im Runbook umstellen (Release-Build kommt dann von GitHub statt vom eigenen PC)
- [ ] Auftragsverarbeitungsvertrag (AVV) mit dem Besitzer des Servers hinter nxlc.de: Entwurf liegt als Dokument vor (nicht im Repo, weil Namen und Anschriften hineinkommen). Dazu gehört auch, dass `PRIVACY.md` noch keinen Verantwortlichen mit Anschrift nennt
