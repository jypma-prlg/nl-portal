# Spec: Changelog en release-proces

Volgt [`docs/workflows/feature-spec-template.md`](../workflows/feature-spec-template.md). Hoort bij
[ADR 0004](0004-changelog-release-proces.md). Geschreven om later opgepakt te worden — nog niet
geïmplementeerd. Kopieer de relevante secties in een issue in
[`nl-portal/nl-portal-issues`](https://github.com/nl-portal/nl-portal-issues) zodra je met de eerste
stap begint (zie `CONTRIBUTING.md`, "Open an issue first").

## Context / waarom

Zie [ADR 0004](0004-changelog-release-proces.md) — er is geen `CHANGELOG.md`, en de GitHub Release
die `build-and-publish-release.yml` aanmaakt heeft alleen een statische tekst als notes. Omdat
commit-berichten al verplichte Conventional Commits zijn, kan het hele proces (versie bepalen,
changelog-sectie samenstellen) machinaal uit de git-historie worden afgeleid in plaats van
handmatig te worden bijgehouden.

## Doel

Na deze PR('s):

- Een nieuwe `mise`-taak (`release:cut-rc`) berekent de volgende SemVer-versie uit de Conventional
  Commits sinds de vorige tag die bereikbaar is vanaf de huidige regel (`main`, of een
  `release/<major>.x`-lijn voor een hotfix — zie Beslissing punt 6), maakt de `rc/<version>`-branch
  aan, werkt `version.txt` en `CHANGELOG.md` bij, en committed dat lokaal — in één commando, zonder
  te pushen.
- `CHANGELOG.md` zelf bestaat **nog niet** na deze PR('s) — het bestand wordt, net als elke latere
  sectie, pas aangemaakt de eerste keer dat `release:cut-rc` daadwerkelijk voor een echte release
  draait (zie ADR 0004, "Nog te bouwen"). Dit is bewust: hand-seeden zou een bestand opleveren dat
  niet daadwerkelijk door het gedocumenteerde proces is gegenereerd.
- `build-and-publish-release.yml` leest bij het aanmaken van de GitHub Release de `CHANGELOG.md`-
  sectie van de zojuist getagde versie en gebruikt die als release notes, in plaats van de huidige
  statische tekst.
- **Toegevoegd t.o.v. de oorspronkelijke scope** (niet in de Doel-lijst hierboven voorzien, maar
  tijdens implementatie logisch gebleken): `ci.yml` krijgt een `scripts`-padfilter en een
  `test-scripts`-job die `node --test "scripts/**/*.test.mjs"` draait zodra `scripts/**` wijzigt.
  Zonder deze job dekt niets in CI de nieuwe, release-kritische logica — dezelfde reden waarom
  `backend/**` en `frontend/**` al eigen CI-jobs hebben.

## Niet-doelen

- Geen verplichte review-/goedkeuringsstap voor de gegenereerde changelog-tekst of het
  versievoorstel (zie ADR 0004, Beslissing punt 4) — dit is bewust *niet* gekozen.
- Geen wijziging van het branchmodel zelf (`main`/`rc/*`/`release/<major>.x`) — alleen een nieuwe
  taak die binnen dat bestaande model past.
- Geen automatisch `git push` vanuit de nieuwe `mise`-taak — dat blijft een aparte, bewuste
  handeling van de releaser (ADR 0004, Beslissing punt 5).
- Geen per-module/per-package changelog — blijft één bestand voor de hele, gedeeld-versiede
  monorepo.

## Geraakte bouwblokken

Nieuw: `CHANGELOG.md` (repo-root). Nieuwe `mise`-taak `release:cut-rc` in `mise.toml`, met een
bijbehorend script (bv. `scripts/release-cut-rc.*`, naamgeving/taal vrij — Node/bash/Kotlin-script,
wat het beste past bij hoe de overige `mise`-taken in dit project zijn geïmplementeerd).
Gewijzigd: `.github/workflows/build-and-publish-release.yml` (stap die de GitHub Release aanmaakt).

## Nieuwe externe integratie?

Nee — geen nieuw extern systeem. Het
[threat-model-werkblad](../workflows/threat-model-template.md) is hier niet van toepassing.

## Alternatieven overwogen

- **Extern tool (git-cliff, semantic-release, release-please)** — afgewezen: voegt een nieuwe
  dependency en configuratietaal toe voor iets dat met een klein script binnen de bestaande
  `mise`-conventie net zo goed kan, en geeft minder controle over de projectspecifieke
  branch-/hotfix-afhandeling (ADR 0004, Beslissing punt 6).
- **Changelog incrementeel bijhouden via een "Unreleased"-sectie per PR** — afgewezen: vereist
  discipline bij elke PR-auteur en een CI-check die daarop afdwingt; "alles in één keer afleiden op
  releasemoment" heeft dezelfde uitkomst zonder die doorlopende last.
- **Verplichte menselijke review van de gegenereerde changelog/versie vóór commit** — afgewezen (zie
  ADR 0004, Consequenties): kost structureel meer tijd per release dan het af en toe achteraf
  herstellen van een slecht geformuleerde regel.
- **Automatisch pushen + direct de release-workflow dispatchen vanuit de `mise`-taak** — afgewezen:
  een taak die zelfstandig naar een gedeelde remote pusht is een onverwachte bijwerking; de
  releaser beslist zelf het moment van pushen.

## Architectuurimpact

Vastgelegd in [ADR 0004](0004-changelog-release-proces.md) (status: voorgesteld). Geen conflict met
een bestaande ADR — dit raakt het release-proces, niet de broncode-architectuur van ADR 0002/0003.

## Dependencies

Geen nieuwe externe dependency verwacht als het script op bestaande tooling (git, het shell/script-
type dat past bij de rest van het project) leunt. Mocht tijdens implementatie toch een library nodig
blijken (bv. een semver-parser), doorloop dan de licentiecheck uit
`docs/workflows/dependency-updates.md`, sectie 3, vóór merge.

## Architectuurdocumentatie bijwerken (per stap, niet vooraf)

- [x] `CONTRIBUTING.md`: §Branch model uitgebreid met hoe `release:cut-rc` zich verhoudt tot het
  bestaande `rc/*`-aanmaakproces; §How to contribute verwijst nu naar
  `docs/workflows/spec-driven-development.md` voor grotere features.
- [x] `docs/arc42/09-architectuurbeslissingen.md`: verwijzing naar ADR 0004 toegevoegd (status:
  voorgesteld — bijwerken naar "geaccepteerd" zodra de eerste echte release via `release:cut-rc` is
  gecut).
- [ ] ADR 0004's status bijwerken naar "Geaccepteerd" zodra `release:cut-rc` daadwerkelijk een
  eerste release heeft gecut (het eerste echte, machinaal gegenereerde `CHANGELOG.md`-bestand
  bestaat dan ook pas).
- [ ] `GLOSSARY.md`: geen wijziging verwacht — "changelog", "release candidate" zijn
  release-procesvocabulaire, geen domeinbegrip (burger/zaak/verzoek-vocabulaire blijft ongemoeid).
- [ ] `docs/likec4`: geen wijziging verwacht — geen nieuw bouwblok in de runtime-architectuur, alleen
  een ontwikkel-/release-tijd-taak.

## Test- en uitrolplan

1. **Versie- en type-afleiding**: unit-test(s) voor de functie die uit een lijst commit-berichten
   (feat/fix/refactor/perf/breaking/overig) de juiste SemVer-bump en de juiste Keep a
   Changelog-categorie afleidt — inclusief randgevallen: geen relevante commits sinds de vorige tag
   (geen bump), meerdere breaking changes (nog steeds één major-bump), een commit die zowel `fix`
   als een `BREAKING CHANGE:`-footer heeft (major wint).
2. **`mise run release:cut-rc`** end-to-end op een scratch-checkout: branch wordt aangemaakt,
   `version.txt` en `CHANGELOG.md` kloppen, de commit is een geldig Conventional Commit (geverifieerd
   met de eigen `parseConventionalCommit`-parser op de gegenereerde `chore(release): x.y.z`-subject —
   hetzelfde contract als `frontend/commitlint.config.js`'s `@commitlint/config-conventional`, zonder
   vanuit een root-script een afhankelijkheid naar `frontend/node_modules` te hoeven leggen), en er
   wordt niet gepusht.
3. **`build-and-publish-release.yml`**: test op een testtag/testrepo dat de GitHub Release notes de
   juiste `CHANGELOG.md`-sectie bevatten voor de getagde versie.
4. **Hotfix-pad**: geautomatiseerd gedekt voor het `release:cut-rc`-deel — een scratch-scenario
   waarin `main` een tag heeft die de `release/<major>.x`-lijn nooit ziet, bewijst dat de volgende
   versie en changelog-sectie van die lijn's eigen laatste tag worden afgeleid, niet van `main`'s.
   Het terugpropageren van de resulterende changelog-entry naar `main` zelf (ADR 0004, Beslissing
   punt 6) blijft handmatig doorlopen zolang er geen geautomatiseerde propagatie-tooling is.

Na implementatie: volledige lokale build (`./gradlew build` / `pnpm -r build`) blijft ongewijzigd
groen — deze wijziging raakt alleen release-tooling, niet de applicatiecode.
