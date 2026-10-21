# NL Portal — arc42-architectuurdocumentatie

Dit is de technische architectuurdocumentatie van NL Portal volgens de
[arc42](https://arc42.org/)-indeling. Ze vult de bestaande productdocumentatie aan en dupliceert
die niet:

- [`documentation/fundamentals/architectuur`](../../documentation/fundamentals/architectuur) beschrijft
  het **product** (scope, Common Ground-principes, authenticatie, integratiepatronen) voor een breed
  publiek. Niet gepubliceerd buiten deze repository; zie
  [issue #18](https://github.com/jypma-prlg/nl-portal/issues/18).
- `docs/arc42` (dit pad) beschrijft de **software-architectuur** voor ontwikkelaars en architecten:
  bouwblokken, runtime-gedrag, verdeling, dwarsdoorsnede-concepten, risico's en beslissingen.
- [`docs/likec4`](../likec4) bevat het navigeerbare C4-diagrammodel waarnaar dit document verwijst
  in plaats van losse afbeeldingen te dupliceren.
- [`docs/adr`](../adr) bevat de individuele architectuurbeslissingen (arc42-hoofdstuk 9 verwijst
  hiernaar).
- [`GLOSSARY.md`](../../GLOSSARY.md) is het canonieke domeinwoordenlijst waarnaar hoofdstuk 12
  verwijst.

## Hoofdstukken

| # | Hoofdstuk | Inhoud |
|---|---|---|
| 1 | [Inleiding en doelen](01-inleiding-en-doelen.md) | Wat NL Portal is, voor wie, en de belangrijkste kwaliteitsdoelen |
| 2 | [Randvoorwaarden](02-randvoorwaarden.md) | Technische, organisatorische en normatieve kaders |
| 3 | [Context en scope](03-context-en-scope.md) | Systeemgrens, actoren, buren (zie ook `docs/likec4` C1) |
| 4 | [Oplossingsstrategie](04-oplossingsstrategie.md) | De fundamentele architecturale keuzes en waarom |
| 5 | [Bouwblokkenzicht](05-bouwblokkenzicht.md) | Statische decompositie (zie ook `docs/likec4` C2/C3) |
| 6 | [Runtime-zicht](06-runtime-zicht.md) | Belangrijkste scenario's als sequence-achtige beschrijvingen |
| 7 | [Verdelingszicht](07-verdelingszicht.md) | Hoe het systeem wordt gebouwd, verpakt en uitgerold |
| 8 | [Dwarsdoorsnede-concepten](08-dwarsdoorsnede-concepten.md) | Concepten die meerdere bouwblokken overstijgen |
| 9 | [Architectuurbeslissingen](09-architectuurbeslissingen.md) | Verwijzing naar `docs/adr` |
| 10 | [Kwaliteitseisen](10-kwaliteitseisen.md) | De kwaliteitsboom en concrete scenario's |
| 11 | [Risico's en technische schulden](11-risicos-en-technische-schulden.md) | Bekende risico's, mede gevoed door de architectuurreview |
| 12 | [Glossarium](12-glossarium.md) | Verwijzing naar `GLOSSARY.md` |

## Status

Dit is een eerste versie, geschreven vanuit de bestaande code en documentatie (geen
stakeholderinterviews). Hoofdstukken 1 ("kwaliteitsdoelen") en 10 ("kwaliteitseisen") zijn het meest
afhankelijk van stakeholderinput en zijn hier als **voorstel** opgenomen — valideer ze met de
productverantwoordelijke(n) voordat je ze als vaststaand beschouwt. Houd dit document bij zodra een
architectuurbeslissing wijzigt; het hoort bij de pull request die de wijziging doorvoert, niet bij
een aparte "documentatie-inhaalslag".
