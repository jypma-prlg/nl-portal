# Workflow: spec-driven functionaliteit toevoegen

Dit document bindt de andere `docs/`-stukken (arc42, likec4, ADR's, GLOSSARY, de security- en
dependency-workflows) samen tot één werkwijze voor het toevoegen van nieuwe functionaliteit —
precies het moment waarvoor deze hele documentatieset is opgezet.

## Waarom spec-driven

`CONTRIBUTING.md` eist al "open een issue eerst" voor alles behalve kleine fixes, en laat een
maintainer meekijken vóór grotere features. Dat is de juiste intentie, maar zonder vaste vorm
beland je makkelijk bij een issue dat alleen "we willen X" zegt — te vaag om een maintainer iets
zinnigs te laten beoordelen, en te vaag om er later de architectuurdocumentatie uit te kunnen
bijwerken. Een spec is het verschil tussen "we hebben erover gepraat" en "we weten wat we bouwen
en waarom".

## De werkwijze

### 1. Schrijf de spec

Open een issue in [`nl-portal/nl-portal-issues`](https://github.com/nl-portal/nl-portal-issues) met
[`docs/workflows/feature-spec-template.md`](feature-spec-template.md). De secties "Geraakte
bouwblokken" en "Nieuwe externe integratie?" zijn niet optioneel — ze dwingen je om vóór het bouwen
even in `docs/arc42`/`docs/likec4` te kijken in plaats van pas bij de PR-review te ontdekken dat er
al een vergelijkbare module bestaat.

### 2. Nieuwe externe integratie? Doorloop eerst Learn

Als de spec een nieuw extern systeem introduceert (nieuwe ZGW-bron, nieuwe Haal Centraal-bevraging):
vul [`docs/workflows/threat-model-template.md`](threat-model-template.md) in **voordat** je gaat
bouwen, niet als afterthought bij de PR. Dit is de "Learn"-stap uit
[`docs/workflows/security.md`](security.md) toegepast op één concrete wijziging.

### 3. Maintainer-review op de spec, niet pas op de code

Laat de spec goedkeuren vóórdat je begint (`CONTRIBUTING.md`). Dit is de goedkoopste plek om een
verkeerde aanpak bij te sturen — vóór de implementatie, niet in PR-review.

### 4. Bouw, met de architectuurdocumentatie als onderdeel van dezelfde verandering

Niet als losse "documentatie-PR" achteraf, maar in dezelfde PR als de code:

| Wijziging | Update |
|---|---|
| Nieuwe/aangepaste module | `docs/likec4` (`mise run likec4:validate`), `docs/arc42/05-bouwblokkenzicht.md` |
| Nieuwe architectuurbeslissing | ADR in `docs/adr` (zie `docs/adr/README.md`, "Wanneer een ADR schrijven") |
| Nieuwe domeinterm | `GLOSSARY.md` |
| Nieuwe dependency | licentiecheck (`docs/workflows/dependency-updates.md`, sectie 3); bij twijfel `mise run sbom:all` |
| Nieuwe externe integratie | het ingevulde threat-model-werkblad als bijlage bij de PR |

Een PR die een module "verdiept" (in de zin van de `codebase-design`-architectuurvocabulaire: een
kleinere interface, meer gedrag erachter) in plaats van er een shallow kopie naast te zetten, is
precies wat de architectuurreview van fase 1 als "Strong"-kandidaten aanmerkte — gebruik die
kandidaten als voorbeeld van de interface-vorm die je nastreeft, niet alleen als los te plukken
refactors.

### 5. PR

Het bestaande `.github/pull_request_template.md`, Conventional Commit-titel, `Closes #<issue>`. Zie
`CONTRIBUTING.md` voor de volledige flow (branchmodel, squash-merge, copyright-headers).

## Wat dit niet is

Geen waterval. De spec is een startpunt, geen contract dat elke latere aanpassing verbiedt — als
de implementatie een andere aanpak vraagt dan de spec, werk de spec (het issue) bij en zeg waarom,
in plaats van er stilzwijgend van af te wijken. Dat "waarom" is precies wat een ADR vastlegt als de
afwijking architecturaal is.
