# NL Portal Backend Application

The shippable backend application of NL Portal. It assembles the backend library modules (`core`,
`graphql`, `case`, `form`, `zgw`, ...) into a single runnable Spring Boot service and is packaged as
the `nl-portal-backend` image (Dockerfile at [`backend/Dockerfile`](../Dockerfile)).

This is the production-shaped app: it carries no producten/thema demo and ships with openproduct off.
Active feature work and the demo live in the developer-only dev app `:app-dev`
([backend/app-dev/README.md](../app-dev/README.md)), which is configured entirely from its own
`application.yml` and is never published or imaged.

## Running locally

Start the supporting services (the default `sources` mode leaves the app ports `8080`/`8000`/`3000`
free for an IDE/Gradle run), create your local config, then run the app:

```shell
cd ../../docker-compose && docker compose --profile zgw up -d
cd ../backend/app && cp .env.properties.example .env.properties
cd .. && ./gradlew :app:bootRun
```

The GraphQL endpoint is served at `/graphql`. Keycloak and ZGW service URLs (and the test
users to log in with) are provided by the `docker-compose/` stack - see its
[README](../../docker-compose/README.md) for service ports and credentials.

## Configuration

The app has a single config surface: **`src/main/resources/config/application.yml`** - the
base/production config, fully externalized via `${ENV}` variables and prod-safe (GraphQL
introspection is off). There is no Spring `dev` profile; every run (container or from-source) uses
this same config and supplies differences purely through environment variables.

### Local development (`bootRun`)

`./gradlew :app:bootRun` loads its environment from **`.env.properties`** in this directory. This is
the *only* config source for a from-source run - it is independent of
[`docker-compose/imports/backend.env`](../../docker-compose/imports/backend.env), which configures
the *containerised* backend under `RUN_MODE=local`/`remote`.

`.env.properties` is **gitignored** (it may hold local secrets). Copy the tracked template and adjust
it for your machine:

```shell
cp .env.properties.example .env.properties
```

`bootRun` fails with a clear message if the file is missing. The template assumes the supporting
stack runs via `docker compose --profile zgw up -d` (default `sources` mode), so every service is reachable
on `localhost:<published-port>` from the host. It differs from the containerised
`backend.env` in a few ways:

| Setting | Container (`backend.env`) | Host (`.env.properties`) | Why |
|---|---|---|---|
| `LOGLEVEL` | `INFO` | `DEBUG` | verbose inner dev loop |
| `SPRING_GRAPHQL_SCHEMA_INTROSPECTION_ENABLED` | unset (off) | `true` | serve an introspectable schema for frontend codegen (never on in the shipped image) |
| `CONFIGURATION_PANEL_URI`, `..._PREFILL_..._TYPEURL` | `host.docker.internal:<port>` | `localhost:<port>` | the host reaches these services on `localhost`, not `host.docker.internal` |

ClamAV virus scanning runs by default: the `clamav` service starts with the compose stack (no profile)
and `.env.properties.example` already ships `NLPORTAL_CONFIG_VIRUSSCAN_CLAMAV_ENABLED=true`, so a
from-source run scans uploads out of the box. Set it to `false` in `.env.properties` to opt out.

When running as a container, supply configuration through environment variables instead.
