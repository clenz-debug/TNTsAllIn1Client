# Ideen für den Client:

## Ideen für den Client von anderen Personen:

### Ingame Features:

- F3 und Shift+F3 Münü verbessern (sofern möglich) vll mit eigenen hotkeys (im mod menü einstellbar) [check]
- Optionen für 3D Texturen oder verbundenes Glas
  - verbundenes Glas: [check] (5f, Continuity)
  - 3D Texturen (echte Blockmodelle mit zusätzlicher Geometrie): [check] (5p — kein Mod-Code nötig, gebündeltes Resourcepack "3D Default", live bestätigt: platzierte Tür wird als echtes 3D-Modell gerendert; Lizenz-Hinweis siehe Aktuelle_Phase.md)
- Praktische Sachen fürs Inventar (schnell Items sortieren o.ä.) [check]
- Vielleicht irgendwie eine Anzeige oben rechts im Bildschirm, die anzeigt, wie viele Materialien man für irgendetwas noch braucht [check]
- Koordinaten und Kompass als Anzeige [check]























## Meine Persöhnlichen Idden:

### overall:
- Kompatibel mit Linux und MacOS [?]
- man sollte einfach updates aufspielen können, wenn z.B. neue features hinzugefügt wurden [?]
- jeder User soll sein Farbschema mit Hauptfarben und hintergrundfarbe selbst einstellen können 
- beim ersten starten nach installieren des clients soll sich der user eine willkommensnachricht bekommen, dann soll ein menü sich öffnen wo er die Farben einstellen kann (das kann man skippen, dann bekommt man den Standartfarbcode) und als letztes soll sich ein Tutorial öffnen (das soll man auch überspringen können)
- in Discord soll angezeigt werden das man MC über meinen Client spielt — Timing-Entscheidung 2026-08-10: bewusst spät (nahe Packaging/Phase 9), siehe Chat-Begründung in Aktuelle_Phase.md
- 3D Overlay für skin (Ingame) statt den flachen etwas vom skin abstehenden elementen jetzt würfel, sodass es besser aussieht [check] (5s, schon vorher erledigt — 3D Skin Layers gebündelt + Mod-Menü-Toggle)
- speichung von einstelungen in MC wenn die Vesrsion gewechselt wird [check] (6e — geteilte options.txt über alle Versionen hinweg)

### Launcher:

- Gut aussehenden Launcher mit den folgenden Features: 
    - Modloader
    - Skin und Cape Editor (und gutes Auswahlfenster für Skin und Cape)
    - gute Versionsauswahl
    - Freundes bereich
- Instanz-System statt reinem Versionswechsel: pro Version mehrere eigene Instanzen anlegbar (z.B. gleiche Version einmal mit, einmal ohne bestimmten Mods), statt bei jedem Wechsel hin- und herzuschalten [check] (umgesetzt — Play-Screen hat jetzt eine Instanz-Auswahl statt der reinen Versionsauswahl, neuer "Instanzen verwalten"-Screen zum Anlegen/Umbenennen/Löschen; jede Instanz hat ihren eigenen Ordner, eigene Version und eigene gebündelte-Mods-Auswahl; alte, schon heruntergeladene Version-Ordner werden beim ersten Start nach dem Update automatisch zu einer Instanz migriert, kein erneuter Download nötig)
- Speicherplatz-Problem: Instanzen luden Version-Jar/Bibliotheken/Assets bisher komplett doppelt (pro Instanz), und der Speicherort war fest auf dem System-Laufwerk (%APPDATA%) — beides sollte sich ändern lassen [check] (umgesetzt — `versions/`, `libraries/`, `assets/` liegen jetzt einmal geteilt unter dem Datenwurzel-Ordner statt pro Instanz dupliziert zu werden; neuer "Speicherort"-Abschnitt im Instanzen-Screen mit Ordnerauswahl + Verschieben-Fortschrittsanzeige, lässt sich auf ein anderes Laufwerk legen; bereits vorhandene Alt-Duplikate aus der Zeit vor diesem Fix werden beim ersten Start nach dem Update automatisch in den geteilten Speicher konsolidiert, kein erneuter Download nötig — gleiches Prinzip wie die Instanz-Migration oben; dabei gleich mit erledigt: die Server-Liste (`servers.dat`) ist jetzt ebenfalls instanz-/versionsübergreifend geteilt, genau wie schon `options.txt`, statt beim Versionswechsel neu eingetragen werden zu müssen; live bestätigt, "hat funktioniert")
- Mods direkt im Launcher über Modrinths API suchen/installieren, statt nur lokale Jar-Dateien auszuwählen — so wie es z.B. der Client "Dawn" macht [check] (neuer "Mods durchsuchen"-Abschnitt im Mods-Screen: Suchfeld + Ergebnisliste mit Icon/Titel/Beschreibung, "Installieren" lädt automatisch die neueste passende Fabric-Version herunter, SHA-1-geprüft; kein Abhängigkeits-Auflösen, keine Versionsauswahl in v1; gebaut, noch nicht live getestet)
- Einstellungen/Mods von einem anderen, bereits installierten Client übernehmen können: legt dafür eine eigene neue Instanz an, übernimmt Standard-MC-Einstellungen sowie vorhandene, selbst heruntergeladene externe Mod-Jars aus dessen Mods-Ordner (Überschneidungen mit unseren eigenen gebündelten Mods dabei überspringen). Ausdrücklich NICHT übernommen werden Cosmetics und Capes — die gehören zum jeweiligen Client, das wäre sonst wie Diebstahl [check] (neuer Button "Von anderem Client übernehmen…" im Instanzen-Screen, freie Ordnerauswahl statt fest hinterlegter Client-Pfade; Cosmetics/Capes werden nie angefasst, da die ohnehin nur im Mojang-Account liegen; Rückfrage-Ergebnis: übernommene options.txt landet bewusst im geteilten Einstellungen-Cache und gilt damit für alle Instanzen, mit Warnhinweis im UI; gebaut, noch nicht live getestet)
- Finales Launcher-Layout (Buttons, Versions-/Instanzauswahl) erst überarbeiten, wenn alle Launcher-Features fertig sind, nicht schon jetzt am aktuellen Zwischenstand [Entscheidung 2026-08-10: ja, Redesign bewusst ans Ende schieben — siehe Chat]
- Launcher muss beim Schließen wirklich komplett beendet sein, darf nicht wie bei Lunar im Hintergrund weiterlaufen [Status 2026-08-10: aktuell schon so — kein Tray-Icon, `window-all-closed` beendet den Prozess; vor Release nochmal gegenchecken, v.a. ob ein laufendes Spiel den Launcher-Schließen-Vorgang überlebt]

### Ingame Features:
- Gutes Ingame Menü um die featurs / mods auzuwählen (auch über taste(-nkombie) zu öffnen welche mann selbst einstellen kann) [check] (5e)
    - Fullbright und lightlevel oveerlay [check] (5j — gebaut, noch nicht live getestet; nur die Zahl an der eigenen Position, kein voller In-World-Tile-Overlay)
    - Custom Crosshair (auch mit Farbauswahl) [check] (5i — gebaut, noch nicht live getestet)
    - Anpassung der Hitbox Farbe [check] (5l — gebaut, noch nicht live getestet)
    - Itemphysics [check] (5o — gebaut, noch nicht live getestet; bewusst kein gebündelter Fremd-Mod, siehe Aktuelle_Phase.md: das übliche "ItemPhysic"-Mod braucht auch Server-Seite, hätte auf normalen Servern nichts bewirkt — stattdessen eigener rein kosmetischer Neige-Effekt)
    - Beim erstellen eines Screenshots soll eine Nachricht aufploppen mit: open und copy was dann die jeweilige Funktion ausfürt [check] (5n — gebaut, noch nicht live getestet; "open" gab es bei Vanilla schon als Chat-Link, "copy" ins Bild-Clipboard war der fehlende Teil)
    - Keystrokes (vll auch mehr als das klassische wasd lmb rmb shift, space) [check] (5m — gebaut, noch nicht live getestet; plus Sprint- und Q-Box)
    - proximity voice mod oder was ähnliches wäre cool nicht jeder nutzt Discord zum labern das macht es einfacher [später — zu groß für Phase 5, eigene Phase/Bündelung wie 5f prüfen]
    - zoom [check] (5h — gebaut, noch nicht live getestet)
    - fps anzeige [check] (5g — gebaut, noch nicht live getestet)
    - Shulkerbox inhalts anzeige (wenn bestimmter key gedrükt wird) [check] (5k — gebaut, noch nicht live getestet; Vanilla zeigt seit 1.21.11 selbst schon die ersten 5 Items im Tooltip, unser Zusatz zeigt den Rest beim Tastehalten)
    - suchfunktion für mods [check] (5v — live bestätigt, "klappt gut")
    - Abschnitte in dem Mod options wo man quasie bereiche hat in denen die mods separiert werden, explizit keine Extra menüs nur einzelne Abschnitte [check] (5w — live bestätigt, "das passt")
    - dark mode toggle [check] (5x — live bestätigt, "das passt")
    - material counter verschiebbar machen und item als bild einblenden (wenn in der Material counter mod option eingeschaltet) [check] (5y — live bestätigt, "passt")
    - feinschleifen von sort funktion (z.B. nicht nur nach Name sondern wenn eingestellt nach Item Gruppe wie redstone stuff, building blocks, funktionale blöcke, etc.) [check] (5z — live bestätigt, "passt"; ein Bugfix unterwegs: Cursor-Item-Duplizierung in Creative)
    - custom hitbox blickrichtung und das was mann sonst bei f3 + b sichtbar machen (wenn in custom hitbox mod option eingeschaltet) [check] (5aa — live bestätigt, "passt"; plus 5ab: jeder Indikator einzeln ein/ausschaltbar + einfärbbar)
    - andere crosshair form wenn auf mob geaimt wird (in crosshair mod option ein und ausschaltbar machen) [check] (5ac — live bestätigt, "klappt")
    - farbe von den pixeln beim drawing vom crosshair auf weiß ändern weil das ist besser für leute mit rot grünschwäche [check] (5ad — live bestätigt, "passt")
    - im recipie book rezepte anpinnen, anzahl der benötigten materialien runterzählen wenn man ein item was benötigt wird ins inventar packt [check] (5ah — Anpinnen per Taste beim Hovern, angepinntes Rezept als verschiebbares HUD-Element, zählt Restmenge live gegen das Inventar runter)
    - material counter in item counter umbennen [check] (5ag — Klassen/Config-Felder/Lang-Keys durchgehend umbenannt)
    - hitbox color und blockoutline color das custom entfernen [check] (5ae — reine Lang-Key-Änderung)
    - Waypoint System die graphisch angezeigt werden und ein eigenes menü haben das man über einen key öffnen kann [check] (5ai — gebaut, noch nicht live getestet; In-World-Leuchtsäule + Namensschild inkl. Entfernung, eigenes Wegpunkt-Menü über eigene Taste ODER Mod-Menü erreichbar, Anlegen/Bearbeiten/Löschen/Sichtbarkeit pro Wegpunkt, farbwählbar; Nachtrag: Lösch-Bestätigung für einzelne Wegpunkte optional ein/ausschaltbar, "Alle löschen"-Button fragt immer nach)
    - Waypoints sollen pro Welt und pro Server getrennt sein (sofern technisch möglich), nicht eine gemeinsame Liste für alle [check] (Nachtrag zu 5ai — ging technisch: Singleplayer wird über den Speicherordner-Namen der Welt identifiziert, Multiplayer über die Server-Adresse; Achtung, siehe Aktuelle_Phase.md: dadurch werden eventuell schon angelegte Test-Wegpunkte aus der alten, noch nicht welten-getrennten Speicherung nicht automatisch übernommen)
    - dazu prüfen, ob (zusätzlich) auch eine Trennung nach Dimension möglich ist [check] (war schon seit 5ai selbst so - jeder Wegpunkt merkt sich seine Dimension und wird nur dort angezeigt/gezählt, keine neue Änderung nötig)
    - Armor & Tool Status Anzeige: zeigt Durability von angelegter Rüstung (Helm, Brustpanzer, Hose, Schuhe), Mainhand und Offhand [check] (Nachtrag zu Phase 5 — gebaut, noch nicht live getestet, siehe Aktuelle_Phase.md)
        - bei stackbaren Items (Blöcke etc.) statt Durability die Anzahl im jeweiligen Stack anzeigen (nur dieser Stack, nicht das gesamte Inventar)
        - jede Anzeige (Helm/Brustpanzer/Hose/Schuhe/Mainhand/Offhand) einzeln ein-/ausschaltbar
        - Item-Name Anzeige ein-/ausschaltbar
        - Zahlenfarbe: frei wählbare Standardfarbe ODER Farbverlauf je nach Restdurability; stackbare Items immer mit fester Standardfarbe, nie mit Verlauf
        - Item-Icon Anzeige ein-/ausschaltbar (passendes Icon zum jeweiligen Item)
        - Layout wählbar: entweder alle 6 Slots einzeln als eigene, frei verschiebbare HUD-Elemente, oder gebündelt als eine Anzeige (gebündelt: horizontal oder vertikal wählbar)
- dawn hat einen ingame button für die externen mods wo man die mods settings öffnen kann [check] (ein Button "Externe Mods" im Mod-Menü öffnet einen separaten, rein informativen Bilder-Screen wie bei Dawn/Texturepacks - Icon + Name pro gebündeltem Mod, kein Aktivieren/Deaktivieren dort; nur Sodium und 3D Skin Layers haben dort einen "Optionen"-Button, da nur sie einen eigenen Einstellungs-Screen haben; Continuitys Toggles [inkl. neu Leuchttexturen] und 3D Skin Layers' Ein/Aus bleiben normale Schalter im Hauptmenü; live bestätigt, "befinde ich als gut")

- skin editor: zurück taste um den letzten strich / pixel rückgängig zu machen. Grid einblenden sonst fällt das malen schwer. Skins und Capes vll voneinander trennen oder sdie anordnung im bereich skins ändern sodass alle skinn sachen zusammen sind und alle cape sachen zusammen, dann aber den bereich in skin und capes umbennen [check] (Rückgängig + Pixel-Raster im Editor umgesetzt, Bereich heißt jetzt "Skin & Capes"; zusätzlich auf Rückfrage: alle Skins inkl. Bibliothek als drehbare 3D-Modelle statt flacher Bilder, ein gemeinsamer Cape-Anzeigen-Schalter für alle, Bibliothek paginiert wegen WebGL-Kontext-Limit; gebaut, noch nicht live getestet)
- skin editor layer-sichtbarkeit "wie bei skinmc.net" statt Checkboxen [check] (zwei klickbare 2D-Figur-Diagramme nebeneinander, eines für Basis-Schicht, eines für Overlay - Klick auf Kopf/Körper/Arm/Bein blendet genau dieses Körperteil auf der jeweiligen Schicht ein/aus, statt einer Checkbox-Liste; gebaut, noch nicht live getestet)