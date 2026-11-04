# 8. Security-transparantie naar klanten en image-registry-strategie

## Status

Geaccepteerd. Beslissing 1 (publicatie-trigger) deels vervangen door [ADR 0009](0009-strix-scan-handmatig-losgekoppelde-publicatie.md):
de CI-stap draait niet meer op elke push naar `main`, maar na elke handmatige scan.

## Context

Twee losse vragen kwamen tegelijk op: (1) Strix-scanresultaten worden nu wel in CI uitgevoerd maar
nergens ingecheckt of naar klanten gepresenteerd, terwijl we — als community-driven, EUPL-gelicenseerd
project — transparantie over onze threat model en security-workflow willen bieden; (2) image-publicatie
naar `ghcr.io/nl-portal/*` faalt structureel omdat pinkroccade die namespace niet bezit, wat opkwam
naast de vraag of backend/app-dev en de deploy-pipeline naar een aparte repo moeten voor een "lean"
nl-portal.

Overwogen alternatieven voor (1): een aparte repo of eigen github.io-pagina voor security-content;
automatisch publiceren van de volledige ruwe Strix-rapporten; of periodieke handmatige curatie van
bevindingen. Voor (2): backend/app-dev-broncode of de volledige build/deploy-pipeline naar een aparte
GitOps-repo verplaatsen, naar analogie van het ArgoCD "app-of-apps"-patroon.

## Beslissing

1. **Security-transparantie blijft automatisch en geaggregeerd, geen aparte publicatieplek.** Een
   CI-stap na scans op `main` schrijft alleen severity-aantallen (geen titels, CVE's of locaties) naar
   de bestaande VitePress-sectie `site/compliance/` (zie [ADR 0006](0006-unified-docs-site.md)).
   Methodologie/workflow-documentatie (hoe en waarmee gescand wordt) staat daar volledig open naast.
   Geen rauwe Strix-rapporten, geen handmatige curatie, geen aparte repo of github.io-pagina.
2. **`strix-scan.yml` blijft in `nl-portal`.** Verplaatsen zou alleen cross-repo
   checkout/permissies-overhead toevoegen zonder de repo "schoner" te maken. `strix_runs/` blijft
   ongewijzigd gitignored. (De oorspronkelijke motivatie hier noemde een koppeling aan PR-diffs
   via `--scope-mode diff`; die trigger en scope-mode zijn inmiddels losgelaten, zie
   [ADR 0009](0009-strix-scan-handmatig-losgekoppelde-publicatie.md) — de conclusie zelf staat.)
3. **Geen GitOps/pipeline-repo-split voor backend/app-dev.** ArgoCD-manifesten worden al beheerd in de
   interne self-hosted GitLab, en Harbor-caching loopt al via een aparte interne pipeline — beide buiten
   de scope van deze repo. `nl-portal` hoeft dus niet naar Harbor te pushen.
4. **GHCR-namespace fixen, geen registry-wissel.** Images blijven naar GHCR gaan (voor publieke
   distributie aan de EUPL-community), maar onder een namespace die pinkroccade daadwerkelijk bezit,
   in plaats van het huidige `ghcr.io/nl-portal/*` dat structureel faalt.

## Consequenties

- De enige code-wijziging die uit dit ADR volgt: (a) een CI-stap die geaggregeerde Strix-severity-counts
  naar `site/compliance/` schrijft, en (b) de GHCR-publish-workflows (`_build-app-image.yml`,
  `build-and-publish-*.yml`) laten wijzen naar de nieuwe, wél in bezit zijnde namespace.
- Geen repo-splitsing, geen nieuwe GitOps-repo, geen Harbor-integratie vanuit `nl-portal` nodig — mocht
  een toekomstig team dit opnieuw overwegen, dan is dat een bewuste koerswijziging, geen voortzetting
  van onafgemaakt werk.
