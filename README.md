# NL Portal

![License EUPL 1.2](https://img.shields.io/badge/License-EUPL%201.2-blue)

**Frontend**

[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=pinkroccade_nl-portal-frontend&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=pinkroccade_nl-portal-frontend)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=pinkroccade_nl-portal-frontend&metric=coverage)](https://sonarcloud.io/summary/new_code?id=pinkroccade_nl-portal-frontend)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=pinkroccade_nl-portal-frontend&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=pinkroccade_nl-portal-frontend)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=pinkroccade_nl-portal-frontend&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=pinkroccade_nl-portal-frontend)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=pinkroccade_nl-portal-frontend&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=pinkroccade_nl-portal-frontend)

**Backend**

[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=pinkroccade_nl-portal-backend&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=pinkroccade_nl-portal-backend)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=pinkroccade_nl-portal-backend&metric=coverage)](https://sonarcloud.io/summary/new_code?id=pinkroccade_nl-portal-backend)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=pinkroccade_nl-portal-backend&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=pinkroccade_nl-portal-backend)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=pinkroccade_nl-portal-backend&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=pinkroccade_nl-portal-backend)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=pinkroccade_nl-portal-backend&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=pinkroccade_nl-portal-backend)

NL Portal is an open-source application for communicating with citizens and third parties, built for
use with the Dutch 'VNG API's voor Zaakgericht Werken' and based on Common Ground principles. Under
the motto 'create once, use 340 times', any (government) organization can use and improve it freely.

This is the NL Portal **monorepo**: the backend, frontend, local development stack and documentation
live together and release at a single, shared version.

## Versionering en herkomst

NL Portal is gebaseerd op de originele repository op versie 4.1.0 en heeft sindsdien een eigen
versielijn. Alle `0.x`-versies zijn experimenteel; `1.0.0` is de eerste productie-release, vanaf
dan gelden SemVer en een deprecation-periode voor breuken (zie
[ADR 0012](./site/development/adr/0012-breaking-changes-tot-eerste-productie-release.md)).

## What's in here

| Path              | Contents                                                                                          |
| ----------------- | ------------------------------------------------------------------------------------------------- |
| `backend/`        | Kotlin/Spring Boot backend libraries + the shippable backend application. One Gradle build. See [backend/README.md](./backend/README.md). |
| `frontend/`       | pnpm workspace of `@nl-portal/*` React libraries + the shippable frontend application. See [frontend/README.md](./frontend/README.md). |
| `docker-compose/` | Local development stack (Keycloak, ZGW services) + fixtures. See [docker-compose/README.md](./docker-compose/README.md). |
| `performance/` | k6 performance tests (smoke now; see [performance/README.md](./performance/README.md)). |
| `site/`           | Documentation site (VitePress): architecture, usage, hosting, development. See [site/index.md](./site/index.md). |

Helm charts previously lived in Ritense's separate `nl-portal/helm-charts` repository; pinkroccade's
own equivalent is tracked in [epic #191](https://github.com/pinkroccade/nl-portal/issues/191).

## Running NL Portal

Three ways, by purpose - see [GETTING_STARTED.md](./GETTING_STARTED.md) for the commands:

- **Remote** - run the published images to evaluate the product (no build).
- **Local** - build and run your changes as they'd ship.
- **From sources** - run the backend/frontend from your IDE for fast development.

For deployment, see [Hosting & configuratie](./site/hosting-en-configuratie/index.md) (see [epic #191](https://github.com/pinkroccade/nl-portal/issues/191) for the helm-charts follow-up).

## Developing

Each stack has its own README with setup, build and run instructions:

- Backend: [backend/README.md](./backend/README.md)
- Frontend: [frontend/README.md](./frontend/README.md)
- Local supporting services: [docker-compose/README.md](./docker-compose/README.md)

Branch model, commit conventions and workflow are in [CONTRIBUTING.md](./CONTRIBUTING.md). Please
also read the [Code of Conduct](./CODE_OF_CONDUCT.md). Report vulnerabilities privately per the
[Security Policy](./SECURITY.md).

## Documentation

All documentation - architecture, configuration, feature/theme guides, ADRs and workflows - lives in
one VitePress site under [`site/`](./site) (`mise run docs:dev` to preview it). It is published at
<https://pinkroccade.github.io/nl-portal/> (GitHub Pages, via `.github/workflows/publish-docs.yml`).
Sections: [Architectuur](./site/architectuur/index.md), [Gebruik & inrichting](./site/gebruik-en-inrichting/index.md),
[Hosting & configuratie](./site/hosting-en-configuratie/index.md), [Development](./site/development/index.md).

## License

Licensed under the EUPL 1.2 - see [LICENSE.MD](./LICENSE.MD).
