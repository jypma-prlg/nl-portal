# 1. Architectuurbeslissingen vastleggen als ADR's

## Status

Geaccepteerd

## Context

NL Portal had tot nu toe geen vastgelegde architectuurbeslissingen: keuzes zoals "één Gradle-module
per ZGW-bron" of "GraphQL als extern contract" staan impliciet in de code en in
`documentation/fundamentals/architectuur`, maar niet de afweging erachter of de alternatieven die
zijn afgewezen. Dat maakt het makkelijk om een eerder afgewogen keuze onbedoeld opnieuw ter discussie
te stellen, zeker in een open source project met een wisselende groep bijdragers.

## Beslissing

Elke structurele, moeilijk terug te draaien architectuurbeslissing wordt vastgelegd als een
genummerd Markdown-bestand in `docs/adr/`, met minimaal: status, context, beslissing, consequenties.
Nieuwe ADR's worden toegevoegd met oplopend nummer; een eerdere ADR wordt niet verwijderd of
herschreven, alleen van status voorzien (`Vervangen door ADR-000X`).

## Consequenties

- Een architectuurreview (zoals deliverable 1 van 2026-10-20) kan verwijzen naar een ADR in plaats
  van een eerder afgewezen voorstel te herhalen.
- Nieuwe bijdragers kunnen `docs/adr/` doorlezen om te begrijpen welke alternatieven al zijn
  overwogen, zonder de volledige git-geschiedenis te hoeven uitpluizen.
- Dit voegt een stap toe aan het maken van een grote structurele wijziging (een ADR schrijven), wat
  bewust een kleine frictie is om impulsieve herstructurering te ontmoedigen.
