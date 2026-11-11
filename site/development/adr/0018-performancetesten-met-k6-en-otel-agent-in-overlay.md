# 18. Performancetesten met k6, en tracing via een OTel-agent alleen in de perf-overlay

## Status

Voorgesteld (#428)

## Context

NL Portal heeft geen performance- of loadtests en geen performancebudget (arc42 hoofdstuk 10 noemt
het alleen als toekomstige rij). We willen bottlenecks vinden, een baseline vastleggen en bij
releases zien of een wijziging de app beter of slechter maakt.

De backend is stateless (zie [ADR 0013](./0013-stateless-backend-zonder-database.md)): Spring WebFlux,
coroutines en Spring GraphQL, die de ZGW-API's via HTTP aanroepen. Latency zit dus vooral in downstream
calls en in de resolvers, niet in een eigen database. Een loadtool laat zien *dát* het traag is;
voor *waarom* is server-side telemetrie nodig.

## Beslissing

1. **Grafana k6 is de loadtool.** Scripts zijn TypeScript in `performance/` in de repo-root, met
   `thresholds` als pass/fail, draaiend via `mise run perf:*`.
2. **Het testobject is de volledige compose-stack** (portal-backend, Keycloak, OpenZaak, Objecten) met
   deterministische seed-data, via een overlay `docker-compose/compose.perf.yaml`. Een perf-only
   Keycloak-client met direct access grants leeft alleen in die overlay.
3. **Eerst backend en alleen lezen:** GraphQL-journeys met een echte tokenflow, zonder browser. Schrijf-
   scenario's en frontend-performance volgen apart.
4. **Tracing via de OpenTelemetry Java-agent, alleen in de perf-overlay**, met Grafana Tempo als
   collector. De baseline draait zonder tracing; een aparte diagnostische run draait met 100% tracing.
   Er komt **geen** tracing- of metrics-dependency in het release-artefact.
5. **Resultaten:** k6-summary als JSON-artifact in CI (nachtelijk op `main` en per release-candidate,
   mediaan van 3 runs op GitHub-hosted runners), vergeleken met een baseline-bestand in Git dat alleen
   via een PR wordt bijgewerkt. Eerst signalerend, niet blokkerend; harde thresholds pas nadat de
   variantie bekend is.
6. **Micrometer in de applicatie is uitgesteld** en vraagt een eigen beslissing (zie Gevolgen).

## Overwogen alternatieven

- **Gatling (Kotlin/Java-DSL):** past bij een Kotlin-team en geeft rijke rapporten, maar is zwaarder in
  de Gradle-integratie en kent geen eigen trendopslag. k6 is lichter, draait als losse binary en heeft
  ingebouwde thresholds.
- **JMeter:** GUI-gedreven en slecht reviewbaar in Git.
- **Locust:** vraagt Python, dat in dit project niet geïnstalleerd is.
- **Micrometer Tracing met OTel-bridge in de app:** levert ook productiemonitoring, maar wordt
  onderdeel van elk release-artefact en raakt de SBOM, `verification-metadata.xml` en de
  security-taken. Voor alleen performance-analyse is dat meer impact dan nodig.
- **Grafana Cloud k6 voor opslag en trends:** stuurt testdata naar een derde partij; voor een
  overheidscontext een afweging die we nu niet nodig hebben.
- **Performancetest per PR:** op gedeelde CI-runners te ruisig en te traag.

## Gevolgen

- De baseline op GitHub-hosted runners is een trendindicator met brede marges (±20–30% ruis); een
  dedicated runner is een vervolgstap als de variantie te hoog blijkt.
- Er is een extra tool (k6) in `mise.toml` en een extra map `performance/` met eigen seed-script.
- Wie later Prometheus/Grafana-trends of permanente productiemetrics wil, neemt de beslissing over
  Micrometer in de app dan als nieuwe ADR; deze ADR sluit dat niet uit.
- Zodra de eerste baseline bestaat, krijgt arc42 hoofdstuk 10 een rij met het performancebudget.
