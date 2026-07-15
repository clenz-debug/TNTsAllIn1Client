# Aktuelle Phase

**Phase 0 — Setup & externe Abhängigkeit anstoßen** ✅ abgeschlossen

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
**Phase 1 — "Hello World"-Fabric-Mod**: Fabric Template Mod Generator (fabricmc.net/develop/template) für die aktuelle Stable-Version nutzen, in IntelliJ öffnen, `runClient` starten. Muss nicht auf die Minecraft-API-Freischaltung warten.
