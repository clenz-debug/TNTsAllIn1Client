# Offene Punkte (ohne 3D-Modelle)

Stand 2026-11-05. Zusammengetragen aus `Aktuelle_Phase.md`, `Projekt_Roadmap.md` und `Ideen_für_den_client.md`.
Stand der Veröffentlichung: Launcher und Mod 0.17.0 (2026-10-19); was danach gebaut wird, kommt unter 2a. Erledigtes steht nicht mehr hier, sondern in `Aktuelle_Phase.md`.

## 1. Noch nicht umgesetzt
- [ ] Eigene Mod auf weitere Minecraft-Versionen bringen. Regel seit 2026-10-02: ab 1.14 Fabric, darunter („Legacy“) ohne Mod-Loader mit eigenem Einstieg, kein Forge; pro alter Hauptversion nur die letzte Unterversion. Die vollständige Liste aller Versionen mit ihrem Stand steht in `Minecraft_Versionen.md`
  - [ ] Legacy-Versionen neben 1.8.9: 1.7.10, 1.9.4, 1.10.2, 1.11.2, 1.12.2 und 1.13.2 starten bisher als reines Minecraft ohne unsere Mod
  - [ ] 1.6.4 und älter: im Launcher noch nicht startbar (brauchen das alte Asset-Format und das Session-Argument)
  - [ ] Fabric-Versionen zwischen 1.15 und 1.21.10: starten mit Fabric, aber ohne unsere Mod (1.17.1 ist gebaut und bestätigt, siehe 2a; 1.14.4 hat sie seit 0.1.9, 1.21.1 seit 0.1.10, 1.20.6 seit 0.1.11, 1.21.10 seit 0.1.12, 1.21.8 seit 0.1.14; 1.21.5, 1.21.4, 1.21.3 und 1.20.1 seit 0.16.0; 1.19.4, 1.19.2 und 1.18.2 seit 0.17.0)
- [ ] Linux und macOS

## 2. Noch von dir zu prüfen (schon veröffentlicht)
Zurzeit nichts. Alles aus Release 0.17.0 hast du am 2026-11-05 bestätigt (Auto-Update, die drei neuen Versionen im installierten Launcher, Paket-Update, die beiden Launcher-Hinweise).

## 2a. Wartet auf das nächste Release
- [x] 1.17.1-Port (`mod/1.17.1`, Java 16) mit Launcher-Hinweis bei 1.17: von dir am 2026-11-07 im Dev-Launcher bestätigt. Für das Release: Manifest-Eintrag `versions.1.17.1` mit Fabric API, Sodium, Indium, Lithium, Continuity und den vier Paketen, ohne e4mc (Einzelheiten in `Aktuelle_Phase.md`). Der Eintrag liegt seit 2026-11-07 vorbereitet und **uncommittet** in `mod-bundle-manifest.json` (Links auf `v0.18.0`, Pakete mit SHA-1); beim Release noch `ownMod.sha1` aus dem hochgeladenen Jar eintragen, erst dann committen und pushen
- [x] Waypoint-Beam bei großer Entfernung (über die Fernebene hinaus abgeschnitten): in allen Versionen von 1.18.2 bis 26.3 behoben, von dir am 2026-11-05 bestätigt. Kommt mit den neuen Mod-Jars dieser Versionen

## 2b. Vor der 1.0 zu testen
Die 1.0 ist die Version, in der alles umgesetzt ist, was du beim Client haben wolltest – sie steht noch nicht an. Diese Tests sind einer der letzten Schritte vor ihrer Veröffentlichung (dein Vorschlag vom 2026-10-03).
- [ ] Multiplayer-Test mit einem zweiten Client-Nutzer auf demselben Server, beides in einem Durchgang:
  - [ ] Capes (1.14.4, 1.21.1, 1.21.4, 1.21.11, 26.1.2, 26.3): sehen beide gegenseitig ihre Capes? In 1.21.4 bisher nur im Einzelspieler gesehen
  - [ ] Einladung in die eigene Welt (e4mc) in 1.21.4: kommt die Einladung beim Freund an und kann er beitreten?
  - [ ] Logo am Nametag (1.14.4, 1.21.1, 1.21.11, 26.1.2, 26.3): sehen beide das Client-Logo am Namen des anderen, stehend und geduckt, in den richtigen Farben?

## 2c. Backend angehalten wegen php2shell (2026-10-22 bis 2026-10-26, läuft wieder)
Auf dem Server von nxlc.de läuft PHP 7.4 (`7.4.33-1+deb11u9`, Debian 11) mit PHP-FPM für alle Seiten. Gegen die PHP-Lücke php2shell (CVE-2026-109618/109619, Angriffe seit etwa 17.10.) gibt es für 7.4 auf Debian 11 nur über das kostenpflichtige Freexian ELTS einen Fix. Du hast den Admin am 2026-10-22 informiert. Zum Schutz der Nutzer ist unser Dienst auf deinen Wunsch angehalten und nicht mehr automatisch gestartet (`systemctl --user stop` + `disable tntcapes.service`). Bis dahin gibt es keine Freunde, keine Einladungen und keinen Cape-Upload. Die vorhandenen Capes werden weiter angezeigt (Apache liefert sie direkt aus).
- Stand 2026-10-25: PHP war noch unverändert da. Der Admin: „hab ich schon längst aber irgendwie gibt es einen fehler … ich setze ein neues server image auf“. Unsere Daten sind deshalb vorher gesichert, nach `A:\tntcapes-backup-2026-10-25` (außerhalb des Repos; `tntcapes-data` mit Datenbank samt `-wal`/`-shm` und `www` mit den aktiven Capes, 47 Dateien, Prüfsummen mit dem Server verglichen, Datenbank-Prüfung „ok“)
- Stand 2026-10-26: noch kein neues Image. PHP 7.4 ist weiter installiert (`deb11u9`), aber `php7.4-fpm` ist gestoppt und deaktiviert, und Apache reicht kein PHP mehr weiter. Damit ist die Lücke von außen zu. Unser Dienst ist weiter aus
- Stand 2026-10-26, später: Der Admin geht von Debian 11 auf 12 (Upgrade, kein neues Image, unsere Daten sind noch da). Das Update auf `deb11u12` hat `php7.4-fpm` wieder eingeschaltet, also läuft PHP 7.4 wieder, und `deb11u12` schließt php2shell nicht
- [x] Nach dem Upgrade geprüft (2026-10-26, lesend): Debian 12.15, PHP 7.4 komplett weg, stattdessen PHP 8.2.35 (`deb12u1`, schließt laut Changelog CVE-2026-109618/109619), `bookworm-security` eingetragen. Unsere Daten unverändert (gleiche SHA-1 wie die Sicherung), nichts zurückzuspielen. Der Admin hat unseren Dienst um 18:21 selbst wieder eingeschaltet (als root, `enabled` + läuft). `/app/health` antwortet 200, Capes 200
- [x] Alter Anmeldeweg abgeschaltet (2026-10-26, mit deinem OK): `TNTCAPES_ACCEPT_ACCESS_TOKENS=0` in `backend/deploy/tntcapes.service`, Unit auf den Server gelegt (alte gesichert als `~/tntcapes-data/tntcapes.service.bak-2026-10-26`), Dienst neu gestartet. Launcher vor 0.15.0 kommen nicht mehr an Freunde/Cape-Upload
- [x] Cape-Upload mit dem neuen Anmeldeweg geht: 2026-10-26 20:09 hat theTNTde ein neues Cape aktiviert (512x256, im Server-Log und unter nxlc.de/tntcapes/ zu sehen)
- [x] Freundesliste und der Rest im Launcher: vom Nutzer bestätigt („alles klappt“, 2026-10-26)

## 3. Vor einer öffentlichen Veröffentlichung
Code-Signing steht unter „4. Eventuell“. Aus der Sicherheits-Durchsicht des Backends vom 2026-10-09:

- [ ] Mit dem Besitzer des Servers zu klären (liegt nicht in unserer Hand): Sicherung der Freunde-Datenbank. HSTS für nxlc.de hat der Besitzer am 2026-10-13 eingeschaltet (von außen geprüft: ein Jahr, ohne Unterdomains). Geklärt am 2026-10-10: Apache bewahrt die Zugriffslogs 14 Tage auf (steht jetzt in `PRIVACY.md`)
- Bewusst so: jedes Minecraft-Konto kann über `/players/lookup` oder eine Freundesanfrage erfahren, ob ein Spieler den Client nutzt (nötig fürs Logo am Nametag)

## 4. Eventuell
Ruht seit 2026-10-01, bis du dir Beratung geholt und es mit dem Besitzer des Servers abgesprochen hast – nichts voreilig tun. Bis dahin keine Bewerbung bei SignPath, kein Vertrag und keine Änderung an `PRIVACY.md`.

- [ ] Code-Signing: der Installer ist unsigniert und zeigt deshalb die SmartScreen-Warnung „Unbekannter Herausgeber“. Überlegung: zuerst kostenlos über SignPath Foundation versuchen (Herausgeber wäre „SignPath Foundation“), sonst Certum Open Source (49 €/Jahr, echter Name im Installer). Noch offen:
  - [ ] `PRIVACY.md` gegenlesen (du und der Server-Besitzer)
  - [ ] Auf signpath.org/apply bewerben
  - [ ] Nach Zusage: Signieren in den Workflow einbauen und den Release-Ablauf im Runbook umstellen (Release-Build kommt dann von GitHub statt vom eigenen PC)
- [ ] Auftragsverarbeitungsvertrag (AVV) mit dem Besitzer des Servers hinter nxlc.de: Entwurf liegt als Dokument vor (nicht im Repo, weil Namen und Anschriften hineinkommen). Dazu gehört auch, dass `PRIVACY.md` noch keinen Verantwortlichen mit Anschrift nennt
