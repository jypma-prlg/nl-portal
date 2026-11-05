# 11. Mutation testing (pitest) op backend `core`

## Status

Geaccepteerd.

## Context

Hoge line coverage zegt niet of tests gedrag verifiëren (#93 control 4, opvolging #95). `core` heeft
21 productiebestanden en 5 testklassen. Doel is een bruikbare quality gate die ook agent-geschreven
code stuurt naar tests die echt iets aantonen, zonder PR's te vertragen.

## Beslissing

- **Tooling:** `info.solidsoft.pitest` 1.19.0 met pitest 1.30.1 en `pitest-junit5-plugin`, alleen in
  module `core`. Taak `./gradlew :core:pitest` (of `mise run backend:pitest`), bewust **niet** gekoppeld
  aan `build`/`check`. Rapport: `backend/core/build/reports/pitest`.
- **Geen `pitest-kotlin`:** de Kotlin-filterplugin van arcmutate vereist een commerciële licentie
  (`No licence found`). In plaats daarvan sluiten we Kotlin-intrinsics en logging uit via
  `avoidCallsTo`. Zie je veel equivalente mutanten, heroverweeg dan de licentie.
- **Twee drempels:**
  - *Totaal `core`: ratchet.* Vloer = gemeten baseline (21%, 15/70 mutanten gedood op 2026-11-05),
    nu `mutationThreshold` = 20. Alleen verhogen, nooit verlagen. Streefwaarde is 80%, maar die is
    geen gate voor bestaande code.
  - *Nieuwe en gewijzigde klassen: 80%.* In CI op PR's wordt de run beperkt tot de gewijzigde
    `core`-klassen (`-PpitestTargetClasses`) met `-PpitestThreshold=80`.
- **CI** (`.github/workflows/mutation-testing.yml`): op PR's die `backend/core` raken (gewijzigde
  klassen, 80%), nachtelijk en handmatig (volledige run, ratchet). Een volledige run duurt ~30 s
  pitest (~80 s inclusief Gradle), ruim binnen de afgesproken grens van 3 minuten extra per PR. Gaat
  de PR-run structureel boven die grens uitkomen, schuif hem dan naar alleen nachtelijk en lokaal.
- **Dependency-verification:** `backend/gradle/verification-metadata.xml` is aangevuld met de plugin
  en transitieve pitest-artefacten. Het eerdere "hangen" van `--write-verification-metadata` is hier niet
  reproduceerbaar (78 s); de oorzaak was waarschijnlijk `mise exec` dat andere tools wilde installeren.

## Consequences

- Wie een bestaande, slecht geteste `core`-klasse aanraakt, moet die klasse op 80% brengen of de PR
  blijft rood. Dat is bedoeld: het dwingt tests af waar code verandert.
- Agents kunnen lokaal `mise run backend:pitest` draaien en overlevende mutanten per bestand en
  regel uit `mutations.xml` lezen.
- De drempel van het totaal moet bij elke verbetering handmatig omhoog; dat doet niemand
  automatisch. Een vervolg kan dat automatiseren.
