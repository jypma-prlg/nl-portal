# nl-portal-docker-compose

<!-- TOC -->
* [nl-portal-docker-compose](#nl-portal-docker-compose)
  * [Getting started](#getting-started)
    * [Requirements](#requirements)
    * [Compose structure and profiles](#compose-structure-and-profiles)
    * [Running the application](#running-the-application)
      * [Local (build from source)](#local-build-from-source)
      * [Remote (published images)](#remote-published-images)
      * [From sources (active development, fast inner loop)](#from-sources-active-development-fast-inner-loop)
    * [Starting up supporting services only](#starting-up-supporting-services-only)
      * [Including all ZGW related services](#including-all-zgw-related-services)
      * [Keycloak only](#keycloak-only)
  * [Maintenance](#maintenance)
    * [Demo data imports](#demo-data-imports)
    * [Regenerating fixtures](#regenerating-fixtures)
  * [Configuratie en extern netwerk](#configuratie-en-extern-netwerk)
  * [Known issues](#known-issues)
<!-- TOC -->

## Getting started
### Requirements
- [Docker Desktop](https://docs.docker.com/desktop/install/)
- [Docker Compose](https://docs.docker.com/compose/install/)

> **Linux (Docker Engine, not Docker Desktop):** the `local` build mode (`RUN_MODE=local ... --build`)
> needs the [Buildx](https://docs.docker.com/build/concepts/overview/) plugin, which Docker Desktop
> bundles but a plain Docker Engine install does not. Without it the build fails. Install it from
> Docker's package repo, e.g. `sudo apt-get install docker-buildx-plugin` (Debian/Ubuntu) or the
> equivalent `docker-buildx` package for your distro. The `remote` and `sources` modes do not need it.

### Compose structure and profiles
The compose file is built up out of sets of containers (profiles) that each enable a piece of
functionality, so you only start what you need. A service without a profile is a core
requirement and is always started (even with a bare `docker compose up`). Profiles combine:
e.g. `--profile zgw` starts the full ZGW suite, while the granular `openzaak` / `objecten` /
`openklant` / `openproduct` profiles let you start a single component of it.

Whether (and how) the NL Portal app itself runs is a separate dimension, controlled by the
`RUN_MODE` variable rather than a profile: `sources` (default) runs no app container and leaves the
app ports free, `local` builds and runs the app from source, `remote` runs the published images.
See [Running the application](#running-the-application).

All ports below are exposed via the `localhost` helper container (the services share its
network namespace), so from your host every service is reachable on `localhost:<port>`.

| Service                                 | Port  | Profile / RUN_MODE|
|-----------------------------------------|-------|-------------------|
| NL Portal Backend (build from source)   | 8080  | RUN_MODE=local    |
| NL Portal Frontend (build from source)  | 3000  | RUN_MODE=local    |
| NL Portal Backend (published image)     | 8080  | RUN_MODE=remote   |
| NL Portal Backend beheerpoort (`/actuator/modulestatus`) | 8091 | RUN_MODE=local, remote |
| NL Portal Frontend (published image)    | 3000  | RUN_MODE=remote   |
| Keycloak                                | 8082  | - (core)          |
| Keycloak Database (postgres)            | -     | - (core)          |
| Open Zaak                               | 8001  | zgw, openzaak     |
| Open Zaak Database (postgis)            | -     | zgw, openzaak     |
| OpenKlant 2                             | 8014  | zgw, openklant    |
| OpenKlant 2 Database (postgres)         | -     | zgw, openklant    |
| Open Object                             | 8010  | zgw, objecten     |
| Open Notificaties                       | 8012  | zgw, objecten     |
| OpenProduct                             | 8015  | zgw, openproduct  |
| Redis                                   | -     | zgw (sub-profiles)|
| Haalcentraal BRP                        | 5010  | haalcentraal      |
| Haalcentraal Bewoning                   | 5011  | haalcentraal      |
| ClamAV virus scan                       | 3310  | (default)         |

> **NB!** `RUN_MODE` selects exactly one app-run mode, so `local` and `remote` can never bind the
> app ports 8080/3000 at the same time. ClamAV now runs by default (no profile) and virus scanning is
> enabled in `imports/backend.env`; its image is large and its healthcheck has a ~120s start period
> (virus-DB download), so the first `up` on a cold cache is slower.

### Running the application
This stack can run the whole NL Portal product (backend + frontend) together with its
supporting services. NL Portal is now a single monorepo, so the images are named
`nl-portal-backend` and `nl-portal-frontend` (the old `nl-portal-app-*` names are retired).
The `RUN_MODE` variable selects how the app runs (`sources` default / `local` / `remote`); the
supporting-service profiles (`zgw`, `config`, ...) are chosen independently:

#### Local (build from source)
`RUN_MODE=local` builds the backend and frontend images straight from this monorepo's `../backend`
and `../frontend` sources (the frontend builds the app together with its workspace libraries) and
runs them containerized. Use this to verify a change in its shipped form:
```shell
RUN_MODE=local docker compose --profile zgw --profile haalcentraal up -d --build
```

#### Remote (published images)
`RUN_MODE=remote` pulls the published images `ghcr.io/pinkroccade/nl-portal-backend` and
`ghcr.io/pinkroccade/nl-portal-frontend` instead of building — useful for evaluating
the product without a local build. The tag is required and comes from `NL_PORTAL_IMAGE_TAG`:
```shell
RUN_MODE=remote NL_PORTAL_IMAGE_TAG=<tag> docker compose --profile zgw --profile haalcentraal up -d
```
> Release `0.1.0` is published under `ghcr.io/pinkroccade`; version (`0.1.0`), minor (`0.1`), major (`0`)
> and `latest` tags all work. Snapshot tags (`<version>-main-<timestamp>-<sha>-SNAPSHOT`, one per push
> to `main`) also exist. Each release image is signed and carries a CycloneDX SBOM attestation and the
> label `nl.nlportal.sbom` (see [SBOM](../site/development/workflows/sbom.md)). The older
> `ghcr.io/nl-portal/*` images are Ritense's and no longer receive updates from this repository.
> Either way, make sure the `imports/` fixtures and configuration in this
> stack are compatible with the chosen tag.

#### From sources (active development, fast inner loop)
Run only the supporting services in compose and run the backend and frontend yourself for fast
reload. `sources` is the default `RUN_MODE`, so the stack does **not** publish the app ports
(`8080`/`8000`/`3000`), leaving them free for your locally-run app — no `RUN_MODE` needs to be set:
```shell
# supporting services only (app ports left free)
docker compose --profile zgw up -d
```
There are two app pairs. For **feature work** run the developer-only dev apps (all modules plus GraphQL
introspection on, the producten/thema demo, never published). This is the default inner loop:
```shell
# backend dev app: no .env.properties, configured from its own application.yml
cd ../backend && ./gradlew :app-dev:bootRun

# frontend dev app on :3000: libraries in watch plus the dev app's vite dev server
cd ../frontend && pnpm install && pnpm dev
```
To run the **shippable app** from sources instead (production-shaped: no demo, openproduct off), use the
`:app` backend and the `app:`-prefixed frontend script:
```shell
cd ../backend && ./gradlew :app:bootRun          # needs .env.properties (see backend/app/README.md)
cd ../frontend && pnpm app:dev
```
Backend GraphQL is at http://localhost:8080/graphql, frontend at http://localhost:3000. `pnpm dev`
builds the `@nl-portal/*` libraries in watch mode and serves the dev app (`pnpm app:dev` serves the
shippable app); changing a library rebuilds it and the app hot-reloads. Full config details:
[`backend/app/README.md`](../backend/app/README.md#local-development-bootrun) and
[`frontend/README.md`](../frontend/README.md).

> **Setting `RUN_MODE` on Windows.** This mode is the default, so it needs no `RUN_MODE` on Windows.
> It is the `local`/`remote` modes that do, and `RUN_MODE=remote docker compose ...` inline is
> POSIX-shell syntax that does **not** work in PowerShell or CMD (the variable is silently ignored and
> the stack falls back to the `sources` default, so no app is started). Set it separately instead:
> - **PowerShell:** `$env:RUN_MODE="remote"; $env:NL_PORTAL_IMAGE_TAG="<tag>"; docker compose --profile zgw up -d`
> - **CMD:** `set "RUN_MODE=remote" && set "NL_PORTAL_IMAGE_TAG=<tag>" && docker compose --profile zgw up -d`
> - **Any shell:** add `RUN_MODE=remote` (and `NL_PORTAL_IMAGE_TAG=<tag>`) to a `.env` file in this
>   directory (Compose reads it automatically). This persists across runs, so remove it to go back
>   to the `sources` default.

### Configuratie en extern netwerk
Alles start met `docker compose up`; knoppen staan in een `.env` (kopieer `.env.example`).

- **Flags:** `imports/flags/nl-portal.flagd.json` (flagd-bestand) wordt read-only in de backend gemount; een wijziging werkt na een herstart van de backend (op een cluster via een rolling update). Welke flags (ook alle frontend-toggles) er zijn en wat ze doen: [feature flags-referentie](../site/hosting-en-configuratie/feature-flags.md).
- **Koppelingen:** `imports/koppelingen/` bevat per koppeling endpoint en authenticatie, met geheimen als bestand in `secrets/` (gitignored). Zie [imports/koppelingen/README.md](imports/koppelingen/README.md).
- **Netwerk `mijngemeente`:** met `NETWERK=mijngemeente` sluit de stack aan op het externe Docker-netwerk met de API's (ZGW, Haal Centraal, Diftar). Maak het netwerk eenmalig aan met `docker network create mijngemeente`.
- **Pentest-doelwit:** `RUN_MODE=local`: gebouwd uit de bronnen, met de geïmporteerde realm en testgebruikers.

### Performance-overlay en tracing
`compose.perf.yaml` is een overlay voor performancetesten (zie
[ADR 0018](../site/development/adr/0018-performancetesten-met-k6-en-otel-agent-in-overlay.md)). Hij werkt
met `RUN_MODE=local` of `remote` (niet met `sources`, want dan draait de backend niet in compose).

- `mise run perf:stack-up`: de stack zonder tracing. Zo draaien baselineruns.
- `mise run perf:stack-up-tracing`: dezelfde stack plus de OpenTelemetry Java-agent op de portal-backend,
  OTel aan op OpenZaak en Objecten, en Grafana Tempo als collector (100% sampling). Zo draait de diagnostische run.
- `mise run perf:stack-down`: stopt de stack en ruimt de volumes op.

De agent komt alleen via een volume in de backend-container (init-service `otel-javaagent`); de
backend-image en het release-artefact blijven ongewijzigd. Een trage request vind je terug in Tempo
op `http://localhost:3200/api/traces/<trace-id>`; de trace-id is het tweede deel van de `traceparent`-header
die k6 meestuurt (`00-<trace-id>-<span-id>-01`). Een trace loopt van portal-backend naar OpenZaak.

### Starting up supporting services only
To run only the supporting services (e.g. when running the application from your IDE),
leave `RUN_MODE` at its default (`sources`), which starts no app container. Two options:
- Including all ZGW related services, like Open Zaak, Open Klant and Open Object (Objects API and
  Objecttypes API, served together by one container)
- Keycloak only

#### Including all ZGW related services
Execute the following command:
```shell
docker compose --profile zgw up -d
```

The following services will be started:

| Service                           |      Mapped port      |
|-----------------------------------|:-------------:|
| Keycloak                          |  8082         |
| Keycloak database (postgres)      |    -   |
| Open Zaak                         | 8001 |
| Open Zaak database (postgis)      | - |
| Open Objecten                     | 8010 |
| Open Objecten database (postgis)  | - |
| Open Notificaties                 | 8012 |
| Open Notificaties database (postgres) | - |
| OpenKlant 2                       | 8014 |
| OpenKlant 2 database (postgres)   | - |
| OpenProduct                       | 8015 |
| OpenProduct database (postgres)   | - |
| Redis                             | - |

#### Haalcentraal only
Execute the following command:
```shell
docker compose --profile haalcentraal up -d
```

| Service      | Mapped port |
|--------------|:-----------:|
| Haalcentraal BRP |    5010     |
| Haalcentraal Bewoning     |    5011     |

#### ClamAV virusscanner only
Execute the following command:
```shell
docker compose --profile clamav up -d
```

| Service               | Mapped port |
|-----------------------|:-----------:|
| ClamAV                |    3310     |

#### Keycloak only
Execute the following command: 
```shell
docker compose up -d
```

| Service   |      Mapped port      |
|----------|:-------------:|
| Keycloak |  8082         |
| Keycloak database (postgres) |    -   |


#### To run all containers Keycloak, ZGW related services, HaalCentraal and ClamAV
Execute the following command:
```shell
docker compose --profile zgw --profile haalcentraal --profile clamav up -d
```

## Maintenance

### Demo data imports

> Uitgebreide, Nederlandstalige uitleg (wanneer gebruiken, waar automatisch geladen, bijwerken, `mise run fixtures:check`): [site/development/demo-fixtures.md](../site/development/demo-fixtures.md).

All demo data is seeded as model-serialized Django fixtures loaded with `manage.py loaddata` after
`migrate`. There is no raw SQL seeding: nothing is mounted into a service's postgres container at
`/docker-entrypoint-initdb.d/`.

Every service uses the same wiring: the app container runs the image's own default `/start.sh`, which
applies migrations and creates the `admin` superuser from the image's
`<SERVICE>_SUPERUSER_USERNAME` / `_EMAIL` / `_PASSWORD` env triplet; the password is generated by `mise run dev:setup`
(`<SERVICE>_ADMIN_PASSWORD` in `.env`). A dedicated `*-import` sidecar
reuses the service image, mounts `imports/<service>/fixtures` into the image's app package,
`imports/<service>/init` and the generated `.generated/<service>` (read-only, at `/app/generated`), and runs `imports/<service>/init/init.sh`, which only `loaddata`s the
fixtures. Every import container uses that same path and script name. This applies to `openzaak`,
`open-object`, `open-notificaties`, `openproduct`, and `openklant-2`. The `openproduct` import sidecar
additionally loads the UPL reference list via
`manage.py load_upl --file imports/openproduct/init/UPL-actueel.csv`.

No service overrides the image's launcher. The exact name of the port and password env vars differs
per image, so check the image's own `/start.sh` before changing them:

| Service | Port var | Superuser password var |
|---|---|---|
| `openzaak` | `OPENZAAK_PORT` | `DJANGO_SUPERUSER_PASSWORD` |
| `openklant-2` | `OPENKLANT_PORT` | not supported, see below |
| `open-object` | `OPENOBJECT_PORT` | `OBJECTS_SUPERUSER_PASSWORD` |
| `open-notificaties` | `OPENNOTIFICATIES_PORT` | `DJANGO_SUPERUSER_PASSWORD` |
| `openproduct` | `OPENPRODUCT_PORT` | `OPENPRODUCT_SUPERUSER_PASSWORD` |

`openklant-2` is the one exception. Its image has no superuser hook in `/start.sh` at all, and its
`createinitialsuperuser` command generates a random password and mails it. Its `admin` user is
therefore seeded like any other row, from the `admin_user.json` fixture that `mise run dev:setup` renders from
`templates/openklant-2/admin_user.json` with a Django password hash of the generated `OPENKLANT_ADMIN_PASSWORD`.

Each import sidecar gates on `python /app/src/manage.py migrate --check` before seeding: it is
read-only and exits 0 only once every migration in the pinned image is applied, so the load never
races ahead of migrations and needs no hardcoded per-image sentinel table. This poll is required
because none of the pinned `open-*` images ship a Dockerfile `HEALTHCHECK`, so
`depends_on: condition: service_healthy` is not available to gate the sidecars. Verify with
`docker image inspect <image> --format '{{json .Config.Healthcheck}}'` before assuming otherwise; the
healthchecks mentioned in the upstream changelogs are the celery worker liveness probes, not the app
image.

Because fixtures are model-serialized, a minor image bump that only adds nullable columns imports
without changes.

### Regenerating fixtures

Fixtures are committed artifacts. To regenerate after a demo-data or schema change, seed a container
with the desired data, then dump the relevant apps/models:
```shell
docker compose --profile <profile> up -d
docker compose exec <service> python /app/src/manage.py dumpdata <apps> --indent 2 -o <fixture.json>
```
Copy the resulting JSON into `imports/<service>/fixtures/` and commit it. The fixtures that carry secrets
(`configuration.json` and Open Klant's `admin_user.json`) are templates under `templates/<service>/`: replace every
secret or token in the dump with a placeholder of the form double braces around a `SECRET_NAMES` entry from
`scripts/dev-setup.mjs`, never commit the real value.

`templates/openklant-2/admin_user.json` is a `dumpdata accounts.user` artifact of an
openklant superuser, with the password replaced by a placeholder for the generated hash, with `last_login` nulled and `date_joined` pinned to a fixed timestamp so
the file stays diff-stable across regenerations. Regenerate it the same way if openklant's user model
changes:
```shell
docker compose exec nl-portal-openklant-2 sh -c \
  'DJANGO_SUPERUSER_PASSWORD=<tijdelijk> python src/manage.py createsuperuser \
     --noinput --username admin --email admin@example.org'
docker compose exec nl-portal-openklant-2 python src/manage.py dumpdata accounts.user --indent 2
```
The openklant integration-test stack under `backend/docker-resources/imports/openklant-2/` keeps its own
fixed test fixtures; it is not part of `dev:setup`.

## Known issues

* Running on Windows  
If the services within this compose file fail to start up with the following error or similar:  
    ```log
    2025-11-18 11:15:41.901 | /app/init/init.sh: 2: 
    2025-11-18 11:15:41.902 | Apply database migrations
    2025-11-18 11:15:59.325 | Unknown command: 'migrate\r'. Did you mean migrate?
    2025-11-18 11:15:59.325 | Type 'manage.py help' for usage.
    2025-11-18 11:16:04.229 | Traceback (most recent call last):
    2025-11-18 11:16:04.229 |   File "/usr/local/lib/python3.12/site-packages/django/db/backends/base/base.py", line 279, in ensure_connection
    2025-11-18 11:16:04.235 |     self.connect()
    2025-11-18 11:16:04.235 |   File "/usr/local/lib/python3.12/site-packages/django/utils/asyncio.py", line 26, in inner
    2025-11-18 11:16:04.237 |     return func(*args, **kwargs)
    2025-11-18 11:16:04.237 |            ^^^^^^^^^^^^^^^^^^^^^
    2025-11-18 11:16:04.237 |   File "/usr/local/lib/python3.12/site-packages/django/db/backends/base/base.py", line 256, in connect
    2025-11-18 11:16:04.237 |     self.connection = self.get_new_connection(conn_params)
    2025-11-18 11:16:04.237 |                       
    ```
  Then that means either your editor or git is replacing linux line endings with windows compatible crlf line endings.
  To work with this repository you need to make sure that doesn't happen:
  * Git: https://docs.github.com/en/get-started/git-basics/configuring-git-to-handle-line-endings?platform=windows#about-line-endings  
    * Make sure that the `core.autocrlf` property in git is set to `false` and then pull the project again.
