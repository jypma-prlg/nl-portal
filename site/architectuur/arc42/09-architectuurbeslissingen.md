# 9. Architectuurbeslissingen

Individuele beslissingen staan als Architecture Decision Records (ADR's) in
[`site/development/adr`](../../development/adr/index.md), niet in dit arc42-document — zo kan een beslissing worden toegevoegd zonder dit
hele hoofdstuk te herschrijven, en kan naar één beslissing specifiek gelinkt worden.

De eerste ADR, [`0001-record-architecture-decisions.md`](../../development/adr/0001-record-architecture-decisions.md),
legt vast *dat* en *hoe* beslissingen worden vastgelegd.

## Vastgelegde beslissingen

- [`0002-zgw-client-consolidation.md`](../../development/adr/0002-zgw-client-consolidation.md) — **in uitvoering**.
  Vervangt "kandidaat-ADR 1" hieronder: in plaats van één Gradle-module per ZGW-bron met een eigen
  HTTP-client, komt er één gedeelde, diepe `AuthenticatedZgwClient` (module `backend/zgw/zgw-client`)
  met een uniform, RFC 9457-gebaseerd foutafhandelingsbeleid. Zie de bijbehorende
  [spec](../../development/adr/0002-zgw-client-consolidation-spec.md) voor de concrete, nog te zetten
  implementatiestappen (nog niet uitgevoerd op het moment van schrijven).

- [`0004-changelog-release-proces.md`](../../development/adr/0004-changelog-release-proces.md) — **voorgesteld**.
  `CHANGELOG.md` (Keep a Changelog + SemVer) wordt automatisch afgeleid uit Conventional Commits via
  `mise run release:cut-rc`, bij het cutten van een `rc/*`-branch. Zie de bijbehorende
  [spec](../../development/adr/0004-changelog-release-proces-spec.md).

- [`0005-build-test-gates.md`](../../development/adr/0005-build-test-gates.md) — **geaccepteerd**. AGENTS.md,
  CONTRIBUTING.md en CI beschrijven elk een andere build/test-commando-set; dit legt vast dat het
  vier bewust verschillende tiers zijn (agent-fast-loop, lokale pre-push-gate, CI PR-gate, CI
  post-merge stage-gate) die voortaan vanuit één gedeeld document (`site/development/workflows/
  build-and-test-gates.md`, nog te schrijven) geciteerd worden in plaats van elk hun eigen kopie te
  onderhouden.

- [`0006-unified-docs-site.md`](../../development/adr/0006-unified-docs-site.md) — **geaccepteerd**. `docs/` en
  `documentation/` (de oude GitBook-export) worden één VitePress-site volgens het
  [Diataxis](https://diataxis.fr/)-framework, met secties Architectuur (arc42-leidend),
  Gebruik & inrichting, Hosting & configuratie, Development (incl. productmanagement-proces en het
  release-notes-archief) en een footer-only Compliance-sectie. Root-conventiebestanden
  (AGENTS.md, CONTRIBUTING.md, ...) verhuizen niet mee.

- [`0011-mutation-testing-backend-core.md`](../../development/adr/0011-mutation-testing-backend-core.md) — **geaccepteerd**.
  Mutation testing met pitest op `backend/core`: ratchet op de gemeten baseline voor het totaal,
  80% voor nieuwe en gewijzigde klassen, in CI op PR's (gewijzigde klassen) en nachtelijk.

- [`0012-breaking-changes-tot-eerste-productie-release.md`](../../development/adr/0012-breaking-changes-tot-eerste-productie-release.md) — **geaccepteerd**.
  Tot de eerste productie-release (1.0.0) mogen breaking changes zonder deprecation-periode; alle
  0.x-versies zijn experimenteel. De versie is teruggezet van 4.1.0 (upstream) naar 0.1.0.

- [`0013-stateless-backend-zonder-database.md`](../../development/adr/0013-stateless-backend-zonder-database.md) — **geaccepteerd**.
  De backend heeft geen eigen database meer (geen JPA, Liquibase of Postgres); module `data` is
  verwijderd. State blijft in de bronsystemen.

## Overige kandidaten

Nog niet als ADR vastgelegd:

1. **Keuze van test-dubbel-strategie voor de resterende ZGW-clients** (`openklant`, `taak`,
   `berichten`, `verificatie`, `besluiten`, `openproduct` — buiten de scope van ADR 0002):
   `MockWebServer` is het precedent dat ADR 0002 volgt voor de vier modules die het wél meeneemt;
   relevant zodra die overige modules aan de beurt zijn.
