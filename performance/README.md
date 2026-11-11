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
| `scenarios/smoke.ts` | Smoke: 1 gebruiker, 1 iteratie |

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

- `mise run perf:smoke`: de smoke-test tegen de draaiende stack.
- `mise run perf:test`: unit-tests van de helpers (`lib/*.test.ts`, `node --test`); onderdeel van `mise run test`.
- `mise run perf:check`: laadt het script zonder stack (syntax en imports); onderdeel van `mise run lint`.

Seed-data, de perf-overlay en de overige profielen volgen in de andere sub-tickets van #428.
