# NL Portal Frontend Dev App

A developer-only sandbox clone of [`packages/app`](../app). It carries work-in-progress feature demos
(currently the producten / thema pages) so developers can iterate without destabilizing the shippable
`nl-portal-app`.

It is **developer-only** and never built into the frontend image.

## Running

From `frontend/`:

```shell
pnpm dev        # dev app (this package) on :3000
pnpm app:dev    # stable app (nl-portal-app) on :3000
```

`build` and `preview` follow the same scheme (`pnpm build`/`preview` drive the dev app,
`pnpm app:build`/`app:preview` the stable app). CI builds everything via `pnpm -r build` and the image
builds only the stable app, so these root scripts are free for developer ergonomics.

Point it at a backend that exposes the openproduct schema. Running [`:app-dev:bootRun`](../../../backend/app-dev/README.md)
is recommended.

## Producten / thema demo (initial scaffold, needs follow-up)

The producten route and the thema pages are an **initial** setup aligned to the current openproduct
fixtures. They need further work as the openproducten feature continues:

- The thema menu and pages are data-driven: the backend returns the top-level themas the **logged-in
  user owns products under**, so log in as the demo user **BSN `999993847`** to see them.
- Only themas backed by the current fixtures are configured (`hoofdthema`, `inkomensondersteuning`,
  `erfpacht`, `wonen-en-bouwen`); menu labels reflect the raw fixture names.
- All thema pages use the same generic overview template. A product whose producttype has no thema link
  in the fixtures (for example the `BELASTINGZAKEN` product) is listed under Producten but is not
  clickable.

Extend `src/constants/themes.ts` (the registry), `src/constants/routes.tsx`, and
`src/constants/menu-items.tsx` as the fixtures grow.
