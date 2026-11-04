# Feature-spec sjabloon

Kopieer dit in het issue dat je opent in de Issues van deze repository (zie
`CONTRIBUTING.md`, "Open an issue first") voor elke RFC/grotere feature. Zie
[`site/development/workflows/spec-driven-development.md`](./spec-driven-development.md) voor de volledige
werkwijze eromheen. Een kleine bugfix heeft dit sjabloon niet nodig.

```markdown
## Context / waarom

_Welk probleem van de klant of ketenpartner lost dit op? Verwijs naar `site/architectuur/arc42/01-inleiding-en-doelen.md`
als het een van de kwaliteitsdoelen raakt._

## Doel

_Wat moet er na deze feature waar zijn?_

## Niet-doelen

_Wat lossen we hier expliciet niet op? (voorkomt scope creep tijdens implementatie)_

## Geraakte bouwblokken

_Welke backend-module(s) / frontend-package(s) uit `site/architectuur/arc42/05-bouwblokkenzicht.md` en
`site/likec4` (C2/C3) worden aangepast? Nieuwe module nodig?_

## Nieuwe externe integratie?

_Komt er een nieuw extern systeem bij (nieuwe ZGW-bron, nieuwe Haal Centraal-bevraging, ...)? Zo ja:
vul [`site/development/workflows/threat-model-template.md`](threat-model-template.md) in en werk
`site/likec4/model.c4` bij (nieuwe `system`/relatie) voordat je begint met bouwen._

## Alternatieven overwogen

_Welke andere aanpak is overwogen, en waarom is die afgevallen? (Voorkomt dat een reviewer dezelfde
vraag stelt die je zelf al had uitgezocht.)_

## Architectuurimpact

_Raakt dit een bestaande ADR (`site/development/adr`)? Verdient de beslissing zelf een nieuwe ADR? (Zie
`site/development/adr/index.md`, "Wanneer een ADR schrijven".)_

## Dependencies

_Nieuwe afhankelijkheid nodig? Noteer hem hier zodat de licentiecheck
(`site/development/workflows/dependency-updates.md`, sectie 3) niet wordt vergeten._

## Test- en uitrolplan

_Welke scenario's uit `site/architectuur/arc42/10-kwaliteitseisen.md` worden hiermee getest? Nieuw scenario nodig?_
```

## Waarom dit sjabloon hier staat, en niet als GitHub issue-template

Issues voor dit project worden aangemaakt in de Issues van deze repository zelf. Dit sjabloon
staat (nog) als losstaand Markdown-document in `site/development/workflows/` in plaats van als
`.github/ISSUE_TEMPLATE/`; het daarheen verplaatsen is een logische vervolgstap, maar is hier niet
doorgevoerd.
