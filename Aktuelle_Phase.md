# Aktuelle Phase

**Phase 0 — Setup & externe Abhängigkeit anstoßen** ✅ abgeschlossen

**Phase 1 — "Hello World"-Fabric-Mod** ✅ abgeschlossen

**Phase 2 — Erstes Mixin** ✅ abgeschlossen

## Phase 2 — Erledigt
- Mixin-Infrastruktur aufgesetzt: `src/main/resources/tntsallin1client.mixins.json` (Package `com.tntsallin1client.mixin`, `compatibilityLevel: JAVA_21`), in `fabric.mod.json` unter `"mixins"` registriert. Fabric Loom (1.17.16, über den `net.fabricmc.fabric-loom-remap`-Plugin-Alias) erkennt die Config automatisch und übernimmt Annotation-Processing/Refmap ohne zusätzliche `build.gradle.kts`-Änderungen.
- `mixin/GuiMixin.java`: `@Mixin(Gui.class)` mit `@Inject(method = "render", at = @At("TAIL"))` in `net.minecraft.client.gui.Gui#render(GuiGraphics, DeltaTracker)` — die zentrale HUD-Render-Methode. Zielsignatur vorher direkt aus dem von `genSources` erzeugten, gemappten Vanilla-Quellcode verifiziert (Mojang-Mappings, kein Blindraten).
  - Zeichnet einmal pro Frame den Text "TNT's All-In-1 Client (Mixin active)" oben links ins HUD (`guiGraphics.drawString(...)`).
  - Loggt genau einmal (statisches Flag) eine Bestätigungszeile beim ersten Feuern, statt bei jedem Frame zu spammen.
- **Bug während des Tests gefunden und gefixt:** Farbwert `0xFFFFFF` an `drawString` übergeben wurde als 32-Bit-ARGB zu `0x00FFFFFF` (Alpha = 0) — `GuiGraphics.drawString` verwirft den Aufruf still, wenn `ARGB.alpha(k) == 0` (Quelle: `GuiGraphics.java`, Zeile 253). Fix: `0xFFFFFFFF` (volles Alpha) verwendet. Lehre: Farbwerte für GuiGraphics-Zeichenmethoden immer als ARGB inkl. Alpha-Byte angeben, nicht als reines RGB.
- Verifiziert im Dev-Client (`runClient`, zwei Durchläufe — erster zeigte den Bug, zweiter nach dem Fix bestätigt):
  - Log-Zeile `[tntsallin1client] Phase 2 mixin fired: injected into Gui#render.` erscheint beim Weltbeitritt ✅
  - Text ist sichtbar im HUD oben links ✅ (visuell vom Nutzer bestätigt)
- Mixin-Injection in eigenen Worten: Der Fabric-Mixin-Annotation-Prozessor webt zur Kompilierzeit Bytecode aus `GuiMixin` in die vom Spiel geladene `Gui`-Klasse ein — `@At("TAIL")` platziert den injizierten Code kurz vor dem `return` der `render`-Methode, sodass er nach allem Vanilla-HUD-Rendering, aber noch im selben Methodenaufruf läuft. Kein Overriding/Ersetzen der Originalmethode, sondern additive Injection in bestehenden Bytecode.

## Phase 1 — Erledigt
- Versionsrecherche direkt bei fabricmc.net/Modrinth (nicht auf die Roadmap-Planzahlen verlassen, siehe Risikohinweis #6):
  - **Minecraft hat tatsächlich auf ein jahresbasiertes Versionsschema gewechselt** (aktuell stabil: `26.2`, davor `26.1.x`). Der klassische `1.21.x`-Strang läuft aber parallel weiter und ist ebenfalls aktuell stabil (`1.21.11`).
  - Entscheidung: **`1.21.11` statt `26.2`** für den Mod verwendet. Grund: FabricMC's eigenes Referenz-Template für `26.2` verlangt **JDK 25**, `1.21.11` läuft mit dem bereits installierten **JDK 21** (Phase 0). Fabric API, Loader (`0.19.3`) und Sodium sind für beide Versionen aktuell — ein späterer Umstieg auf `26.x` ist somit ein reiner Versions-Bump, kein Rewrite.
- `mod/`-Projekt angelegt (Fabric Loom, Kotlin DSL wie in der Roadmap festgelegt, offizielle Mojang-Mappings statt Yarn — Yarn hat für `26.x` noch keine Builds, für `1.21.11` wären sie zwar da, aber das Template selbst nutzt inzwischen `officialMojangMappings()`):
  - `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties` (MC `1.21.11`, Loader `0.19.3`, Loom `1.17-SNAPSHOT`, Fabric API `0.141.5+1.21.11`).
  - Gradle Wrapper (Gradle `9.5.1`) aus FabricMC's offiziellem `fabric-example-mod`-Repo (Branch `1.21.11`) übernommen.
  - `com.tntsallin1client.TNTsAllIn1ClientMod` — Client-Entrypoint, Logger-Zeile beim Start, Log + Chat-Nachricht via `ClientPlayConnectionEvents.JOIN` beim Welt-Beitritt.
  - Bewusst **kein** Mixin-Beispiel und **kein** separates `client`-Sourceset eingebaut — Mixins sind laut Roadmap erst Phase 2, und das Projekt ist ohnehin reines Client-Mod (kein Server-Entrypoint nötig).
- `./gradlew build` lokal erfolgreich (Mojang-Mappings-Setup, Kompilierung, Jar+Sources-Jar) — `tntsallin1client-0.1.0.jar` liegt in `mod/build/libs/`. `genSources`- und `runClient`-Tasks sind vorhanden und einsatzbereit.

- IntelliJ: "Trust Project" bestätigt, Gradle-Sync durchgelaufen, "Minecraft Development"-Plugin (minecraft-dev, Fabric/Mixin-Support) installiert.
- `genSources` gelaufen (Vineflower, 6622 Klassen dekompiliert) — lesbarer Vanilla-Code für Navigation/Debugging verfügbar.
- `runClient` im Debug-Modus gestartet, Singleplayer-Welt erstellt. **Alle drei "Fertig, wenn"-Kriterien bestätigt:**
  - Dev-Client startet ✅
  - Breakpoint funktioniert ✅ (griff zuerst bei Zeile 19, dem `onInitializeClient`-Start — `client` ist dort noch nicht im Scope, das ist erwartet, da erst innerhalb der Lambda ab Zeile 22 verfügbar; nach Resume normal weitergelaufen)
  - Log-Zeile + Chat-Nachricht beim Welt-Beitritt ✅ ("TNT's All-In-1 Client loaded." im Chat, `[tntsallin1client] Joined a world.` im Log)

## Erledigt
- `git init` im Projektverzeichnis, Commits erstellt.
- `.gitignore` für das geplante Monorepo (mod/, launcher/, backend/) angelegt.
- JDK 21 (Temurin/Adoptium) installiert — `java -version` → 21.0.11.
- Node.js LTS installiert — `node -v` → v24.18.0.
- IntelliJ IDEA Community Edition installiert.
- Git war bereits vorhanden (2.51.2).
- Azure AD (Entra ID) Public-Client-App registriert: `TNTsAllIn1Client`, "Nur persönliche Konten", Redirect-URI `http://localhost` (öffentlicher Client), "Öffentliche Clientflows zulassen" = Ja.
  - Application (Client) ID: `6820dddd-8bc1-4fe6-ab9b-945a3a7d400a`
  - Verzeichnis-ID (Tenant): `a3dd876f-c607-4a29-a300-73b9e7130cbb`
- Minecraft-API-Freischaltungsantrag unter aka.ms/mce-reviewappid abgeschickt (2026-07-15). **Antwortzeit unklar — laut Roadmap-Risikohinweis nicht darauf blockieren, direkt mit Phase 1 weitermachen.**
- Öffentliches GitHub-Repo angelegt und gepusht: [github.com/clenz-debug/TNTsAllIn1Client](https://github.com/clenz-debug/TNTsAllIn1Client)
- Projekt-Roadmap zusätzlich auf Englisch verfügbar: `Projekt_Roadmap.en.md`.

*Fertig, wenn:* Tools laufen (✅), Azure-App existiert (✅), Antrag ist abgeschickt (✅).

## Blocker-Log (Azure-Login, gelöst)
Beim Registrieren der Azure-App gab es mehrere ineinandergreifende Probleme, bis alles lief:

1. **Tenant-Routing-Fehler** (`AADSTS50020`/`AADSTS16000`, Tenant "Microsoft Services"): Login/Hintergrund-Widgets versuchten wiederholt, einen Token gegen einen fremden Phantom-Tenant zu holen, in dem der Account kein Mitglied ist. Trat bei mehreren Accounts gleich auf.
2. **Inkognito-Fenster** löste einen neuen Fehler aus (`AADSTS50058`, silent sign-in), da Inkognito-Modus Third-Party-Cookies blockiert, die Azure Portal fürs Silent-SSO braucht — zurück zu normalem Fenster.
3. **Systemzeit war nie synchronisiert** (`w32tm`: "nicht synchronisiert", Quelle "Local CMOS Clock") — nach manueller Sync über Windows-Einstellungen lief der Login sauber durch.
4. **Root Cause**: Der Account hatte schlicht noch **kein eigenes Entra-ID-Verzeichnis** (App-Registrierungen-Seite zeigte das explizit an). Der "Mandanten verwalten"-Self-Service-Weg war gesperrt, da er ohne bestehendes Abo/Lizenz keine Tenant-Erstellung erlaubt.
5. **Lösung**: Kostenloser Azure-Free-Account-Signup (Telefon-/Kartenverifizierung, keine Kosten im Free-Tier) hat automatisch ein echtes Verzeichnis provisioniert — danach lief die App-Registrierung ohne Probleme durch.

## Nächster Schritt
**Phase 3 — Electron-Launcher-MVP: echter Microsoft-Login + Vanilla-Start**. Auth-Kette (Microsoft OAuth2 → Xbox Live → XSTS → Minecraft-Access-Token → Entitlement/Profil-Check), danach Version-Manifest, Download von Client-Jar/Libraries/Assets, Classpath/JVM-Args, `java` als Kindprozess starten. Praxis-Tipp aus der Roadmap: alles außer dem eigentlichen Minecraft-Login mit Platzhalter-Profildaten bauen und testen, während auf die Azure-Freischaltung gewartet wird — nicht blockieren lassen.
