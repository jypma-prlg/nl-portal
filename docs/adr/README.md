# Architecture Decision Records

Elke map hier is één architectuurbeslissing (ADR), genummerd oplopend, nooit hernummerd of
verwijderd — een verworpen beslissing krijgt status `Afgewezen`/`Vervangen`, niet een verwijderd
bestand. Gebruik `0001-record-architecture-decisions.md` als sjabloon voor een nieuwe ADR.

## Wanneer een ADR schrijven

- Een structurele keuze die **moeilijk terug te draaien** is (bijv. "één Gradle-module per
  ZGW-bron", "GraphQL als extern contract van de backend").
- Een keuze die **tegen een eerder voorstel ingaat** uit een architectuurreview — zie de
  `improve-codebase-architecture`-skill: als een gebruiker een verdiepingskandidaat afwijst om een
  onderbouwde reden, hoort die reden hier, zodat een latere review niet hetzelfde kandidaat opnieuw
  voorstelt.
- Een keuze waar een nieuwe bijdrager **anders aan zou twijfelen** bij het lezen van de code.

Geen ADR nodig voor: een bugfix, een naamgevingskeuze binnen één module, iets dat al in
[`docs/arc42`](../arc42) hoofdstuk 4 (oplossingsstrategie) staat beschreven zonder dat er een
alternatief serieus is overwogen.

## Status

Nog geen beslissingen vastgelegd op het moment van schrijven (2026-10-20) — dit is de eerste keer
dat dit project een ADR-log heeft. Zie [`docs/arc42/09-architectuurbeslissingen.md`](../arc42/09-architectuurbeslissingen.md).
