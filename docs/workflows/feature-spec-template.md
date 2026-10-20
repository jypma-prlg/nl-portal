# Feature-spec sjabloon

Kopieer dit in het issue dat je opent in
[`nl-portal/nl-portal-issues`](https://github.com/nl-portal/nl-portal-issues/issues) (zie
`CONTRIBUTING.md`, "Open an issue first") voor elke RFC/grotere feature. Zie
[`docs/workflows/spec-driven-development.md`](spec-driven-development.md) voor de volledige
werkwijze eromheen. Een kleine bugfix heeft dit sjabloon niet nodig.

```markdown
## Context / waarom

_Welk probleem van de klant of ketenpartner lost dit op? Verwijs naar `docs/arc42/01-inleiding-en-doelen.md`
als het een van de kwaliteitsdoelen raakt._

## Doel

_Wat moet er na deze feature waar zijn?_

## Niet-doelen

_Wat lossen we hier expliciet niet op? (voorkomt scope creep tijdens implementatie)_

## Geraakte bouwblokken

_Welke backend-module(s) / frontend-package(s) uit `docs/arc42/05-bouwblokkenzicht.md` en
`docs/likec4` (C2/C3) worden aangepast? Nieuwe module nodig?_

## Nieuwe externe integratie?

_Komt er een nieuw extern systeem bij (nieuwe ZGW-bron, nieuwe Haal Centraal-bevraging, ...)? Zo ja:
vul [`docs/workflows/threat-model-template.md`](threat-model-template.md) in en werk
`docs/likec4/model.c4` bij (nieuwe `system`/relatie) voordat je begint met bouwen._

## Alternatieven overwogen

_Welke andere aanpak is overwogen, en waarom is die afgevallen? (Voorkomt dat een reviewer dezelfde
vraag stelt die je zelf al had uitgezocht.)_

## Architectuurimpact

_Raakt dit een bestaande ADR (`docs/adr`)? Verdient de beslissing zelf een nieuwe ADR? (Zie
`docs/adr/README.md`, "Wanneer een ADR schrijven".)_

## Dependencies

_Nieuwe afhankelijkheid nodig? Noteer hem hier zodat de licentiecheck
(`docs/workflows/dependency-updates.md`, sectie 3) niet wordt vergeten._

## Test- en uitrolplan

_Welke scenario's uit `docs/arc42/10-kwaliteitseisen.md` worden hiermee getest? Nieuw scenario nodig?_
```

## Waarom dit sjabloon hier staat, en niet als GitHub issue-template

Issues voor dit project worden aangemaakt in het aparte repository
[`nl-portal/nl-portal-issues`](https://github.com/nl-portal/nl-portal-issues), niet in dit
monorepo — een `.github/ISSUE_TEMPLATE/` hier zou dus nooit gebruikt worden. Zodra iemand toegang
heeft om dat andere repository aan te passen, is het daar toevoegen van dit sjabloon als
issue-template een logische vervolgstap.
