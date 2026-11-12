# 19. Trunk-based continuous delivery: één lijn, één build, promotie op digest

## Status

Voorgesteld. Vervangt het branchmodel (`rc/*`, `release/<major>.x`) uit `CONTRIBUTING.md` en de
rc-stap uit [ADR 0004](./0004-changelog-release-proces.md); de changelog-afleiding uit
Conventional Commits van ADR 0004 blijft.

## Context

Het huidige model is Spring-achtig: `main` → `rc/*` (handmatige dispatch, `release:cut-rc`) →
tag op `release/<major>.x`. De RC wordt gebouwd en getest, waarna de release opnieuw bouwt: wat in
productie komt is niet de geteste digest. Een release is een bewuste, zware stap met een aparte
branch, terwijl het doel het omgekeerde is: op elk moment van de dag een update kunnen pushen, ook
een bugfix of security-patch, met alles automatisch behalve de goedkeuring voor productie.
Functionele wijzigingen zitten achter feature flags
([ADR 0014](./0014-feature-flags-via-openfeature.md)), dus onafgemaakt werk hoeft niet uit `main` te
blijven. NL Portal deployt zelf niets naar productie: afnemers rollen uit, in het geval van Pink via
ArgoCD. De pijplijn publiceert dus een artefact dat altijd uitrolbaar moet zijn.

## Beslissing

1. **`main` is de enige lijn.** `rc/*`, `release/<major>.x`, `hotfix/*` en `release:cut-rc` vervallen.
   Een onderhoudslijn ontstaat pas wanneer er een tweede major onderhouden moet worden.
2. **Eén build per commit op `main`.** Elke merge bouwt backend en frontend één keer, runt de
   poorten (tests, smoke-test op de compose-stack, SBOM) en pusht een onveranderlijke tag
   (`<versie>-<sha>`). De referentieomgeving (dev, onder ArgoCD) rolt de nieuwste groene digest
   automatisch uit.
3. **Promotie is retaggen, niet herbouwen.** Een handmatige `workflow_dispatch` met als invoer een
   commit of digest (standaard de nieuwste groene `main`-build), achter de `release`-environment,
   zet `x.y.z`, `x.y`, `x` en `latest` op diezelfde digest, signeert en attesteert die, en maakt de
   GitHub Release. Een oudere groene digest promoveren kan ook.
4. **`latest` betekent: nieuwste goedgekeurde release**, niet nieuwste build. Consumenten (ArgoCD)
   verwijzen naar `x.y.z` of een digest, nooit naar `latest`.
5. **De tag is de bron van de versie.** De volgende versie volgt uit de Conventional Commits sinds
   de vorige tag; `version.txt` vervalt. De changelog-sectie gaat in de GitHub Release en via een
   automatisch mergende bot-PR naar `CHANGELOG.md` op `main`.
6. **Hotfix en security-patch gebruiken hetzelfde pad:** merge naar `main`, dezelfde poorten,
   patchrelease via promotie.
7. **Rollback is geen pijplijnstap:** ArgoCD terugzetten naar de vorige onveranderlijke tag.
   `latest` schuift nooit terug.
8. **De metrics-poort volgt later:** zodra de metrics-tickets leveren, beoordeelt de pijplijn de
   dev-omgeving N minuten op foutpercentage, latency en readiness voordat promotie beschikbaar is.

## Consequenties

- **Eén-weg-deur voor gepubliceerde tags en labels:** `x.y.z` en `latest` kunnen niet terug.
  De pijplijn zelf is te herstellen.
- **Elke merge moet releasable zijn.** Dat leunt op feature flags en op de smoke-test; zonder die
  poort is automatisch promoveren niet verantwoord. Er is nog geen test tegen een draaiende stack
  in CI en geen metrics-endpoint (alleen actuator `health`).
- **Flagwijzigingen gaan via een rolling update** (ADR 0014), dus een flag uitzetten is geen snelle
  noodrem.
- **De tag staat op de geteste commit; de changelog-commit volgt daarna.** De tag bevat zijn eigen
  changelog-sectie dus niet.
- **Nog te bouwen:** smoke-test, build-once- en promotie-workflow (vervangt snapshot, rc en
  release), versie uit tags en changelog-bot, opruimen van branchmodel en docs, metrics-poort.
- **Afgewezen:** het huidige rc-model behouden (dubbele build, zware release) en `latest` laten
  meebewegen met elke `main`-build (geen audit, geen rollback).
