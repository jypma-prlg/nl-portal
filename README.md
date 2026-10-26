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

## What's in here

| Path              | Contents                                                                                          |
| ----------------- | ------------------------------------------------------------------------------------------------- |
| `backend/`        | Kotlin/Spring Boot backend libraries + the shippable backend application. One Gradle build. See [backend/README.md](./backend/README.md). |
| `frontend/`       | pnpm workspace of `@nl-portal/*` React libraries + the shippable frontend application. See [frontend/README.md](./frontend/README.md). |
| `docker-compose/` | Local development stack (Keycloak, databases, ZGW services) + fixtures. See [docker-compose/README.md](./docker-compose/README.md). |
| `documentation/`  | Product documentation. Not yet published anywhere outside this repository; see [issue #18](https://github.com/jypma-prlg/nl-portal/issues/18). |

Helm charts previously lived in Ritense's separate `nl-portal/helm-charts` repository; pinkroccade's
own equivalent is tracked in [issue #18](https://github.com/jypma-prlg/nl-portal/issues/18).

## Running NL Portal

Three ways, by purpose - see [GETTING_STARTED.md](./GETTING_STARTED.md) for the commands:

- **Remote** - run the published images to evaluate the product (no build).
- **Local** - build and run your changes as they'd ship.
- **From sources** - run the backend/frontend from your IDE for fast development.

For deployment, see [`documentation/`](./documentation) (see [issue #18](https://github.com/jypma-prlg/nl-portal/issues/18) for the helm-charts follow-up).

## Developing

Each stack has its own README with setup, build and run instructions:

- Backend: [backend/README.md](./backend/README.md)
- Frontend: [frontend/README.md](./frontend/README.md)
- Local supporting services: [docker-compose/README.md](./docker-compose/README.md)

Branch model, commit conventions and workflow are in [CONTRIBUTING.md](./CONTRIBUTING.md). Please
also read the [Code of Conduct](./CODE_OF_CONDUCT.md). Report vulnerabilities privately per the
[Security Policy](./SECURITY.md).

## Documentation

Full product documentation - architecture, configuration, and feature/theme guides - lives in
[`documentation/`](./documentation). It is not currently published anywhere outside this
repository; see [issue #18](https://github.com/jypma-prlg/nl-portal/issues/18).

`docs/` and `documentation/` are being consolidated into a single documentation site - see
[ADR 0007](docs/adr/0007-documentatie-architectuur-synthese.md) for where everything will live.

## License

Licensed under the EUPL 1.2 - see [LICENSE.MD](./LICENSE.MD).
