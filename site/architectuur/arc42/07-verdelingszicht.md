# 7. Verdelingszicht (deployment view)

## 7.1 Lokale ontwikkeling

`docker-compose/` draait de ondersteunende diensten (Keycloak, Open Zaak-suite, Haal Centraal,
ClamAV, databases) via profielen (`zgw`, `haalcentraal`, `clamav`, `config`); `RUN_MODE` kiest
onafhankelijk hoe de NL Portal-app zelf draait:

| RUN_MODE | Wat draait waar |
|---|---|
| `sources` (default) | Geen app-container; backend via `./gradlew :app-dev:bootRun`, frontend via `pnpm dev` — snelste innerloop |
| `local` | Backend + frontend gebouwd uit deze monorepo-sources, als container | 
| `remote` | Gepubliceerde images van GHCR (`ghcr.io/pinkroccade/nl-portal-backend`, `...-frontend`); tag via `NL_PORTAL_IMAGE_TAG` |

Zie [`docker-compose/README.md`](https://github.com/pinkroccade/nl-portal/blob/main/docker-compose/README.md) voor de volledige service-/poorttabel.

## 7.2 Build en packaging

- **Backend**: Gradle multi-module build → `nl-portal-backend`-image (`backend/Dockerfile`).
- **Frontend**: pnpm-workspace build → `nl-portal-frontend`-image (`frontend/Dockerfile`).
- **Bibliotheken**: de backendmodules en de `@nl-portal/*`-pakketten worden niet los gepubliceerd
  (geen Maven Central of npm); alleen de twee images worden uitgeleverd.
- **CI** (`.github/workflows/`): `ci.yml` (algemene build/test), `_stage-be.yml`/`_stage-fe.yml`
  (buildartefacten + reports/SBOM), `build-and-publish-*.yml` (snapshot/release-candidate/release),
  `sonarcloud.yml` (kwaliteitspoort), `dependency-check.yml` (beveiligingspoort — zie deliverable 6).

## 7.3 Productie

Buiten deze repository: Helm-charts rollen de twee images (backend, frontend) uit op Kubernetes.
Ritense's `nl-portal/helm-charts` werd hier eerder voor gebruikt; pinkroccade's eigen equivalent is
nog niet ingericht, zie [epic #191](https://github.com/pinkroccade/nl-portal/issues/191). Dit
arc42-document beschrijft die laag niet in detail, maar benoemt wel de aanname die eruit volgt:
**NL Portal heeft geen eigen
database**: de backend is stateless en vereist geen persistentielaag. Een BFF vraagt wel Redis voor sessies, zonder back-up ([ADR 0017](../../development/adr/0017-vluchtige-state-toegestaan.md)). Elke overheidsorganisatie
draait haar eigen instantie tegen haar eigen ZGW-landschap.

## 7.4 Voorgesteld doelbeeld: gedeelde runtime met cellen

> **Voorgesteld, nog niet gebouwd of ingericht.** Dit beschrijft het doelbeeld uit
> [ADR 0017](../../development/adr/0017-gedeelde-multi-tenant-runtime-met-cellen.md). Paragraaf 7.3 blijft het
> geldende beeld totdat die ADR is geaccepteerd en uitgevoerd; er draait nog niets in productie.

Pink biedt NL Portal als SaaS aan voor circa 50 gemeenten (tenants) binnen twee jaar. In plaats van een
instantie per gemeente (7.3) draait één multi-tenant runtime, ingedeeld in **cellen**:

- Een **cel** is dezelfde runtime (zelfde images en chart): een Helm-release met eigen values (cel,
  replica's, tenantlijst) voor een groep tenants. De uitrolvolgorde canarycel naar hoofdcel, met wachttijd
  en health-controle, is een eis aan de gitops-tooling en geen onderdeel van de chart.
  Een cel laadt alleen de tenants en secrets van die cel, en beperkt zo de gevolgen van een fout of
  kwetsbaarheid tot die cel.
- Livegang met minimaal twee productiecellen: een **canarycel** (Pink-eigen tenant en één of twee
  pilotgemeenten) en een **hoofdcel**. Een release gaat eerst naar de canarycel en daarna, na vaste
  controles, naar de hoofdcel. Een tenant met hogere eisen of een eis tot fysieke scheiding krijgt een eigen
  cel (een cel met één tenant).
- Per cel minimaal twee replica's, een PodDisruptionBudget en spreiding over nodes of zones.
- Verkeer loopt via de Kubernetes Gateway API (`HTTPRoute` per gemeentedomein, naar de juiste cel). De
  host-allowlist en het certificaat per tenant horen op die edge. De runtime is alleen via de ingress te
  bereiken; de actuator draait op een aparte managementpoort.
- Per omgeving twee gedeelde Keycloaks: een **publieke ID-broker** (met een realm per tenant, beheerd als code
  via gitops) en een **private ID-broker**, waarvan de portal-runtime nooit tokens accepteert.
- Configuratie komt per tenant uit een bestand in Git (uitgerold door gitops-tooling); secrets via External Secrets Operator,
  per tenant een eigen pad. Elke wijziging is een rolling restart van de eigen cel. Zie
  [8.9](./08-dwarsdoorsnede-concepten.md#89-tenantscheiding-en-configuratiemodel-voorgesteld).

**Context: ArgoCD.** De beoogde gitops-tool is ArgoCD. De Helm-charts blijven gitops-tool-agnostisch; Argo is
alleen de context waartegen we verifiëren dat de generieke charts niet botsen met Argo-best practices (zie
ADR 0017, "Vóór livegang").

Het blijft waar dat de backend stateless is en geen database heeft
([ADR 0013](../../development/adr/0013-stateless-backend-zonder-database.md)).

## 7.5 Open vraag

Er is geen architectuurdocument gevonden dat de productie-topologie (hoeveel replica's, welke
externe systemen zijn per-omgeving vs. gedeeld, hoe secrets worden aangeleverd) beschrijft binnen
déze repository. Als dat elders bestaat (bijv. in het helm-charts-repository), is een verwijzing
hiernaar een goede eerste aanvulling op dit hoofdstuk.
