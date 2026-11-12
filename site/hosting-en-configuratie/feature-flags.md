# Feature flags: referentie

Hier staat welke schakelaars NL Portal kent, wat ze doen en waar je ze instelt: de lijst van flags en toggles vind je alleen hier. Hoe je een flagbestand aanwijst, de evaluatiecontext en targeting staan in de [deployment guide](./deployment-guide.md#feature-flags); de keuze achter flags staat in [ADR 0014](../development/adr/0014-feature-flags-via-openfeature.md).

Alle frontend-schakelaars zijn feature flags (flagd-bestand, zie [ADR 0014](../development/adr/0014-feature-flags-via-openfeature.md)). Ze komen samen in het antwoord van `GET /api/public/frontend` onder `toggles`. De eigenschappen `nl-portal.frontend.toggles.*` bestaan niet meer; een omgevingsvariabele voor een toggle werkt dus niet meer. Wijzigingen landen via een rolling update (pods herstarten).

## Feature flags

Evaluatie gebeurt per request met de [evaluatiecontext](./deployment-guide.md#feature-flags) (tenant, omgeving, gebruikerstype). Een flag die in je bestand ontbreekt, valt terug op de standaard in de code (uit).

| Flag | Type | Standaard | Effect |
|---|---|---|---|
| `ops.frontend.message-count` | ops | uit | Toont het aantal ongelezen berichten in de frontend. De frontend haalt het aantal dan periodiek op (`properties.messageCountPollingInterval`, standaard 30000 ms). Via `toggles.messageCountEnabled`. |
| `ops.frontend.cases-partial-search` | ops | uit | Zoeken op zaaknummer vindt ook gedeeltelijke overeenkomsten (`identificatieContains`) in plaats van alleen een exacte. Via `toggles.casesPartialSearchEnabled`. |
| `ops.frontend.open-product` | ops | uit | Haalt Open Product-thema's en producten op en toont ze op het overzicht. Via `toggles.openProductEnabled`. |
| `ops.frontend.cases-result-explanation` | ops | uit | Toont de toelichting op het resultaat van een zaak op de zaakpagina. Via `toggles.casesResultExplanationEnabled`. |
| `ops.frontend.my-inhabitant-count` | ops | uit | Toont het aantal inwoners op het adres op de accountpagina. Via `toggles.myInhabitantCountEnabled`. |
| `ops.frontend.cases-contact-moments` | ops | uit | Haalt de contactmomenten bij een zaak op en toont ze op de zaakpagina. Via `toggles.casesContactMomentsEnabled`. |
| `ops.frontend.overview-intro` | ops | uit | Toont de begroeting met de naam van de gebruiker (en de volmachtgever) bovenaan het overzicht. Via `toggles.overviewIntroEnabled`. |
| `ops.frontend.overview-maintenance-alert` | ops | uit | Toont de onderhoudsmelding op het overzicht; titel en tekst komen uit `properties.overviewMaintenanceAlert{Title,Text}{Nl,En}`. Via `toggles.overviewMaintenanceAlertEnabled`. |
| `ops.frontend.custom-toggles` | ops (object) | leeg | Vrije eigen toggles: een object met booleans, bijvoorbeeld `{ "betaBanner": true }`. De frontend ontvangt het als JSON-tekst in `toggles.custom`. Waarden die geen boolean zijn worden genegeerd. Staat niet in de meegeleverde bestanden; voeg hem toe aan je eigen bestand als je custom toggles nodig hebt. |

Het meegeleverde bestand (`backend/core/src/main/resources/feature-flags/default.flagd.json`) zet alle toggles uit. Het compose-bestand `docker-compose/imports/flags/nl-portal.flagd.json` zet ze voor de lokale stack aan, behalve `open-product` en `cases-result-explanation`.
