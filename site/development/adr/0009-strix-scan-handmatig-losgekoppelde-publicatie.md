# 9. Strix-scan handmatig, publicatie losgekoppeld van push

## Status

Geaccepteerd.

## Context

`strix-scan.yml` draaide tot nu toe op elke pull request (`--scope-mode diff`, scope beperkt tot de
PR-diff) en op elke push naar `main`; die laatste trigger voedde ook meteen de
`publish-security-status`-job uit [ADR 0008](./0008-security-transparantie-en-registry-strategie.md),
in dezelfde workflow-run via `needs:` en `if: github.event_name == 'push'`.

Een volledige agentic security-scan op elke PR en elke push is kostbaar (LLM-tijd, sandbox-pulls) en
remt de ontwikkelsnelheid, terwijl bevindingen niet per se bij elke wijziging opnieuw hoeven te
worden opgehaald. Voor nu is besloten de scan alleen nog handmatig te starten; automatisch draaien
(bijv. op een schema, of weer op PR's) is een mogelijke latere stap, geen onderdeel van deze
beslissing.

Met de scan alleen nog handmatig (`workflow_dispatch`) heeft `publish-security-status` geen
`push`-event meer om aan te haken, en moet het uit hetzelfde workflow-bestand getrokken worden: één
`on:`-blok kan niet voor twee onafhankelijke triggers dienen zonder de andere job onbedoeld mee te
laten lopen.

## Beslissing

1. **`strix-scan` draait alleen nog handmatig.** `pull_request`- en `push`-triggers zijn verwijderd;
   alleen `workflow_dispatch` blijft, met een `scan_mode`-input (`quick`/`deep`, default `quick`) om
   de scandiepte per run te kiezen.
2. **Scope is full-repo, niet meer PR-diff.** `--scope-mode diff` had alleen zin tegen een PR-diff;
   bij een handmatige trigger is er geen diff om tegen te scannen, dus de scan dekt voortaan de hele
   checkout.
3. **`publish-security-status` verhuist naar een eigen workflow-bestand**
   (`publish-security-status.yml`), getriggerd door `workflow_run` op het voltooien van
   `strix-scan` (`completed`, zowel bij `success` als `failure` — een `failure`-conclusie betekent
   hier juist dat er high-severity bevindingen zijn, de reden waarom de severity-counts gepubliceerd
   worden). De job haalt het `strix-scan-report`-artifact niet uit zijn eigen run, maar uit de
   triggerende run via <code v-pre>run-id: ${{ github.event.workflow_run.id }}</code>.
4. **Publicatie blijft verder ongewijzigd ten opzichte van ADR 0008**: alleen geaggregeerde
   severity-counts, geen rauwe rapporten, automatisch via een PR naar `site/compliance/`.

## Consequenties

- Security-scans lopen niet meer automatisch mee op elke PR of push; een scan moet bewust gestart
  worden via Actions → "Strix security scan" → Run workflow.
- `site/compliance/security-status.md` wordt bijgewerkt na elke handmatige scan in plaats van na
  elke push naar `main`; buiten een handmatige trigger om kan de status dus verouderen. Toekomstige
  automatisering (schema of PR-trigger) is een bewuste vervolgstap, geen onderdeel van deze ADR.
- Twee workflow-bestanden in plaats van één, met een cross-workflow artifact-afhankelijkheid
  (`workflow_run` + `run-id`) in plaats van een `needs:`-afhankelijkheid binnen dezelfde run.
