# NL Portal Frontend

![Node 24](https://img.shields.io/badge/Node-24-green)

The frontend stack of the [NL Portal monorepo](../README.md). It is a single
[pnpm workspace](https://pnpm.io/workspaces) of React packages that together provide a configurable
portal implementation for municipalities, built on components that follow the
[NL Design System](https://designsystem.gebruikercentraal.nl/). Look and feel is customized through
design tokens; the back-end systems it talks to are configured at runtime.

## Packages

Reusable libraries, published as `@nl-portal/*`:

- `api` - GraphQL client + generated hooks (Apollo Client).
- `authentication` - OIDC authentication.
- `localization` - translations and i18n.
- `user-interface` - the portal UI components and pages.

And the shippable application:

- `app` (`nl-portal-app`) - the runnable portal that consumes the libraries via `workspace:*` and is
  packaged as the `nl-portal-frontend` image (Dockerfile at [`frontend/Dockerfile`](./Dockerfile)).

## Requirements

- Node 24
- pnpm 11 (`corepack enable` or see https://pnpm.io/installation)

## Getting started

From `frontend/`:

```shell
pnpm install   # install all workspace dependencies
pnpm dev       # build the libraries in watch mode and serve the DEV app on :3000
```

There are two app packages: `nl-portal-app` (the shippable app) and `nl-portal-app-dev` (a
developer-only sandbox that carries work-in-progress demos and is never published, see
[packages/app-dev](./packages/app-dev/README.md)). The root scripts default to the dev app; add the
`app:` prefix for the stable app:

| Dev app (default) | Stable app         |
| ----------------- | ------------------ |
| `pnpm dev`        | `pnpm app:dev`     |
| `pnpm build`      | `pnpm app:build`   |
| `pnpm preview`    | `pnpm app:preview` |

Each `dev` script runs its package's dependencies' dev scripts in parallel: the libraries build in watch
mode (via `vite build --watch`) and the app waits for them, then starts its vite dev server. For a
one-off build of every package instead, use `pnpm -r build` (this is what CI runs).

## Quality checks

Run from `frontend/`:

```shell
pnpm test          # vitest
pnpm lint:css      # stylelint
pnpm prettier      # formatting check (pnpm prettier:fix to apply)
```

`pnpm prettier` is the quality gate. A husky pre-commit hook runs `lint-staged` over the staged files
(prettier, stylelint and a `tsc --noEmit` type-check) and `commitlint` over the commit message, so
these checks run automatically before a commit lands. Run `pnpm prettier:fix` to auto-format.

## GraphQL codegen

Only `packages/api` runs codegen (`pnpm codegen`), generating typed hooks from the query files. The
generated output is committed, so consumers reuse it without regenerating. Regeneration requires the
GraphQL endpoint (the backend) to be reachable.

## PWA assets

Both app packages ship PWA icons and splash screens in `public/pwa`, generated from
`src/assets/pwa-logo.svg`. The output is committed (56 files per package), so installing and
building never regenerate it.

Regenerate only after changing the source logo, from `packages/app` or `packages/app-dev`:

```shell
pnpm generate-pwa-assets
```

The generator (`pwa-asset-generator`) is deliberately **not** a dependency; the script fetches it
on demand via `pnpm dlx`. Regenerating therefore needs network access and downloads a headless
Chromium, while a normal `pnpm install` stays free of the puppeteer tree.

The run also rewrites the icon and splash-screen `<head>` links in `index.html` and the `icons`
entries in `public/manifest.json`, so review those two files in the diff alongside the images.
Run it in both app packages so the shippable app and the dev app stay in sync.

## Consuming snapshot builds

Every push to `main` publishes the libraries as **snapshot** tarballs to a public S3 bucket.
(Tagged releases are published to the npm registry as `@nl-portal/*`; that is the primary channel
and installs with plain `pnpm add @nl-portal/nl-portal-<pkg>`.) Snapshots are not on a registry, so
install them by URL, pinning the exact build:

```shell
pnpm add https://nl-portal-frontend-snapshots.s3.eu-central-1.amazonaws.com/<version>-SNAPSHOT/nl-portal-nl-portal-user-interface.tgz
```

The `@nl-portal/` scope is stripped in the tarball filename (`nl-portal-nl-portal-<pkg>.tgz`, where
`<pkg>` is `api`, `authentication`, `localization` or `user-interface`). The bucket is public-read;
no credentials are needed. Each build publishes a unique version, so there is no floating `latest`.

## Configuration

Runtime configuration comes from two places:

- **Feature toggles and properties** are served by the backend at `GET /api/public/features` (from
  `nl-portal.config.features.*`) and read by the `user-interface` app context. `window.*` no longer
  carries feature config.
- **OIDC and API URLs** are the only `window.*` globals left, set by `packages/app/public/config.js`
  (local dev defaults) and, in a container, `config.template.js` (populated from environment
  variables at startup via nginx `envsubst`).

See the [documentation](../documentation) for the feature and theming configuration.

## More information

See the [monorepo README](../README.md) and the [documentation](../documentation).
