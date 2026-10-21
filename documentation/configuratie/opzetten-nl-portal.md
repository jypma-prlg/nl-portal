# Opzetten NL Portal

### Wat heb je nodig

Om de NL Portal lokaal op te zetten is enige basiskennis van de CLI (command-line interface) vereist. Daarnaast heb je de volgende software nodig:

* [Docker Desktop](https://www.docker.com/products/docker-desktop/) — de enige harde vereiste om de NL Portal demo te draaien.
* Voor het maken van een eigen portaal: [Git](https://git-scm.com/) en een [GitHub](https://github.com/) account. Het bouwen van de applicatie gebeurt via Docker, een lokale JDK- of Node-installatie is niet nodig.

#### Waar kan ik de code vinden

Alle code en informatie is opensource en kan gevonden worden in deze GitHub-repository. De backend, frontend, docker-compose stack en documentatie leven samen in één monorepo.

| Onderdeel                                                                         | Beschrijving                                                                                                    |
| --------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| Deze repository                                                                   | De monorepo: backend (`backend/`), frontend (`frontend/`), docker-compose demo (`docker-compose/`) en docs.    |
| Helm Charts                                                                       | Nog niet ingericht voor pinkroccade; zie [issue #18](https://github.com/jypma-prlg/nl-portal/issues/18).       |

Voor de meeste implementaties gebruik je de gepubliceerde images (zie Route 1). Voor maatwerk dienen de app-directories (`backend/app`, `frontend/packages/app`) als startpunt.

### Route 1: De prebuilt images draaien

De snelste manier om de NL Portal te bekijken is via de kant-en-klare demo-omgeving in de `docker-compose/` map van de monorepo. Die bevat alles wat nodig is:

* De prebuilt NL Portal backend en frontend images (`ghcr.io/nl-portal/nl-portal-backend` en `ghcr.io/nl-portal/nl-portal-frontend`).
* De benodigde ZGW componenten (Open Zaak, Open Object, OpenKlant, OpenProduct) en Haal Centraal mocks, opgedeeld in docker-compose profielen zodat je zelf kiest welke onderdelen je start.
* Een voorgeconfigureerde Keycloak met [standard token exchange (v2)](keycloak-token-exchange-v2.md) (zie ook de [Keycloak configuratie](keycloak.md) pagina).

Clone de monorepo op de gewenste `release/*` branch of release-tag en start de demo-omgeving vanuit `docker-compose/`:

```shell
RUN_MODE=remote docker compose --profile zgw --profile haalcentraal up -d
```

**Let op:** Route 1 draait de gepubliceerde stable image met de bijbehorende demo-imports; image en imports horen als één release bij elkaar. Gebruik daarom een `release/*` branch of een release-tag. Op andere branches bestaat die image (nog) niet — bouw daar lokaal met Route 2.

De NL Portal is daarna bereikbaar op `http://localhost:3000`. Je kunt inloggen met de volgende testgebruikers:

| Gebruikersnaam | Wachtwoord | Identificatie (BSN/KVK) |
| -------------- | ---------- | ----------------------- |
| burger         | burger     | 999993847               |
| bedrijf        | bedrijf    | 14127293                |

**Let op:** het opstarten van alle ZGW componenten kan enkele minuten duren. De `RUN_MODE`-waarden `remote` en `local` kunnen niet tegelijk draaien omdat ze dezelfde poorten gebruiken.

De volledige tabel met services, poorten en profielen vind je in `docker-compose/README.md` in de monorepo.

### Route 2: Zelf bouwen (local)

Draai je eigen wijzigingen zoals ze verscheept zouden worden, of zit je op een branch zonder gepubliceerde images? Bouw de images uit je checkout. Dit werkt op elke branch. Start vanuit `docker-compose/` met:

```shell
RUN_MODE=local docker compose --profile zgw --profile haalcentraal up -d --build
```

De NL Portal is bereikbaar op `http://localhost:3000` met dezelfde testgebruikers als hierboven.

### Route 3: Vanuit broncode (sources)

Voor actieve ontwikkeling draai je alleen de ondersteunende services in compose en start je de backend en frontend zelf (snelle reload). Start vanuit `docker-compose/` met:

```shell
docker compose --profile zgw up -d
```

Start daarna de backend (`./gradlew :app:bootRun` vanuit `backend/`; vereist een `.env.properties`, zie `backend/app/README.md`) en de frontend (`pnpm install && pnpm dev` vanuit `frontend/`). Zie [`GETTING_STARTED.md`](../../GETTING_STARTED.md) en `docker-compose/README.md` voor de details.

### Configureren via het Configuration Panel

Het [Configuration Panel](configuration-panel.md) laat je backend modules, logo en huisstijl configureren zonder code en zonder eigen images te bouwen. Voeg het `config`-profiel toe aan de route die je draait, bijvoorbeeld:

```shell
RUN_MODE=remote docker compose --profile zgw --profile haalcentraal --profile config up -d
```

Het Configuration Panel is bereikbaar op `http://localhost:3001` met gebruikersnaam **admin** en wachtwoord **admin**. Zie de [Configuration Panel](configuration-panel.md) pagina voor het aansluiten van een bestaande NL Portal instantie.

### Maatwerk

Voor uitbreidingen die niet via configuratie mogelijk zijn (eigen componenten, plugins of authenticatiemethoden) dienen de app-directories `backend/app` en `frontend/packages/app` als startpunt. Bouw en draai zoals in Route 2.

### Configuratie via environment variabelen

De NL Portal images kunnen ook volledig geconfigureerd worden via environment variabelen. De volledige referentie met inline documentatie vind je in de monorepo, in de bestanden `docker-compose/imports/backend.env` en `docker-compose/imports/frontend.env`. Zie de [Deployment guide](deployment-guide.md) voor de naamconventie, de module-configuratie en het deployen met Helm.

### Vervolgstappen

Nu je eerste NL Portal draait kun je verder met:

* [Configuration Panel](configuration-panel.md) — configureer API-koppelingen en theming via een UI.
* [Eigen vormgeving](eigen-vormgeving.md) — meer informatie over design tokens en huisstijl.
* [Deployment guide](deployment-guide.md) — deploy de NL Portal naar je eigen omgeving (Azure, AWS of Kubernetes).
* [Keycloak configuratie](keycloak.md) — sluit de NL Portal aan op je eigen Keycloak.
