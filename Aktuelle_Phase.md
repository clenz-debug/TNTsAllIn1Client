# Aktuelle Phase

**Phase 0 — Setup & externe Abhängigkeit anstoßen** (in Arbeit)

## Erledigt
- `git init` im Projektverzeichnis, erster Commit erstellt.
- `.gitignore` für das geplante Monorepo (mod/, launcher/, backend/) angelegt.
- JDK 21 (Temurin/Adoptium) installiert — `java -version` → 21.0.11.
- Node.js LTS installiert — `node -v` → v24.18.0.
- IntelliJ IDEA Community Edition installiert.
- Git war bereits vorhanden (2.51.2).

## Offen
- [x] Azure AD (Entra ID) Public-Client-App registriert: `TNTsAllIn1Client`, "Nur persönliche Konten", Redirect-URI `http://localhost` (öffentlicher Client), "Öffentliche Clientflows zulassen" = Ja.
  - Application (Client) ID: `6820dddd-8bc1-4fe6-ab9b-945a3a7d400a`
  - Verzeichnis-ID (Tenant): `a3dd876f-c607-4a29-a300-73b9e7130cbb`
- [ ] Minecraft-API-Freischaltungsantrag unter aka.ms/mce-reviewappid mit der Client ID abschicken (in Arbeit).

*Fertig, wenn:* Tools laufen (✅), Azure-App existiert (✅), Antrag ist abgeschickt.

## Blocker (2026-07-14)
Beim Versuch, im Azure Portal die App zu registrieren, Tenant-Routing-Fehler mit zwei verschiedenen Accounts:

```
AADSTS50020: User account '<account>' from identity provider 'live.com'
does not exist in tenant 'Microsoft Services' and cannot access the application
'74658136-14ec-4630-ad9b-26e160ff0fc6' (ADIbizaUX) in that tenant.
```

Gleicher Fehler auch mit einem zweiten, alternativen Account. Beide Male landet der
Login im fremden Tenant "Microsoft Services" statt im eigenen persönlichen Tenant —
vermutlich Tenant-Kontext aus einem Lesezeichen/gecachter Session, kein Account-Problem.

**Versuch 2:** Inkognito-Fenster ausprobiert → neuer Fehler `AADSTS50058` (silent
sign-in schlägt fehl, da Inkognito-Modus Third-Party-Cookies blockiert, die Azure
Portal fürs Silent-SSO braucht).

**Versuch 3:** Systemzeit war nie mit einem Zeitserver synchronisiert (`w32tm`:
"nicht synchronisiert", Quelle "Local CMOS Clock") — nach manueller Sync in den
Windows-Einstellungen ("Jetzt synchronisieren") funktionierte der Login im normalen
Edge-Fenster.

**Root Cause gefunden:** Verwendeter Account hatte noch **kein eigenes
Entra-ID-Verzeichnis** — App-Registrierungen-Seite zeigt explizit: "Diese Anwendungen
sind dem Konto zugeordnet, jedoch in keinem Verzeichnis enthalten." Einziges sichtbares
"Verzeichnis" (`F8CDEF31-A31E-4B4A-93E4-5F57...`) hat keine Subscriptions und liefert
im Hintergrund wiederholt AADSTS16000-Fehler gegen einen Phantom-Tenant "Microsoft
Services" (harmlos, aber nervig — Popup ignorieren bzw. per Deep-Link direkt zu
"App-Registrierungen"/"Mandanten verwalten" navigieren, umgeht die Startseite, wo das
Widget hängt).

**Nächster Schritt:** Neuen echten Tenant anlegen über "Mandanten verwalten" →
"+ Erstellen" → Typ "Microsoft Entra ID" (kein Abonnement nötig), dann dorthin
wechseln und App Registration dort erstellen.

## Nächster Schritt danach
Phase 1 — "Hello World"-Fabric-Mod (Template Mod Generator, muss nicht auf die Azure-Freischaltung warten).
