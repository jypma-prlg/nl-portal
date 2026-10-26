# 5. Build/test-gates: canonieke bron en expliciete tiers

## Status

Geaccepteerd. Zie wayfinder-ticket [#38](https://github.com/jypma-prlg/nl-portal/issues/38) op map
[#37](https://github.com/jypma-prlg/nl-portal/issues/37).

## Context

AGENTS.md, CONTRIBUTING.md en de daadwerkelijke CI (`.github/workflows/ci.yml` →
`_validate-build.yml`) beschrijven elk een ander commando-set voor "backend/frontend build+test is
klaar", en geen van de drie komt overeen met wat de lokale `frontend/.husky/`-hooks en de
post-merge `_stage-fe.yml` daadwerkelijk afdwingen:

- **AGENTS.md** ("Before you call it done"): backend `./gradlew build -x integrationTest` (geen
  Docker nodig); frontend `pnpm -r build && pnpm lint && pnpm lint:css && pnpm typecheck && pnpm
  test && pnpm prettier`.
- **CONTRIBUTING.md** ("Building and testing locally"): backend de volledige `./gradlew build`
  (inclusief integration tests, Docker vereist); frontend `pnpm install && pnpm -r build`, `pnpm
  test`, `pnpm lint`, `pnpm prettier` (zonder `lint:css`/`typecheck`).
- **CI PR-gate** (`_validate-build.yml`, blokkeert merge naar `main`/`release/*.x`): backend de
  volledige `./gradlew build`; frontend **alleen** `pnpm -r build` + `pnpm test` — geen lint,
  lint:css, typecheck of prettier.
- **Lokale git-hooks** (`frontend/.husky/`): pre-commit → `lint-staged` (Prettier op staged
  bestanden); pre-push → `pnpm lint` (inclusief copyright-header-check) + backend `./gradlew
  spotlessCheck`.
- **Post-merge stage-pipeline** (`_stage-fe.yml`, draait ná merge op `main`/`release/*.x`): `pnpm
  lint`.

Dit is grotendeels bedoeld (agents hebben niet gegarandeerd Docker; CI hoeft niet te herhalen wat
een pre-push-hook al garandeert), maar nergens stond dit zo benoemd — elk document leek zijn eigen,
losstaande "waarheid" te claimen.

## Beslissing

1. **Vier tiers, expliciet benoemd, geen poging om ze tot één commando te herleiden:**
   agent-fast-loop (AGENTS.md), lokale pre-push-gate (`frontend/.husky/pre-push`), CI PR-gate
   (`_validate-build.yml`), CI post-merge stage-gate (`_stage-fe.yml`).
2. **Eén nieuw, gedeeld document is de canonieke bron**: `docs/workflows/build-and-test-gates.md`
   (aan te maken als vervolgwerk) bevat één tabel met deze vier tiers, hun exacte commando's, en de
   reden voor elk verschil. AGENTS.md en CONTRIBUTING.md linken ernaar in plaats van zelf een
   commando-lijst te onderhouden.
3. **De integration-test-uitzondering blijft een bewuste uitzondering**, niet iets om later "gelijk
   te trekken": AGENTS.md mag backend-integratietests blijven overslaan omdat niet elke
   agent-sandbox Docker heeft; CI en de mens-georiënteerde CONTRIBUTING.md-instructie blijven de
   volledige build als eindautoriteit houden.
4. **Het CI-gat wordt expliciet gesignaleerd, niet hier gedicht**: de PR-gate verifieert vandaag
   geen frontend lint/lint:css/typecheck/prettier server-side — die bescherming bestaat alleen via
   lokale hooks, die met `--no-verify` te omzeilen zijn of ontbreken in een headless/agent-omgeving.
   Zolang dat zo is, bewijst een groene PR-check niet wat AGENTS.md/CONTRIBUTING.md beweren dat hij
   bewijst, wat het doel "de meeste PR's automatisch afhandelen" ondermijnt. Dit wordt genoteerd als
   verplicht vervolgwerk (een CI-wijziging), niet als onderdeel van dit besluit.

   **Addendum (2026-10-26, via wayfinder-ticket [#68](https://github.com/jypma-prlg/nl-portal/issues/68)
   op map [#66](https://github.com/jypma-prlg/nl-portal/issues/66))**: het gat bleek groter dan hier
   beschreven. `lint:css` en `typecheck` draaiden nergens in CI, zelfs niet post-merge in
   `_stage-fe.yml` — alleen `lint` (ESLint) draaide daar. Het besluit hierboven (vier tiers, de
   integratietest-uitzondering, het CI-gat signaleren) blijft ongewijzigd correct; alleen de omvang
   van het gesignaleerde gat is hiermee gecorrigeerd.

## Consequenties

- Vervolgwerk, buiten deze wayfinder-map: (a) `docs/workflows/build-and-test-gates.md` daadwerkelijk
  schrijven, (b) AGENTS.md en CONTRIBUTING.md aanpassen zodat ze ernaar linken in plaats van
  commando's te dupliceren, (c) de CI PR-gate uitbreiden met frontend lint/lint:css/typecheck/
  prettier zodat die daadwerkelijk afdwingt wat de docs beweren.
- Niemand mag AGENTS.md's `-x integrationTest` later "repareren" om het gelijk te trekken met CI —
  dat zou de bewuste Docker-uitzondering ongedaan maken.
