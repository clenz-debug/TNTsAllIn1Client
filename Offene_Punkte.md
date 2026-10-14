# Offene Punkte (ohne 3D-Modelle)

Stand 2026-10-14. Zusammengetragen aus `Aktuelle_Phase.md`, `Projekt_Roadmap.md` und `Ideen_für_den_client.md`.
Stand der Veröffentlichung: Launcher 0.15.0 (2026-10-13), Mod 0.1.14 (2026-10-09); was danach gebaut wird, kommt unter 2a. Erledigtes steht nicht mehr hier, sondern in `Aktuelle_Phase.md`.

## 1. Noch nicht umgesetzt
- [ ] Eigene Mod auf weitere Minecraft-Versionen bringen. Regel seit 2026-10-02: ab 1.14 Fabric, darunter („Legacy“) ohne Mod-Loader mit eigenem Einstieg, kein Forge; pro alter Hauptversion nur die letzte Unterversion. Die vollständige Liste aller Versionen mit ihrem Stand steht in `Minecraft_Versionen.md`
  - [ ] Legacy-Versionen neben 1.8.9: 1.7.10, 1.9.4, 1.10.2, 1.11.2, 1.12.2 und 1.13.2 starten bisher als reines Minecraft ohne unsere Mod
  - [ ] 1.6.4 und älter: im Launcher noch nicht startbar (brauchen das alte Asset-Format und das Session-Argument)
  - [ ] Fabric-Versionen zwischen 1.15 und 1.21.10: starten mit Fabric, aber ohne unsere Mod (1.14.4 hat sie seit 0.1.9, 1.21.1 seit 0.1.10, 1.20.6 seit 0.1.11, 1.21.10 seit 0.1.12, 1.21.8 seit 0.1.14; 1.21.5, 1.21.4, 1.21.3 und 1.20.1 sind fertig und warten auf das Release, siehe 1a)
- [ ] Eingabefelder in den Fabric-Versionen stehen im Client-Design noch in der Spielschrift (in 1.8.9 in der Client-Schrift) – eigener Umbau, von dir noch nicht entschieden
- [ ] Linux und macOS
- [ ] Sicherheit des Backends (Durchsicht vom 2026-10-09, Umbau vom 2026-10-10) – gebaut und lokal geprüft. Das Backend ist seit 2026-10-10 auf dem Server (mit deinem OK; Sicherung von Datenbank, Unit und altem Code in `~/tntcapes-data/backup-2026-10-10/`) und versteht alte und neue Launcher. Der Launcher-Teil ist seit 2026-10-13 mit Launcher 0.15.0 veröffentlicht. Offen bleibt hier nur noch das Abschalten des alten Anmeldewegs (siehe 3.) und die Punkte unter 2. Von dir am 2026-10-10 bestätigt: der installierte Launcher 0.14.1 lädt die Freunde weiter (alter Anmeldeweg), der Dev-Launcher zeigt die echten Freunde über die neue Anmeldung und im Cape-Screen „Gemeldete Capes“. Von dir am 2026-10-13 im Fenster bestätigt, gegen ein lokal gestartetes Backend mit Testdatenbank und drei erfundenen Mitspielern (kein zweites Konto nötig): Blockieren und Freigeben, Cape melden, als Moderator entscheiden, „Meine Daten löschen“. Nicht ausprobiert und nur auf Serverebene geprüft: die Sicht eines blockierten Spielers; „Meine Daten löschen“ gegen den echten Server (bewusst nicht). Das Log des Dienstes ist seit 2026-10-10 lesbar (der Besitzer hat unserem Benutzer die Rechte gegeben): `journalctl --user-unit tntcapes.service`; es reicht nur etwa einen halben Tag zurück:
  - Anmeldung ohne Minecraft-Token: der Launcher beweist dem Server über Mojangs Beitritts-Verfahren („join“/„hasJoined“), wer er ist, und bekommt dafür einen eigenen Sitzungsschlüssel, der nur auf nxlc.de gilt (12 Stunden, nur im Arbeitsspeicher). Das Minecraft-Token geht nur noch an Mojang (`backend/src/sessions.ts`, `launcher/src/main/backend/backendSession.ts`). Von dir am 2026-10-10 mit deinem Konto bestätigt, erst gegen ein lokal gestartetes Backend (Dev-Launcher mit `TNT_BACKEND_URL`), dann gegen den echten Server
  - Launcher bis 0.14.1 schicken weiter das Token; der Server nimmt es an, bis `TNTCAPES_ACCEPT_ACCESS_TOKENS=0` gesetzt wird. Im Log des Dienstes steht alle zehn Minuten, wie viele Spieler sich noch so angemeldet haben – steht dort nichts mehr, kann der alte Weg zu. Weil das Log nur etwa einen halben Tag zurückreicht, schreibt der Dienst die Zahl zusätzlich pro Tag in `~/tntcapes-data/access-token-sign-ins.txt` (nur Datum und Anzahl, keine Namen; `backend/src/accessTokenSignIns.ts`) – am 2026-10-13 gebaut, lokal geprüft und mit deinem OK deployt (Sicherung in `~/tntcapes-data/backup-2026-10-13/`); die Datei entsteht mit der ersten Anmeldung über den alten Weg
  - Konten, denen der Mehrspielermodus gesperrt ist (Xbox-Einstellungen, Sperre durch Mojang), können sich mit dem neuen Verfahren nicht anmelden: keine Freunde, kein eigenes Cape hochladen. Der Launcher sagt das mit eigenem Text
  - Cape-Upload: der Server prüft die ganze Datei (Prüfsummen, Bilddaten entpackt und nachgemessen) und speichert nur das Bild selbst – Text, Metadaten und alles hinter dem Bildende fallen weg. Der Launcher bringt Capes vor dem Hochladen in dieselbe Form. Capes von vor dem Umbau gibt es auf dem Server nicht mehr in ungeprüfter Form (geprüft am 2026-10-13): das eine verbliebene ist schon Byte für Byte in der bereinigten Form, dein eigenes hast du am 2026-10-11 selbst deaktiviert
  - Rate-Limit: `clientIp` nimmt den letzten Eintrag von `X-Forwarded-For` (den hängt Apache an); eigene Grenzen für Anmeldeversuche und für Mojang-Abfragen mit erfundenen Tokens
  - Welt-Einladungen: Server und Launcher nehmen nur Adressen unter `e4mc.link` an (die Mod bekommt Einladungen nur über den Launcher)
  - Der Launcher schickt die Server-Adresse nicht mehr mit, solange „Server verbergen“ an ist oder der Status „unsichtbar“
  - `PRIVACY.md` und `launcher/installer/privacy-notice.txt` sind am 2026-10-10 auf diesen Stand gebracht (Anmeldung ohne Token, Server-Adresse, Blockieren, Daten selbst löschen, Cape-Sperre, verschlüsselte Anmeldung, Abmelden) – sie sind auf deinen Wunsch schon auf `main` und nennen damit Funktionen, die es erst mit dem Release gibt. Der Verantwortliche mit Anschrift fehlt weiterhin (siehe 4.)
  - „Abmelden“ löscht die beiden Tokens aus der gespeicherten Anmeldung (`auth.json`) und beendet die Sitzung auf dem Server; bisher blieb alles liegen und das Konto war beim nächsten Start wieder angemeldet. Name, UUID und Skin bleiben, damit der Login-Bildschirm „Offline spielen als <Name>“ anbieten kann (dein Wunsch vom 2026-10-10) – von dir am 2026-10-13 bestätigt: der Knopf startet eine Instanz. Die Datei ist jetzt über Windows für das eigene Benutzerkonto verschlüsselt (schützt vor Kopieren der Datei, nicht vor Schadsoftware unter demselben Benutzer). Ein Launcher bis 0.14.1 kann die verschlüsselte Datei nicht lesen und zeigt dann den Login – von dir noch nicht ausprobiert
  - Blockieren im Freunde-Screen (bei Anfragen und Freunden, eigene Liste „Blockiert“ mit „Freigeben“): entfernt Freundschaft, Anfragen und Einladungen; Anfragen des Blockierten sehen für ihn aus, als gäbe es den Spieler nicht. Serverlogik mit Test-Datenbank geprüft, im Fenster von dir am 2026-10-13 bestätigt (lokales Test-Backend)
  - „Meine Daten vom Server löschen…“ im Freunde-Screen: löscht Freunde, Anfragen, Blockierte, Status, den Eintrag als Client-Nutzer und das aktive Cape, schaltet Freunde aus. Seit 2026-10-13 gibt es dasselbe auch in den Einstellungen als eigenen Bereich „Meine Daten auf dem Server“ mit einem eigenen Tour-Schritt (dein Wunsch). Das Löschen ist von dir am 2026-10-13 bestätigt (lokales Test-Backend)
  - Cape-Moderation von Hand auf dem Server: `node dist/admin.js cape-remove|cape-ban|cape-unban|cape-bans`; ein gesperrtes Konto bekommt beim Upload einen eigenen Text, die Sperre bleibt auch nach „Daten löschen“
  - Capes melden (dein Wunsch vom 2026-10-10): im Cape-Screen „Cape melden“ mit Minecraft-Name und freiwilligem Grund; der Server sichert das Cape, wie es beim Melden war. Moderatoren (Konten in `TNTCAPES_MODERATORS`, bisher nur theTNTde) sehen darunter „Gemeldete Capes“ mit Bild und können verwerfen, das Cape entfernen oder das Konto sperren und wieder entsperren (Sperren löscht das aktive Cape gleich mit). Serverlogik mit Test-Datenbank geprüft, im Fenster von dir am 2026-10-13 bestätigt (lokales Test-Backend)
  - Melden bleibt im Launcher, kein Knopf im Spiel (deine Entscheidung vom 2026-10-10). Dafür im Cape-Screen ein Abschnitt „Regeln für Capes“ und zwei neue Schritte in der Launcher-Tour („Regeln für Capes“, „Cape melden“), dazu seit 2026-10-13 „Deine Daten auf dem Server“ bei den Einstellungen – alle drei von dir am 2026-10-13 im Fenster bestätigt. Der Regeltext (anstößig, illegal, oder passt nicht zum Client) ist von dir am 2026-10-10 bestätigt
  - Platzbremse (dein Wunsch vom 2026-10-10, seit dem zweiten Deploy am selben Tag auf dem Server; Sicherung davor in `~/tntcapes-data/backup-2026-10-10-b/`): keine Uploads und keine Kopien gemeldeter Capes mehr, sobald auf der Platte weniger als 500 MB frei sind; alle Capes zusammen höchstens 1 GB, die Kopien höchstens 200 MB (`backend/src/storage.ts`, per Umgebungsvariable änderbar). Die Cape-Grenze bleibt bei 5 MB – erst senken, falls die Nutzerzahlen es nötig machen. Stand der Platte am 2026-10-10: 2,6 GB von 50 GB frei. Wir belegen 132 MB, seit du Node.js auf das Programm selbst gekürzt und die Sicherung vom 01.10. gelöscht hast (vorher 216 MB); `deploy.sh` kürzt eine künftige Node.js-Installation gleich mit
  - Datenordner des Dienstes nur noch für den eigenen Benutzer lesbar, Dienst mit Speicher- und Prozessgrenze (`backend/deploy/tntcapes.service`)

## 1a. 1.21.5-Port: wartet auf das Sammel-Release
Von dir am 2026-10-13 im Dev-Launcher getestet und bestätigt („damit ist alles getestet, es hat alles geklappt“) - Einzelheiten in `Aktuelle_Phase.md`. Committet und gepusht am 2026-10-13, ohne Manifest-Eintrag.
Das Release kommt nach deiner Vorgabe als ein großes Update-Paket zusammen mit den restlichen 1.21er-Versionen und der 1.20.1:
- [x] 1.21.5
- [x] 1.21.4 – von dir am 2026-10-14 im Dev-Launcher getestet und bestätigt; Welt-Einladung und Capes aus Sicht eines anderen Spielers stehen unter 2b
- [x] 1.21.3 – von dir am 2026-10-14 im Dev-Launcher getestet und bestätigt; einzige Meldung war die Scrollleiste der langen Einstellungsseiten im Client-Design (alle Versionen, behoben und von dir bestätigt)
- [x] 1.20.1 – von dir am 2026-10-14 im Dev-Launcher getestet und bestätigt („es passt sowohl das neugemachte als auch das was schon davor gemacht wurde also alles“), samt den Korrekturen für die Knopfschrift und die Textfelder
- [ ] Übergang zwischen Client- und Minecraft-Design in 1.20.6 und 1.21.1: dort dieselbe Korrektur wie in 1.21.5 (Knopf-Text schien durch die Animation), von dir dort noch nicht angesehen
- [ ] Nur mit deinem OK: Release mit den Manifest-Einträgen der neuen Versionen

## 2. Noch von dir zu prüfen (schon veröffentlicht)
- [ ] Ein Launcher bis 0.14.1 kann die verschlüsselte Anmeldung nicht lesen und zeigt dann den Login – betrifft nur, wer nach dem Update wieder eine alte Version installiert; im Fenster nicht ausprobiert
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
  - [ ] Capes (1.14.4, 1.21.1, 1.21.4, 1.21.11, 26.1.2, 26.3): sehen beide gegenseitig ihre Capes? In 1.21.4 bisher nur im Einzelspieler gesehen
  - [ ] Einladung in die eigene Welt (e4mc) in 1.21.4: kommt die Einladung beim Freund an und kann er beitreten?
  - [ ] Logo am Nametag (1.14.4, 1.21.1, 1.21.11, 26.1.2, 26.3): sehen beide das Client-Logo am Namen des anderen, stehend und geduckt, in den richtigen Farben?

## 3. Vor einer öffentlichen Veröffentlichung
Code-Signing steht unter „4. Eventuell“. Aus der Sicherheits-Durchsicht des Backends vom 2026-10-09:

- [ ] Alten Anmeldeweg (Minecraft-Token) auf dem Server abschalten, sobald kein Launcher bis 0.14.1 mehr unterwegs ist – abzulesen an `~/tntcapes-data/access-token-sign-ins.txt` auf dem Server (eine Zeile pro Tag mit der Zahl der Spieler); Launcher 0.15.0 ist seit 2026-10-13 draußen
- [ ] Mit dem Besitzer des Servers zu klären (liegt nicht in unserer Hand): Sicherung der Freunde-Datenbank. HSTS für nxlc.de hat der Besitzer am 2026-10-13 eingeschaltet (von außen geprüft: ein Jahr, ohne Unterdomains). Geklärt am 2026-10-10: Apache bewahrt die Zugriffslogs 14 Tage auf (steht jetzt in `PRIVACY.md`)
- Bewusst so: jedes Minecraft-Konto kann über `/players/lookup` oder eine Freundesanfrage erfahren, ob ein Spieler den Client nutzt (nötig fürs Logo am Nametag)

## 4. Eventuell
Ruht seit 2026-10-01, bis du dir Beratung geholt und es mit dem Besitzer des Servers abgesprochen hast – nichts voreilig tun. Bis dahin keine Bewerbung bei SignPath, kein Vertrag und keine Änderung an `PRIVACY.md`.

- [ ] Code-Signing: der Installer ist unsigniert und zeigt deshalb die SmartScreen-Warnung „Unbekannter Herausgeber“. Überlegung: zuerst kostenlos über SignPath Foundation versuchen (Herausgeber wäre „SignPath Foundation“), sonst Certum Open Source (49 €/Jahr, echter Name im Installer). Noch offen:
  - [ ] `PRIVACY.md` gegenlesen (du und der Server-Besitzer)
  - [ ] Auf signpath.org/apply bewerben
  - [ ] Nach Zusage: Signieren in den Workflow einbauen und den Release-Ablauf im Runbook umstellen (Release-Build kommt dann von GitHub statt vom eigenen PC)
- [ ] Auftragsverarbeitungsvertrag (AVV) mit dem Besitzer des Servers hinter nxlc.de: Entwurf liegt als Dokument vor (nicht im Repo, weil Namen und Anschriften hineinkommen). Dazu gehört auch, dass `PRIVACY.md` noch keinen Verantwortlichen mit Anschrift nennt
