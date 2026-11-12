# Performancetests (k6)

Performance- en loadtests voor NL Portal met [Grafana k6](https://k6.io/), zie
[ADR 0018](../site/development/adr/0018-performancetesten-met-k6-en-otel-agent-in-overlay.md) en epic #428.
k6 staat in `mise.toml`; de scripts zijn TypeScript en draaien direct onder k6 (geen build- of npm-stap).

## Indeling

| Pad | Inhoud |
|---|---|
| `lib/config.ts` | Configuratie uit de omgeving (URL's, realm, client, testgebruiker) |
| `lib/auth.ts` | Tokenflow: access token via Keycloak (direct access grant) |
| `lib/graphql.ts` | GraphQL-request en foutdetectie op een HTTP 200-antwoord |
| `lib/thresholds.ts` | Gedeelde thresholds (pass/fail) |
| `seed/` | Deterministisch seed-script (`plan.ts` is het pure plan, `seed.ts` voert het uit) en de definitie van de perf-only Keycloak-client |
| `scenarios/smoke.ts` | Smoke: 1 gebruiker, 1 iteratie |

## Seed-data en perf-client

`mise run perf:seed` vult de draaiende stack (profiel `zgw`, Keycloak, Open Zaak en Objecten API) met een vast volume:

- testgebruikers `burger-001`, `burger-002`, ... in Keycloak, elk met een eigen BSN (elfproef, buiten de BSN's van de demodata) en een gedeeld wachtwoord;
- per gebruiker zaken (met rol, status en documenten) in Open Zaak en taken (Taak V2) in de Objecten API;
- de client `nl-portal-perf` (publiek, direct access grants, alleen voor loadtests). Die staat bewust **niet** in de standaard realm-import `docker-compose/imports/keycloak`; het seed-script maakt hem aan via de Keycloak-admin-API uit `seed/keycloak/nl-portal-perf-client.json`.

De data is deterministisch: gebruikersnaam, BSN, omschrijvingen en datums volgen uit het volgnummer, nooit uit klok of toeval. Twee runs op een lege stack geven dus dezelfde data (zaak- en documentidentificaties, titels, datums, BSN's); alleen de door Open Zaak zelf gegenereerde UUID's verschillen. Een tweede run op een al geseede stack slaat bestaande gebruikers, zaken en taken over; bij een afwijkend aantal stopt hij met de melding een lege stack te starten (`docker compose down -v`).

```shell
mise run perf:seed
PERF_USERNAME=burger-001 PERF_PASSWORD=perf-wachtwoord PERF_CLIENT_ID=nl-portal-perf mise run perf:smoke
```

| Variabele | Standaard |
|---|---|
| `PERF_SEED_GEBRUIKERS`, `PERF_SEED_ZAKEN`, `PERF_SEED_TAKEN`, `PERF_SEED_DOCUMENTEN` (per zaak) | 10, 5, 2, 2 |
| `PERF_SEED_PASSWORD` (valt terug op `PERF_PASSWORD`) | `perf-wachtwoord` |
| `PERF_KEYCLOAK_ADMIN`, `PERF_KEYCLOAK_ADMIN_PASSWORD` | `admin`, `admin` |
| `PERF_OPENZAAK_URL`, `PERF_OBJECTEN_URL` | `http://localhost:8001`, `http://localhost:8010` |
| `PERF_ZGW_CLIENT_ID`, `PERF_ZGW_SECRET`, `PERF_OBJECTEN_TOKEN` | de lokale demo-gegevens uit `docker-compose/imports/backend.env` |

Alle standaardwaarden zijn lokale demo-gegevens; zet voor een andere omgeving eigen waarden in `mise.local.toml`. De BSN's van de seed-gebruikers staan niet in de BRP-mock: de profiel/BRP-journey vraagt dus eerst om aanvullende mockdata.

## Draaien

Start eerst de compose-stack (zie [docker-compose/README.md](../docker-compose/README.md)) en geef een
bestaande testgebruiker op. Zet de waarden in je eigen (gitignored) `mise.local.toml` onder `[env]`:

```shell
PERF_USERNAME=<gebruiker> PERF_PASSWORD=<wachtwoord> mise run perf:smoke
```

| Variabele | Standaard |
|---|---|
| `PERF_USERNAME`, `PERF_PASSWORD` | verplicht |
| `PERF_BACKEND_URL` | `http://localhost:8080` |
| `PERF_KEYCLOAK_URL` | `http://localhost:8082/auth` |
| `PERF_REALM` | `nlportal` |
| `PERF_CLIENT_ID` | `nl-portal` |

`perf:smoke` eindigt met een exitcode ongelijk aan 0 bij een falend verzoek, een falende check of een
gemiste threshold.

## Taken

- `mise run perf:seed`: de seed hierboven.
- `mise run perf:smoke`: de smoke-test tegen de draaiende stack.
- `mise run perf:test`: unit-tests van de helpers (`lib/*.test.ts` en `seed/*.test.ts`, `node --test`); onderdeel van `mise run test`.
- `mise run perf:check`: laadt het script zonder stack (syntax en imports); onderdeel van `mise run lint`.

De perf-overlay en de overige profielen volgen in de andere sub-tickets van #428.
