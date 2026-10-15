# Minecraft-Versionen: was unsere Mod schon kann und was noch offen ist

Stand 2026-10-14 (Launcher und Mod 0.16.0). Grundlage: Mojangs Versionsliste (nur Vollversionen, keine
Snapshots, kein Alpha/Beta) und Fabrics Liste der Versionen mit Mod-Loader, beide am 2026-10-08
abgerufen.

## Welche Versionen hier stehen

- **Vor 1.21.2:** pro Hauptversion nur die letzte, stabile Unterversion (z. B. 1.7.10 statt der ganzen
  1.7er-Reihe) – die Unterversionen unterscheiden sich dort für eine Mod kaum.
- **Ab 1.21.2:** jede Unterversion einzeln. Seit Mojangs „Drops" (Oktober 2024) bringt jede
  Unterversion eigene Inhalte und eigene Brüche mit, sie ist also jeweils eine Version für sich.
- **Bauart** (Regel seit 2026-10-02): ab 1.14 Fabric, davor „Legacy" ohne Mod-Loader mit eigenem
  Einstieg, kein Forge.

Wird eine Version fertig und veröffentlicht, wandert sie von „Offen" nach „Unterstützt".

## Unterstützt

| Version | Erschienen | Bauart | Seit |
|---|---|---|---|
| 1.8.9 | 2015-12-03 | Legacy (nur im Installer, kein Manifest-Eintrag) | 0.1.8 |
| 1.14.4 | 2019-07-19 | Fabric | 0.1.9 |
| 1.20.1 | 2023-06-12 | Fabric (Modpack-Version, neben 1.20.6) | 0.16.0 |
| 1.20.6 | 2024-04-29 | Fabric | 0.1.11 |
| 1.21.1 | 2024-08-08 | Fabric | 0.1.10 |
| 1.21.3 | 2024-10-23 | Fabric (deckt 1.21.2 mit ab) | 0.16.0 |
| 1.21.4 | 2024-12-03 | Fabric | 0.16.0 |
| 1.21.5 | 2025-03-25 | Fabric | 0.16.0 |
| 1.21.8 | 2025-07-17 | Fabric (deckt 1.21.6 und 1.21.7 mit ab) | 0.1.14 |
| 1.21.10 | 2025-10-07 | Fabric (deckt 1.21.9 mit ab) | 0.1.12 |
| 1.21.11 | 2025-12-09 | Fabric | von Anfang an |
| 26.1.2 | 2026-04-09 | Fabric | von Anfang an |
| 26.2 | 2026-06-16 | Fabric | 0.1.13 |
| 26.3 | 2026-09-15 | Fabric | 0.1.7 |

## Offen

### Ab 1.21.2 – jede Unterversion einzeln (Fabric)

Seit 0.16.0 (2026-10-14) offen: keine – 1.21.3, 1.21.4 und 1.21.5 sind unter „Unterstützt“.

Entschieden am 2026-10-08: 1.21.2, 1.21.6, 1.21.7, 1.21.9, 26.1 und 26.1.1 bekommen keine eigene
Mod-Version – ihre Fehlerbehebungen 1.21.3, 1.21.8, 1.21.10 und 26.1.2 kamen kurz danach und sind die
stabileren Stände. Der Launcher weist bei diesen sechs Versionen darauf hin
(`SUPERSEDED_MINECRAFT_VERSIONS` in `launcher/src/shared/types.ts`; 1.21.6 und 1.21.7 seit 0.1.11).

Fremd-Mods für 1.21.5 (Modrinth, 2026-10-13, im Manifest seit 0.16.0): Fabric API 0.128.2 (`kKEGlsne`), Sodium 0.6.13
(`DA250htH`), Lithium 0.16.3 (`xcELvp6R`), Continuity 3.0.1-test.4 (`kravz06U`), e4mc 6.2.1 (`k8wW4lF5`) und Bushy
Vegetation 3.4.2 (`aLE579XT`, dieselbe Datei wie für 1.21.8). Cape Provider gibt es nur als 1.1.1, die keine Cape-Adresse
aus einer anderen Mod übernimmt – die Mod bringt die Capes dort selbst mit, wie in 1.20.6.

Fremd-Mods für 1.21.4 (Modrinth, 2026-10-14, im Manifest seit 0.16.0): Fabric API 0.119.4 (`p96k10UR`), Sodium 0.6.13
(`c3YkZvne`), Lithium 0.15.3 (`u8pHPXJl`), Continuity 3.0.0 (`esC1Vejm`), e4mc 6.2.1 (`k8wW4lF5`) und Bushy Vegetation 3.3.2
(`rIzHhAqW`, dieselbe Datei wie für 1.21.1). Cape Provider gibt es nur bis 1.0.2 – die Mod bringt die Capes selbst mit, wie
in 1.21.5.

Fremd-Mods für 1.21.3 (Modrinth, 2026-10-14, im Manifest seit 0.16.0): Fabric API 0.114.1 (`wLq4rGsB`), Sodium 0.6.13
(`rLBgU2jc`), Lithium 0.14.6 (`W0Cc7ZVd`), Continuity 3.0.0 (`kwuHTVHC`), e4mc 6.2.1 (`k8wW4lF5`) und Bushy Vegetation 3.3.2
(`rIzHhAqW`, dieselbe Datei wie für 1.21.1 und 1.21.4). Cape Provider gibt es für 1.21.3 nicht – die Mod bringt die Capes
selbst mit, wie in 1.21.4.

Fremd-Mods für 26.2 (Modrinth, 2026-10-09, im Manifest seit 0.1.13): Fabric API 0.161.0 (`ewUK83HI`), Sodium 0.9.2
(`xJZxADzI`), Lithium 0.25.3 (`f7vZ0VWU`), Continuity 3.0.1 (`mgUN5Xz2`), Cape Provider 5.5.1 (`Puw7YiUS`) und
e4mc 6.2.2 (`AouleFRY`, dieselbe Datei wie für 26.3) – alles da. Bushy Vegetation gibt es für 26.2 nicht; die
26er-Versionen haben ohnehin nur unsere eigenen Büsche.

Fremd-Mods für 1.21.8 (Modrinth, 2026-10-09, im Manifest seit 0.1.14): Fabric API 0.136.1 (`g58ofrov`), Sodium 0.7.3
(`7pwil2dy`), Lithium 0.18.1 (`qxIL7Kb8`), Continuity 3.0.1-beta.1 (`m0cvWhzT`), Cape Provider 4.0.2 (`7FV01Drj`),
e4mc 6.2.1 (`k8wW4lF5`, dieselbe Datei wie für 1.21.10 und 1.21.11; 6.2.3 gäbe es auch) und Bushy Vegetation 3.4.2
(`aLE579XT`) – alles da.

### Vor 1.21.2 mit Fabric – letzte Unterversion je Hauptversion

Starten heute schon mit Fabric, aber ohne unsere Mod.

- [ ] 1.15.2 (2020-01-17)
- [ ] 1.16.5 (2021-01-14)
- [ ] 1.17.1 (2021-07-06)
- [ ] 1.18.2 (2022-02-28)
- [ ] 1.19.4 (2023-03-14)
- [ ] 1.19.2 (2022-08-05) – Entschieden am 2026-10-15: 1.19 bekommt beide Unterversionen, 1.19.4 als letzte und stabilste,
  1.19.2 als die für Modpacks verbreitetere (wie 1.20.1 neben 1.20.6). Geplante Reihenfolge: erst 1.19.4 (aus dem
  1.20.1-Stand; 1.20 brachte `GuiGraphics`, darunter wird die ganze Oberfläche anders gezeichnet), dann 1.19.2 aus dem
  1.19.4-Stand (1.19.3 hat Registries, Kreativ-Tabs und die Mathe-Bibliothek umgebaut). Danach 1.18.2.

Bei zwei Hauptversionen ist die letzte Unterversion nicht die meistgespielte: für Modpacks sind
1.20.1 (statt 1.20.6) und 1.19.2 (statt 1.19.4) verbreiteter. 1.20.5 hat die Items intern umgebaut,
1.20.1 und 1.20.6 wären für die Mod also zwei verschiedene Ports. Entschieden am 2026-10-13: 1.20.1 bekommt eine eigene
Mod-Version, sie kommt mit 1.21.4 und 1.21.3 in dasselbe Update wie 1.21.5.

1.20.1 ist seit 0.16.0 unter „Unterstützt“ (`mod/1.20.1`, Java 17).

Fremd-Mods für 1.20.1 (Modrinth, 2026-10-14, im Manifest seit 0.16.0): Fabric API 0.92.12 (`rvI2dfzR`), Sodium 0.5.13
(`OihdIimA`), Indium 1.0.36 (`nQHYSjxO`, dieselbe Datei wie für 1.20.6 – Sodium 0.5 hat keine Fabric Rendering API, ohne
Indium verbindet Continuity nichts), Lithium 0.11.4 (`iEcXOkz4`), Continuity 3.0.0 (`qGTDcjHM`), e4mc 6.2.1 (`k8wW4lF5`,
dieselbe Datei wie in den anderen Versionen) und Bushy Vegetation 3.2.2 (`tGmPHxzv`, für 1.20.1 gibt es kein 3.3.x). Cape
Provider gibt es für 1.20.x nicht – die Mod bringt die Capes selbst mit, wie in 1.20.6.

Welche der mitgelieferten Fremd-Mods es für eine Auswahl dieser Versionen gibt (Modrinth, Fabric,
2026-10-08):

| Version | Fabric API | Sodium | Lithium | Continuity | Cape Provider | e4mc | Bushy Vegetation |
|---|---|---|---|---|---|---|---|
| 1.20.1 | ja | ja | ja | ja | nein | ja | ja |
| 1.19.4 | ja | ja | ja | ja | nein | ja | ja |
| 1.18.2 | ja | ja | ja | ja | nein | ja | ja |
| 1.16.5 | ja | ja | ja | nein | nein | nein | ja |

Ohne Cape Provider bringt die Mod Capes selbst mit (wie in 1.14.4), ohne e4mc gibt es keine
Einladungen in die eigene Welt.

Fremd-Mods für 1.19.x (Modrinth, Fabric, 2026-10-15, jeweils neueste Vollversion): 1.19.4 – Fabric API 0.87.2 (`nyAmoHlr`),
Sodium 0.4.10 (`b4hTi3mo`), Lithium 0.11.1 (`14hWYkog`), Continuity 3.0.0 (`kHdjiepB`), Indium 1.0.19 (`oYQsfz9e`); 1.19.2 –
Fabric API 0.77.0 (`6g95K303`), Sodium 0.4.4 (`rAfhHfow`), Lithium 0.11.1 (`m6sVgAi6`), Continuity 2.0.2 (`Ql3Ho9eR`, ältere
Reihe), Indium 1.0.9 (`yTfou6df`). Für beide: e4mc 6.2.x, Bushy Vegetation 3.2.2 (`tGmPHxzv`), kein Cape Provider. 1.15.2 und 1.17.1 sind noch nicht nachgesehen.

Fremd-Mods für 1.20.6 (Modrinth, 2026-10-08): Fabric API 0.100.8, Sodium 0.5.11, Lithium 0.12.5,
Continuity 3.0.0 (braucht mit Sodium 0.5 zusätzlich Indium 1.0.36), e4mc 6.2.1 und Bushy Vegetation 3.3.2 – alles da außer Cape Provider (für keine
1.20.x-Version).

### Legacy, im Launcher startbar – letzte Unterversion je Hauptversion

Starten heute als reines Minecraft ohne unsere Mod. Bauart wie 1.8.9; Fabric gibt es erst ab 1.14.

- [ ] 1.7.10 (2014-05-14)
- [ ] 1.9.4 (2016-05-10)
- [ ] 1.10.2 (2016-06-23)
- [ ] 1.11.2 (2016-12-21)
- [ ] 1.12.2 (2017-09-18)
- [ ] 1.13.2 (2018-10-22)

### Legacy, im Launcher noch nicht startbar

Brauchen erst das alte Asset-Format und das Session-Argument im Launcher, dann eine eigene Mod.

- [ ] 1.0 (2011-11-17)
- [ ] 1.1 (2012-01-11)
- [ ] 1.2.5 (2012-03-29)
- [ ] 1.3.2 (2012-08-15)
- [ ] 1.4.7 (2012-12-27)
- [ ] 1.5.2 (2013-04-25)
- [ ] 1.6.4 (2013-09-19)

## Zahlen

31 Versionen nach den Regeln oben: 13 unterstützt, 18 offen (5 ältere mit Fabric, 6 startbare Legacy-Versionen,
7 noch nicht startbare). Dazu die 1.20.1 als Ausnahme von der Regel (Modpack-Version neben 1.20.6), ebenfalls unterstützt.

Fremd-Mods für 1.21.10 (Modrinth, 2026-10-09, liegen im Dev-Bundle): Fabric API 0.138.4 (`tV4Gc0Zo`), Sodium 0.7.3
(`sFfidWgd`), Lithium 0.20.1 (`NsswKiwi`), Continuity 3.0.1-beta.2 (`7JbPpQnu`), Cape Provider
4.3.1.0-legacy (`rxDooCVe`), e4mc 6.2.1 (`k8wW4lF5`) und Bushy Vegetation 3.5.2 (`2VhYpHqk`) – alles da.
