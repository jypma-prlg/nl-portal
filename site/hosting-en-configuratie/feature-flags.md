# Feature flags: referentie

Hier staat welke schakelaars NL Portal kent, wat ze doen en waar je ze instelt: de lijst van flags en toggles vind je alleen hier. Hoe je een flagbestand aanwijst, de evaluatiecontext en targeting staan in de [deployment guide](./deployment-guide.md#feature-flags); de keuze achter flags staat in [ADR 0014](../development/adr/0014-feature-flags-via-openfeature.md).

Een schakelaar is een feature flag of een frontend-toggle die nog geen feature flag is:

| Weg | Waar stel je het in | Wijziging actief via |
|---|---|---|
| **Feature flag** (flagd-bestand) | Het bestand achter `NLPORTAL_FEATUREFLAGS_FILE`; zonder die variabele het meegeleverde bestand | Rolling update (pods herstarten) |
| **Frontend-toggle** (property) | `nl-portal.frontend.toggles.*`, bijvoorbeeld `NLPORTAL_FRONTEND_TOGGLES_OPENPRODUCTENABLED` | Rolling update (pods herstarten) |

Alle frontend-schakelaars komen samen in het antwoord van `GET /api/public/frontend` onder `toggles`; de frontend ziet geen verschil tussen de twee wegen. Een schakelaar staat op precies één van de twee wegen. De frontend-toggles verhuizen stapsgewijs naar feature flags (#156); deze pagina volgt dat.

## Feature flags

Evaluatie gebeurt per request met de [evaluatiecontext](./deployment-guide.md#feature-flags) (tenant, omgeving, gebruikerstype). Een flag die in je bestand ontbreekt, valt terug op de standaard in de code.

| Flag | Type | Standaard | Effect |
|---|---|---|---|
| `ops.frontend.message-count` | uit | Toont het aantal ongelezen berichten in de frontend, via `toggles.messageCountEnabled`. De frontend haalt het aantal dan periodiek op (`properties.messageCountPollingInterval`, standaard 30000 ms). |

Het meegeleverde bestand (`backend/core/src/main/resources/feature-flags/default.flagd.json`) zet deze flag uit. Het compose-bestand `docker-compose/imports/flags/nl-portal.flagd.json` zet hem voor de lokale stack aan.

## Frontend-toggles (nog geen flag)

Deze zijn booleans in `nl-portal.frontend.toggles.*` en staan standaard uit (niet gezet is uit). Ze zijn nog niet als flag beschikbaar, dus ze kunnen niet in het flagd-bestand; de omgevingsvariabele is de enige weg.

| Toggle | Omgevingsvariabele | Standaard | Effect |
|---|---|---|---|
| `casesPartialSearchEnabled` | `NLPORTAL_FRONTEND_TOGGLES_CASESPARTIALSEARCHENABLED` | uit | Zoeken op zaaknummer vindt ook gedeeltelijke overeenkomsten (`identificatieContains`) in plaats van alleen een exacte. |
| `openProductEnabled` | `NLPORTAL_FRONTEND_TOGGLES_OPENPRODUCTENABLED` | uit | Haalt Open Product-thema's en producten op en toont ze op het overzicht. |
| `casesResultExplanationEnabled` | `NLPORTAL_FRONTEND_TOGGLES_CASESRESULTEXPLANATIONENABLED` | uit | Toont de toelichting op het resultaat van een zaak op de zaakpagina. |
| `myInhabitantCountEnabled` | `NLPORTAL_FRONTEND_TOGGLES_MYINHABITANTCOUNTENABLED` | uit | Toont het aantal inwoners op het adres op de accountpagina. |
| `casesContactMomentsEnabled` | `NLPORTAL_FRONTEND_TOGGLES_CASESCONTACTMOMENTSENABLED` | uit | Haalt de contactmomenten bij een zaak op en toont ze op de zaakpagina. |
| `overviewIntroEnabled` | `NLPORTAL_FRONTEND_TOGGLES_OVERVIEWINTROENABLED` | uit | Toont de begroeting met de naam van de gebruiker (en de volmachtgever) bovenaan het overzicht. |
| `overviewMaintenanceAlertEnabled` | `NLPORTAL_FRONTEND_TOGGLES_OVERVIEWMAINTENANCEALERTENABLED` | uit | Toont de onderhoudsmelding op het overzicht; titel en tekst komen uit `properties.overviewMaintenanceAlert{Title,Text}{Nl,En}`. |

`toggles.custom` is een vrij tekstveld voor eigen uitbreidingen en geen schakelaar.

