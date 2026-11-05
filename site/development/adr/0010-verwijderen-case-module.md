# 10. Verwijderen van de case-module

## Status

Geaccepteerd en uitgevoerd (module verwijderd, #103). Omdat de applicatie nog niet in productie
draait, is er geen opruim-migratie voor bestaande `case`-tabellen toegevoegd: er verdwijnt niets uit
live systemen en er blijven geen restanten achter in de codebase.

## Context

De capability-inventaris (`docs/capabilities.yaml`, capability `eigen-zaken-case`) laat zien dat
`backend/case` een volledige eigen zaakafhandeling bevat: case-definities, case-instanties, het
indienen van formulieren als case (`allCaseDefinitions`, `allCaseInstances`, `getCaseInstance`,
`processSubmission`) en een listener die case-definities bij opstart deployt.

- Geen van deze operaties wordt door de frontend (`frontend/packages`) gebruikt.
- De functionaliteit overlapt met de ZGW-zaken (`backend/zgw/zaken`), de route die het portaal
  feitelijk gebruikt.
- In de afgelopen 12 maanden zijn er alleen onderhoudscommits geweest (5).

Onderhoud aan een ongebruikte, overlappende module kost tijd (dependency-updates, security-scans,
tests) zonder dat er functionaliteit tegenover staat.

## Beslissing

1. `eigen-zaken-case` krijgt de status `deprecated`.
2. Voordat de code wordt verwijderd, controleren we in de gateway- en applicatielogs of externe
   clients de case-operaties aanroepen. Vinden we geen gebruik, dan verwijderen we de module,
   inclusief de bijbehorende Liquibase-changesets of een expliciete opruim-migratie. Daarna gaat de
   status naar `verwijderd`.
3. Blijkt er wel extern gebruik te zijn, dan herzien we deze ADR.

## Gevolgen

- `backend/case`, de opname in `app`/`app-dev` en de bijbehorende GraphQL-schema's verdwijnen.
- Gemeenten die het portaal zelf hosten krijgen een vermelding in de changelog als breaking change.
- `database-migratie` blijft nodig zolang `form` tabellen gebruikt. Achterhaald: `form` gebruikt geen
  persistentie meer (#135) en de capability, de `data`-module en alle database-infrastructuur zijn
  verwijderd (#142).
