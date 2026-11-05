# 10. Kwaliteitseisen

Uitwerking van de kwaliteitsdoelen uit hoofdstuk 1.2 als concrete, (idealiter) toetsbare scenario's.
**Dit zijn voorstellen, geen vastgestelde eisen** — net als hoofdstuk 1.2 zijn ze afgeleid uit de
bestaande code/CI/documentatie, niet uit een stakeholderinterview.

## 10.1 Kwaliteitsboom (voorstel)

| Kwaliteit | Scenario | Huidige borging |
|---|---|---|
| Vertrouwd/beveiligd | Een afhankelijkheid met een bekende CVE wordt binnen één sprint gesignaleerd | OWASP Dependency-Check in CI (`dependency-check.yml`) — reactief, geen SLA vastgelegd |
| Vertrouwd/beveiligd | Een document met malware wordt geweigerd vóór opslag in Open Zaak | ClamAV-integratie (zie hoofdstuk 6.3) — werking niet apart getest in deze review |
| Interoperabiliteit | Een nieuwe ZGW-bron kan worden toegevoegd zonder bestaande modules te wijzigen | Gedeeltelijk: modulaire opzet bestaat, maar zonder gedeelde adapter (zie hoofdstuk 5.2/8.2) kost het toevoegen copy-paste, geen isolatie van bugs |
| Herbruikbaarheid | Een andere gemeente kan draaien zonder NL Portal-code te wijzigen, alleen configuratie | Niet geverifieerd in deze review — vereist een aparte configuratie-audit |
| Onderhoudbaarheid | Een externe bijdrager kan een bugfix indienen die binnen de bestaande PR-flow past | Issue-first workflow, Conventional Commits, squash-merge (`CONTRIBUTING.md`) — organisatorisch goed geborgd |
| Testbaarheid | Een ZGW-clientfout (404/401/500) kan unit-getest worden zonder een live ZGW-API | **Nee, vandaag niet mogelijk** — zie architectuurreview kandidaat 1; dit is de scherpste testbaarheidslacune die deze review vond |
| Testbaarheid | Een pagina's data-fetching-logica (loading/error) kan los van rendering getest worden | **Nee, vandaag niet systematisch** — zie architectuurreview kandidaat 4 |
| Onderhoudbaarheid | De Sonar-coverage van 54% (backend) / 42,9% (frontend) wordt juist geïnterpreteerd | Dit zijn overall-cijfers inclusief legacy-code en zeggen niets over de kwaliteit van nieuw werk. New-code-coverage (89,1% backend / 91,9% frontend) is de relevante maatstaf voor met AI-agents gewijzigde code; de overall-cijfers verbeteren pas geleidelijk naarmate legacy-code wordt aangeraakt |
| Onderhoudbaarheid | Een PR die de Sonar-quality-gate op new-code-condities rood maakt, wordt tegengehouden | SonarCloud quality-gate-enforcement is PR-blocking voor zowel backend als frontend (`.github/workflows/sonarcloud.yml`, `sonarsource/sonarqube-quality-gate-action`-stap na de analyse-job) |
| Onderhoudbaarheid/Interoperabiliteit | Een wijziging die de lagen-architectuur doorbreekt (`core` → infra → features → `app`) faalt in CI in plaats van pas bij code review | ArchUnit-tests in de losse `architecture-test`-Gradle-module (draait mee in `./gradlew build`): `core` en de gedeelde infra-modules (`data`, `graphql`, `portal-authentication`) mogen niet afhangen van een feature-module, alleen `app`/`app-dev` mag van de rest afhangen en niets mag van `app`/`app-dev` afhangen, en de zgw-submodules mogen geen cyclische afhankelijkheden hebben |
| Testbaarheid | De `core`-module heeft tests die daadwerkelijk gedrag verifiëren, niet alleen regels uitvoeren | Mutation testing met pitest, beperkt tot `core`: `./gradlew :core:pitest` of `mise run backend:pitest`, buiten de standaard `build` ([ADR 0011](../../development/adr/0011-mutation-testing-backend-core.md), #95). Totaal-score is een ratchet op de gemeten baseline (21% op 2026-11-05, vloer 20%), nieuwe en gewijzigde klassen moeten 80% halen. Draait in CI op PR's die `core` raken en nachtelijk (`.github/workflows/mutation-testing.yml`). Zonder `pitest-kotlin` (commerciële licentie), met `avoidCallsTo` voor Kotlin-intrinsics en logging |
| Onderhoudbaarheid | Een package in de frontend pnpm-workspace kan geen intern bestandspad van een andere package importeren, alleen de publieke export | ESLint-regel `import/no-internal-modules` in `frontend/eslint.config.mjs`, PR-blocking via de bestaande lint-stap |

## 10.2 Hoe dit te onderhouden

Voeg een rij toe zodra een kwaliteitsdoel een concreet, meetbaar scenario krijgt (bijv. een
performance-budget, een beschikbaarheids-SLA). Verwijder een rij niet stilzwijgend als de borging
verandert — noteer in plaats daarvan de nieuwe borging, zodat dit hoofdstuk een geschiedenis van
"waar stonden we" blijft in plaats van alleen een huidige snapshot.
