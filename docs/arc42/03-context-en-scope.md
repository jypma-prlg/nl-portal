# 3. Context en scope

Zie [`docs/likec4`](../likec4), view `index` (C1 — systeemcontext) voor het diagram. Dit hoofdstuk
geeft de tekstuele toelichting erbij.

## 3.1 Fachlicher Kontext (functionele context)

**Actoren:**

- **Burger / ondernemer** — gebruikt de frontend om zaken, berichten, taken en eigen gegevens in te
  zien, en kan (via een gemachtigde) ook namens iemand anders optreden.
- **Ketenpartner-medewerker** — gebruikt de frontend om voor hen relevante zaakinformatie te zien.

**Buiten scope** (expliciet, zie
[`documentation/fundamentals/architectuur/5-lagenmodel.md`](../../documentation/fundamentals/architectuur/5-lagenmodel.md)):

- Informatieverstrekking / CMS
- Authenticatie zelf (dat is DigiD/eHerkenning/Keycloak, niet NL Portal)
- Notificaties (e-mail/sms/WhatsApp — bijvoorbeeld NL Notify)
- Zaakafhandeling (bijvoorbeeld GZAC)

## 3.2 Technischer Kontext (technische context)

| Buursysteem | Protocol | Richting |
|---|---|---|
| Keycloak | OIDC | NL Portal ← (token-validatie) / NL Portal → (login-redirect) |
| Open Zaak | ZGW Zaken-, Catalogi-, Documenten-, Besluiten-API (REST/JSON) | NL Portal → |
| OpenKlant 2 | ZGW Klantinteracties-API | NL Portal → |
| Open Object | ZGW Objecten-API | NL Portal → |
| Open Notificaties | ZGW Notificaties-API (webhook) | → NL Portal |
| OpenProduct | REST (UPL-productencatalogus) | NL Portal → |
| Haal Centraal BRP | Haal Centraal BRP-bevragingen-API | NL Portal → |
| Haal Centraal Bewoning | Haal Centraal Bewoning-API | NL Portal → |
| ClamAV | ICAP | NL Portal → |

Alle backend-integraties zijn uitgaand (NL Portal als cliënt), met één uitzondering: Open
Notificaties roept de backend aan via webhook zodra een ZGW-bron wijzigt. Dit is een belangrijk
asymmetrie om te onthouden bij foutafhandeling en beveiliging (inkomend verkeer van een
buitensysteem vereist eigen verificatie, zie hoofdstuk 8).

NL Portal zelf heeft geen directe afhankelijkheid tussen frontend en de externe systemen: de
frontend spreekt uitsluitend de eigen backend aan (GraphQL), en de backend is de enige component die
de ZGW-landschap-aanroepen doet. Dat is een bewuste keuze, zie hoofdstuk 4.
