# Offene Punkte (ohne 3D-Modelle)

Stand 2026-10-09. Zusammengetragen aus `Aktuelle_Phase.md`, `Projekt_Roadmap.md` und `Ideen_für_den_client.md`.
Stand der Veröffentlichung: Launcher 0.14.1, Mod 0.1.14 (2026-10-09); was danach gebaut wird, kommt unter 2a. Erledigtes steht nicht mehr hier, sondern in `Aktuelle_Phase.md`.

## 1. Noch nicht umgesetzt
- [ ] Eigene Mod auf weitere Minecraft-Versionen bringen. Regel seit 2026-10-02: ab 1.14 Fabric, darunter („Legacy“) ohne Mod-Loader mit eigenem Einstieg, kein Forge; pro alter Hauptversion nur die letzte Unterversion. Die vollständige Liste aller Versionen mit ihrem Stand steht in `Minecraft_Versionen.md`
  - [ ] Legacy-Versionen neben 1.8.9: 1.7.10, 1.9.4, 1.10.2, 1.11.2, 1.12.2 und 1.13.2 starten bisher als reines Minecraft ohne unsere Mod
  - [ ] 1.6.4 und älter: im Launcher noch nicht startbar (brauchen das alte Asset-Format und das Session-Argument)
  - [ ] Fabric-Versionen zwischen 1.15 und 1.21.10: starten mit Fabric, aber ohne unsere Mod (1.14.4 hat sie seit 0.1.9, 1.21.1 seit 0.1.10, 1.20.6 seit 0.1.11, 1.21.10 seit 0.1.12, 1.21.8 seit 0.1.14)
- [ ] Eingabefelder in den Fabric-Versionen stehen im Client-Design noch in der Spielschrift (in 1.8.9 in der Client-Schrift) – eigener Umbau, von dir noch nicht entschieden
- [ ] Linux und macOS
- [ ] Sicherheit des Backends (Durchsicht vom 2026-10-09, Umbau vom 2026-10-10) – gebaut und lokal geprüft. Das Backend ist seit 2026-10-10 auf dem Server (mit deinem OK; Sicherung von Datenbank, Unit und altem Code in `~/tntcapes-data/backup-2026-10-10/`) und versteht alte und neue Launcher. Der Launcher-Teil ist in keinem Release und nicht committet. Von dir am 2026-10-10 bestätigt: der installierte Launcher 0.14.1 lädt die Freunde weiter (alter Anmeldeweg), der Dev-Launcher zeigt die echten Freunde über die neue Anmeldung und im Cape-Screen „Gemeldete Capes“. Noch offen: Blockieren und ein Melde-Durchlauf mit zweitem Konto, „Meine Daten löschen“ gegen den echten Server (bewusst nicht ausprobiert). Das Log des Dienstes ist seit 2026-10-10 lesbar (der Besitzer hat unserem Benutzer die Rechte gegeben): `journalctl --user-unit tntcapes.service`; es reicht nur etwa einen halben Tag zurück:
  - Anmeldung ohne Minecraft-Token: der Launcher beweist dem Server über Mojangs Beitritts-Verfahren („join“/„hasJoined“), wer er ist, und bekommt dafür einen eigenen Sitzungsschlüssel, der nur auf nxlc.de gilt (12 Stunden, nur im Arbeitsspeicher). Das Minecraft-Token geht nur noch an Mojang (`backend/src/sessions.ts`, `launcher/src/main/backend/backendSession.ts`). Noch nicht mit einem echten Konto durchgespielt – das geht lokal ohne den Server: Backend lokal starten, Dev-Launcher mit `TNT_BACKEND_URL`
  - Launcher bis 0.14.1 schicken weiter das Token; der Server nimmt es an, bis `TNTCAPES_ACCEPT_ACCESS_TOKENS=0` gesetzt wird. Im Log des Dienstes steht alle zehn Minuten, wie viele Spieler sich noch so angemeldet haben – steht dort nichts mehr, kann der alte Weg zu
  - Konten, denen der Mehrspielermodus gesperrt ist (Xbox-Einstellungen, Sperre durch Mojang), können sich mit dem neuen Verfahren nicht anmelden: keine Freunde, kein eigenes Cape hochladen. Der Launcher sagt das mit eigenem Text
  - Cape-Upload: der Server prüft die ganze Datei (Prüfsummen, Bilddaten entpackt und nachgemessen) und speichert nur das Bild selbst – Text, Metadaten und alles hinter dem Bildende fallen weg. Der Launcher bringt Capes vor dem Hochladen in dieselbe Form. Capes, die schon auf dem Server liegen, sind davon nicht berührt
  - Rate-Limit: `clientIp` nimmt den letzten Eintrag von `X-Forwarded-For` (den hängt Apache an); eigene Grenzen für Anmeldeversuche und für Mojang-Abfragen mit erfundenen Tokens
  - Welt-Einladungen: Server und Launcher nehmen nur Adressen unter `e4mc.link` an (die Mod bekommt Einladungen nur über den Launcher)
  - Der Launcher schickt die Server-Adresse nicht mehr mit, solange „Server verbergen“ an ist oder der Status „unsichtbar“
  - `PRIVACY.md` und `launcher/installer/privacy-notice.txt` sind am 2026-10-10 auf diesen Stand gebracht (Anmeldung ohne Token, Server-Adresse, Blockieren, Daten selbst löschen, Cape-Sperre, verschlüsselte Anmeldung, Abmelden) – sie beschreiben den neuen Launcher und dürfen erst mit dem Release nach `main`. Der Verantwortliche mit Anschrift fehlt weiterhin (siehe 4.)
  - „Abmelden“ löscht die beiden Tokens aus der gespeicherten Anmeldung (`auth.json`) und beendet die Sitzung auf dem Server; bisher blieb alles liegen und das Konto war beim nächsten Start wieder angemeldet. Name, UUID und Skin bleiben, damit der Login-Bildschirm „Offline spielen als <Name>“ anbieten kann (dein Wunsch vom 2026-10-10) – der Knopf ist von dir noch nicht ausprobiert. Die Datei ist jetzt über Windows für das eigene Benutzerkonto verschlüsselt (schützt vor Kopieren der Datei, nicht vor Schadsoftware unter demselben Benutzer). Ein Launcher bis 0.14.1 kann die verschlüsselte Datei nicht lesen und zeigt dann den Login – von dir noch nicht ausprobiert
  - Blockieren im Freunde-Screen (bei Anfragen und Freunden, eigene Liste „Blockiert“ mit „Freigeben“): entfernt Freundschaft, Anfragen und Einladungen; Anfragen des Blockierten sehen für ihn aus, als gäbe es den Spieler nicht. Serverlogik mit Test-Datenbank geprüft, im Fenster von dir noch nicht angesehen – zum Ausprobieren braucht es ein zweites Konto
  - „Meine Daten vom Server löschen…“ im Freunde-Screen: löscht Freunde, Anfragen, Blockierte, Status, den Eintrag als Client-Nutzer und das aktive Cape, schaltet Freunde aus
  - Cape-Moderation von Hand auf dem Server: `node dist/admin.js cape-remove|cape-ban|cape-unban|cape-bans`; ein gesperrtes Konto bekommt beim Upload einen eigenen Text, die Sperre bleibt auch nach „Daten löschen“
  - Capes melden (dein Wunsch vom 2026-10-10): im Cape-Screen „Cape melden“ mit Minecraft-Name und freiwilligem Grund; der Server sichert das Cape, wie es beim Melden war. Moderatoren (Konten in `TNTCAPES_MODERATORS`, bisher nur theTNTde) sehen darunter „Gemeldete Capes“ mit Bild und können verwerfen, das Cape entfernen oder das Konto sperren und wieder entsperren. Serverlogik mit Test-Datenbank geprüft, im Fenster von dir noch nicht angesehen
  - Melden bleibt im Launcher, kein Knopf im Spiel (deine Entscheidung vom 2026-10-10). Dafür im Cape-Screen ein Abschnitt „Regeln für Capes“ und zwei neue Schritte in der Launcher-Tour („Regeln für Capes“, „Cape melden“). Der Regeltext (anstößig, illegal, oder passt nicht zum Client) ist von dir am 2026-10-10 bestätigt
  - Platzbremse (dein Wunsch vom 2026-10-10, seit dem zweiten Deploy am selben Tag auf dem Server; Sicherung davor in `~/tntcapes-data/backup-2026-10-10-b/`): keine Uploads und keine Kopien gemeldeter Capes mehr, sobald auf der Platte weniger als 500 MB frei sind; alle Capes zusammen höchstens 1 GB, die Kopien höchstens 200 MB (`backend/src/storage.ts`, per Umgebungsvariable änderbar). Die Cape-Grenze bleibt bei 5 MB – erst senken, falls die Nutzerzahlen es nötig machen. Stand der Platte am 2026-10-10: 2,5 GB von 50 GB frei, wir belegen 216 MB (davon 203 MB Node.js)
  - Datenordner des Dienstes nur noch für den eigenen Benutzer lesbar, Dienst mit Speicher- und Prozessgrenze (`backend/deploy/tntcapes.service`)

## 2. Noch von dir zu prüfen (schon veröffentlicht)
- [ ] Auto-Update des installierten Launchers auf 0.1.11
- [ ] Eine 1.20.6-Instanz im installierten Launcher (0.1.11): lädt er das Bundle übers Manifest (Mods und Ressourcenpakete), startet sie, verbindet Continuity die Texturen? Indium läuft seit 0.1.11 immer mit und darf im Mods-Screen nicht als Schalter auftauchen - das ist neuer Launcher-Code, den du noch nicht gesehen hast
- [ ] Eine 1.21.1-Instanz im installierten Launcher: lädt er das Bundle übers Manifest (Mods und Ressourcenpakete), startet sie, funktionieren die Schalter? Getestet war 1.21.1 bisher nur im Dev-Launcher mit lokal befülltem Bundle
- [ ] Hinweis im Launcher bei 1.21.2, 1.21.9, 26.1 und 26.1.1 (beim Anlegen einer Instanz und auf dem Startbildschirm) - gebaut, im Fenster noch von niemandem angesehen
- [ ] Helligkeit mit Fullbright in 1.21.1 und 1.21.11: Fullbright einschalten, Spiel beenden, neu starten und Fullbright ausschalten - die eigene Helligkeit muss noch dieselbe sein, im Log kein „Error saving option Brightness“ mehr

- [ ] Neues 3D-Blöcke-Paket in einer schon vorhandenen Instanz: bietet der installierte Launcher das Paket als Update an, und liegt der Item-Rahmen danach flach in der Hand?
- [ ] Launcher-Hinweis auch bei 1.21.6 und 1.21.7 (verweist auf 1.21.8) - seit 0.1.11, im Fenster noch nicht angesehen

## 2a. Wartet auf das nächste Release
Derzeit nichts. Der Stolperdraht-Haken wird mit 3D-Items weiter wie ein Block gehalten (der Item-Rahmen seit 0.1.14 flach) - sag Bescheid, falls der auch stört.

## 2b. Vor der 1.0 zu testen
Die 1.0 ist die Version, in der alles umgesetzt ist, was du beim Client haben wolltest – sie steht noch nicht an. Diese Tests sind einer der letzten Schritte vor ihrer Veröffentlichung (dein Vorschlag vom 2026-10-03).
- [ ] Multiplayer-Test mit einem zweiten Client-Nutzer auf demselben Server, beides in einem Durchgang:
  - [ ] Capes (1.14.4, 1.21.1, 1.21.11, 26.1.2, 26.3): sehen beide gegenseitig ihre Capes?
  - [ ] Logo am Nametag (1.14.4, 1.21.1, 1.21.11, 26.1.2, 26.3): sehen beide das Client-Logo am Namen des anderen, stehend und geduckt, in den richtigen Farben?

## 3. Vor einer öffentlichen Veröffentlichung
Code-Signing steht unter „4. Eventuell“. Aus der Sicherheits-Durchsicht des Backends vom 2026-10-09:

- [ ] Alten Anmeldeweg (Minecraft-Token) auf dem Server abschalten, sobald kein Launcher bis 0.14.1 mehr unterwegs ist
- [ ] Mit dem Besitzer des Servers zu klären (liegt nicht in unserer Hand): HSTS für nxlc.de, Sicherung der Freunde-Datenbank. Geklärt am 2026-10-10: Apache bewahrt die Zugriffslogs 14 Tage auf (steht jetzt in `PRIVACY.md`)
- Bewusst so: jedes Minecraft-Konto kann über `/players/lookup` oder eine Freundesanfrage erfahren, ob ein Spieler den Client nutzt (nötig fürs Logo am Nametag)

## 4. Eventuell
Ruht seit 2026-10-01, bis du dir Beratung geholt und es mit dem Besitzer des Servers abgesprochen hast – nichts voreilig tun. Bis dahin keine Bewerbung bei SignPath, kein Vertrag und keine Änderung an `PRIVACY.md`.

- [ ] Code-Signing: der Installer ist unsigniert und zeigt deshalb die SmartScreen-Warnung „Unbekannter Herausgeber“. Überlegung: zuerst kostenlos über SignPath Foundation versuchen (Herausgeber wäre „SignPath Foundation“), sonst Certum Open Source (49 €/Jahr, echter Name im Installer). Noch offen:
  - [ ] `PRIVACY.md` gegenlesen (du und der Server-Besitzer)
  - [ ] Auf signpath.org/apply bewerben
  - [ ] Nach Zusage: Signieren in den Workflow einbauen und den Release-Ablauf im Runbook umstellen (Release-Build kommt dann von GitHub statt vom eigenen PC)
- [ ] Auftragsverarbeitungsvertrag (AVV) mit dem Besitzer des Servers hinter nxlc.de: Entwurf liegt als Dokument vor (nicht im Repo, weil Namen und Anschriften hineinkommen). Dazu gehört auch, dass `PRIVACY.md` noch keinen Verantwortlichen mit Anschrift nennt
