# 4. Changelog en release-proces: geautomatiseerd uit Conventional Commits

## Status

Voorgesteld. Zie de bijbehorende spec,
[`0004-changelog-release-proces-spec.md`](0004-changelog-release-proces-spec.md).

## Context

NL Portal heeft nog geen `CHANGELOG.md`: de GitHub Release die `build-and-publish-release.yml`
aanmaakt, krijgt vandaag alleen de statische tekst "Release notes are published in the NL Portal
documentation" als notes — die documentatie bestaat niet. Backend en frontend releasen op één
gedeelde versie (`version.txt`, nu `4.1.0`), volgens het Spring-achtige branchmodel uit
`CONTRIBUTING.md` (`main` → `rc/*` → tag op `release/<major>.x`). Er zijn nog geen git-tags.

Wat deze beslissing bruikbaar maakt zonder extra handwerk: commit-berichten zijn al verplichte
Conventional Commits (commitlint-afgedwongen; de PR-titel wordt de squash-merge-commit, zie
`CONTRIBUTING.md` §Commit messages), dus "wat is er veranderd sinds de vorige release" is al
machine-leesbaar. Het project heeft bovendien al een `mise`-taken-conventie voor repeatable
processen (`deps:*`, `sbom:*`, `security:*`, `sonar:*` in `mise.toml`) — de natuurlijke plek voor
een nieuwe taak, in plaats van een los script of een extern tool zoals
git-cliff/semantic-release/release-please.

## Beslissing

1. **Eén `CHANGELOG.md` op de repo-root**, [Keep a Changelog](https://keepachangelog.com/)-formaat
   + SemVer, in het Engels (zelfde taal als de commit-berichten). Eén bestand voor backend én
   frontend samen, conform de gedeelde versie.
2. **Geen permanente "Unreleased"-sectie.** Het bestand verandert alleen op het moment dat een
   release wordt gecut; tussen releases in blijft het ongewijzigd.
3. **Alleen gebruikersrelevante commit-types komen in de changelog**: `feat` → *Added*, `fix` →
   *Fixed*, `refactor`/`perf` → *Changed*; een breaking change (`!`-suffix of
   `BREAKING CHANGE:`-footer) krijgt een eigen, duidelijk gemarkeerde entry. `docs`/`test`/`build`/
   `ci`/`chore` worden expliciet weggelaten — ruis voor de lezer van de changelog, ook al blijven ze
   gewoon in de git-historie staan.
4. **De volgende versie wordt automatisch voorgesteld** uit de Conventional Commits sinds de vorige
   tag (`feat` → minor, `fix` → patch, breaking change → major), met `version.txt` als enige plek om
   dat voorstel te overrulen. Er is geen verplichte review-stap: het voorstel wordt zonder
   goedkeuringsgate toegepast; handmatig overrulen kan, maar is niet vereist.
5. **Eén nieuwe, gecombineerde `mise`-taak** (werktitel `release:cut-rc`) voert de hele voorbereiding
   in één commando uit: laatste `main` ophalen → volgende versie berekenen → `rc/<version>`-branch
   aanmaken → `version.txt` bijwerken → de nieuwe sectie van `CHANGELOG.md` genereren en invoegen →
   lokaal committen met een zelf-Conventional-Commit-conform bericht (bv. `chore(release): 4.2.0`).
   **Pushen blijft een aparte, bewuste handeling** van de releaser — de taak pusht zelf niet, om een
   onverwachte bijwerking op een gedeelde remote te voorkomen.
6. **Hotfixes op `release/<major>.x` volgen hetzelfde proces** en krijgen zo ook een changelog-entry
   voor hun release; bij het terugpropageren van de fix naar `main` (bestaande regel,
   `CONTRIBUTING.md` §Fix propagation) gaat de bijbehorende changelog-entry mee, zodat `main`'s
   `CHANGELOG.md` compleet blijft, ook voor hotfix-only releases die nooit via `rc/*` zijn gegaan.
7. **`build-and-publish-release.yml` wordt aangepast** om de GitHub Release notes te vullen met de
   `CHANGELOG.md`-sectie van de zojuist getagde versie, in plaats van de huidige statische tekst —
   zodat er niet twee plekken zijn die uit elkaar kunnen lopen.

## Consequenties

- **Eén bron van waarheid voor "wat is er veranderd"**: `CHANGELOG.md` zelf, hergebruikt voor zowel
  de repo als de GitHub Release notes.
- **Geen per-PR discipline nodig**: niemand hoeft tijdens het werk aan een feature na te denken over
  een changelog-regel; die wordt pas gegenereerd op het moment dat iemand daadwerkelijk een release
  cut.
- **Risico, bewust geaccepteerd**: omdat er geen verplichte review-stap is (Beslissing punt 4), kan
  een slecht geformuleerd commit-bericht (bv. een vage `fix: dinges`) ongefilterd in de changelog
  belanden. Een verplichte review-gate kost structureel meer tijd per release dan het af en toe
  achteraf herschrijven van één regel.
- **Afhankelijk van bestaande commit-discipline**: deze aanpak staat of valt met de al
  commitlint-afgedwongen Conventional Commits-conventie; zonder die afdwinging zou de automatische
  versie- en changelog-afleiding onbetrouwbaar worden.
- **Nog te bouwen** (zie spec): de `mise`-taak zelf, de aanpassing van
  `build-and-publish-release.yml`, en het eerste, lege `CHANGELOG.md`. Deze ADR legt het ontwerp
  vast, niet de implementatie.
