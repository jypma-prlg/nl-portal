# 15. Copyright-headers in NL-Portal Pink Edition

## Status

Geaccepteerd (#178).

## Context

NL-Portal Pink Edition (werknaam) is een fork van NL Portal van Den Haag en Ritense. Sinds 2026-10-20
(eerste commit `0a343d6b6`) ontwikkelt PinkRoccade Local Government B.V. deze fork zelfstandig;
wijzigingen gaan niet terug naar upstream. Tot nu toe zette de tooling op elk bronbestand
`Copyright 2015-<jaar> Den Haag, Ritense, the Netherlands.`, ook op bestanden die PinkRoccade zelf
heeft gemaakt.

Auteursrecht ontstaat bij het maken van het werk; een copyright-regel stelt het alleen vast. Wat
werknemers van PinkRoccade maken is van PinkRoccade (art. 7 Auteurswet). Er is geen CLA, dus
upstream-bijdragers hebben hun rechten niet overgedragen. De EUPL 1.2 verbiedt het verwijderen van
bestaande copyright-vermeldingen (art. 5) en eist dat een gewijzigd werk duidelijk vermeldt dat en
wanneer het is gewijzigd.

## Beslissing

1. De hele codebase blijft onder **EUPL 1.2**. Pink Edition is een afgeleid werk van een
   EUPL-werk; een andere licentie voor eigen bestanden binnen hetzelfde werk is niet mogelijk.
2. Een bestand met de **Ritense-header houdt die**, ook als PinkRoccade het wijzigt. Het jaartal
   ervan is bevroren: de tooling accepteert elk bestaand jaar en schrijft geen nieuw jaar meer.
3. Elk ander bestand, dus ook elk nieuw bestand, krijgt dezelfde EUPL-header met
   `Copyright <jaar> PinkRoccade Local Government B.V.`. Het eindjaar schuift op wanneer het bestand
   wordt gewijzigd (`2026` wordt `2026-2027`); de check accepteert elk jaartal.
4. De tooling onderscheidt de twee op inhoud: begint het bestand met de Ritense-header, dan geldt het
   Ritense-sjabloon, anders het PinkRoccade-sjabloon. De sjablonen staan op één plek,
   `backend/licenses/`, en worden door backend en frontend gedeeld.
5. Bronbestanden die PinkRoccade sinds 2026-10-20 heeft toegevoegd gaan eenmalig over naar de
   PinkRoccade-header, behalve verplaatste of gekopieerde upstream-code.
6. `NOTICE.md` vermeldt de herkomst en de wijzigingsdatum (EUPL art. 5). `LICENSE.MD` blijft
   ongewijzigd.

## Overwogen alternatieven

- **Header ongewijzigd laten en eigenaarschap aan git overlaten**: geen toolingwerk, maar de header
  noemt dan voor nieuwe code de verkeerde rechthebbende. Dat was acceptabel zolang de wijzigingen
  upstream zouden gaan, niet voor een eigen fork.
- **Een extra PinkRoccade-regel in elk gewijzigd bestand**: juridisch het nauwkeurigst, maar elke
  wijziging raakt dan de header en het onderscheid tussen bestanden van upstream en van PinkRoccade
  vervaagt. Bewust niet gekozen.

## Gevolgen

- Backend: Spotless heeft twee header-stappen (`ritense`, `pinkroccade`) met `onlyIfContentMatches`.
  Het PinkRoccade-jaar schuift alleen op bij `./gradlew spotlessApply`, en dan alleen in bestanden
  die afwijken van `origin/main` (`ratchetFrom`; een andere basis met `-PcopyrightRatchetFrom=<ref>`).
  `spotlessCheck` in CI heeft geen git-geschiedenis nodig. De ophoging draait alleen als de taak
  letterlijk `spotlessApply` heet, en niet in `buildSrc` (daar staan nog geen PinkRoccade-bestanden).
- Frontend: de ESLint-regel `copyright/header` (`frontend/eslint-rules/copyright-header.mjs`)
  vervangt `eslint-plugin-notice`. Lint-staged schuift bij een commit het jaar op in de gecommitte
  bestanden via `eslint.copyright-year.config.mjs`; `pnpm lint` in CI accepteert elk jaartal, zodat
  een build niet op 1 januari faalt.
- Wie een Ritense-header handmatig verwijdert, krijgt de PinkRoccade-header. Dat is zichtbaar in de
  diff en moet in de review worden opgemerkt.
- Of "NL-Portal" in de productnaam mag blijven staan valt buiten deze beslissing (EUPL geeft geen
  merkrechten); dat ligt bij PinkRoccade legal.
