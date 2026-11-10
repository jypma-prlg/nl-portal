# NL Portal Backend Dev App

A developer-only sandbox clone of [`backend/app`](../app). It lets developers run long-lived feature
work (currently the producten / thema demo) against a fully-enabled backend without touching the
shippable `:app` module.

It is **run from sources only**. It is never built into an image, staged, or published (no GHCR). PR CI
compiles it (`./gradlew build`, including its integration tests), but nothing ships it.

## Running

```shell
cd ../../docker-compose && docker compose --profile zgw --profile haalcentraal up -d
cd ../backend && ./gradlew :app-dev:bootRun
```

No `.env.properties` is needed. Unlike `:app`, this module is configured entirely from its own
[`src/main/resources/config/application.yml`](src/main/resources/config/application.yml), which inlines
concrete local-stack values for the whole `nl-portal.config.*` tree, so `bootRun` needs no environment
setup.

GraphQL is at http://localhost:8080/graphql (GraphiQL and introspection are on). Log in with the test
users from the [docker-compose stack](../../docker-compose/README.md).

Virus scanning (`nl-portal.config.virusscan.clamav`) is enabled, so start the ClamAV service from the
compose stack before exercising document uploads.

## Every module enabled (recommended codegen backend)

`application.yml` enables **every** `nl-portal.config.*` module and turns introspection on. Each enabled
module contributes its queries to the GraphQL schema, so this app exposes the full schema, which makes it
the recommended backend for the frontend GraphQL codegen. With `:app-dev:bootRun` up:

```shell
cd ../../frontend && pnpm --filter @nl-portal/nl-portal-api codegen
```

If a module were missing or disabled, or introspection were off, codegen would silently drop that
module's queries.
