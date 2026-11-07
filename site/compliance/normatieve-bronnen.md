# Normatieve bronnen

Register van de externe en interne bronnen waaraan NL Portal zich bewust toetst (ASVS, RFC's, het
5-lagenmodel), met per bron een vast ID, de versie en de datum van raadpleging. De
traceerbaarheidsmatrix daaronder volgt elke bron naar de eis, de guardrail of check die de eis
afdwingt en het bewijs. Een bron die we bewust niet toepassen krijgt de status *Bewust niet
toegepast* met een verwijzing naar het besluit.

`mise run compliance:check` bewaakt dit register en draait in CI. Het faalt op een ID dat ergens in
`site/`, `backend/` (Kotlin) of `.github/workflows/` wordt genoemd maar hier niet bestaat, en op een
bron zonder check of afwijkingsbesluit. Periodiek nalopen of de versies nog actueel zijn staat in
[periodieke checks](../development/workflows/periodic-checks.md#6-bronversies-nalopen).

## Register

Een ID heeft de vorm `N-<BRON>-<VERSIE>`. Verwijs in ArchUnit-`.because()`-teksten, ADR's, workflows
en toetsingsdocumenten naar dit ID waar een bron wordt uitgevoerd.

| ID | Bron | Versie | Geraadpleegd op | Wijze van raadplegen |
|---|---|---|---|---|
| `N-ASVS-5.0` | [OWASP ASVS](https://github.com/OWASP/ASVS/tree/v5.0.0_release/5.0/en), hoofdstukken V6, V8, V9, V10, V12, V13 | 5.0.0 (2025-05-30), level 3 | 2026-11-05 | Zelf gelezen (V6, V8, V9, V10 volledig; V12 en V13 alleen de eisentabellen) |
| `N-RFC-8693` | [RFC 8693, OAuth 2.0 Token Exchange](https://www.rfc-editor.org/rfc/rfc8693) | RFC 8693 (2020-01) | 2026-11-05 | Niet zelf gelezen: via een samenvattende fetch-tool |
| `N-KEYCLOAK-TOKEN-EXCHANGE` | [Keycloak, token exchange](https://www.keycloak.org/securing-apps/token-exchange) | Niet vastgesteld (de pagina noemt geen Keycloak-versie) | 2026-11-05 | Niet zelf gelezen: via een samenvattende fetch-tool |
| `N-CG-5LAGEN` | Common Ground 5-lagenmodel, zoals uitgewerkt in [5-lagenmodel](../architectuur/5-lagenmodel.md) | Interne uitwerking, geen versienummer | 2026-11-08 | Zelf gelezen (interne pagina; het externe model is niet opnieuw geraadpleegd) |

RFC 9068 en RFC 8707 zijn achtergrond bij ASVS-eisen 9.2.x en 10.3.1 en staan daarom niet als eigen bron in dit register; ze komen erbij zodra een guardrail of besluit ze uitvoert. RFC 8693 en de Keycloak-pagina zijn tweedehands: uitspraken die we daaruit overnemen moeten
voor definitief gebruik tegen de bron zelf gelezen worden. Het onderzoek
[ASVS 5.0.0 level 3 voor token-exchange](../development/research/asvs-5-l3-token-exchange.md#1-bronnen-en-versie)
beschrijft per hoofdstuk wat precies is gelezen.

## Statussen

| Status | Betekenis |
|---|---|
| Afgedwongen | Een geautomatiseerde guardrail (test, lint, CI-check) faalt bij een schending. Vereist guardrail en bewijs. |
| Getoetst | Handmatig getoetst aan de code of configuratie, met vastgelegd resultaat. Vereist guardrail (waar de eis in de code zit) en bewijs. |
| Bewust niet toegepast | We passen de eis niet toe. Vereist een verwijzing naar het besluit. |
| Open | Nog niet getoetst of nog niet voldaan; verwijst naar het issue of de bevinding. Telt niet mee als dekking. |

## Traceerbaarheidsmatrix

Bron → eis → guardrail of check → bewijs.

| Bron | Eis | Status | Guardrail of check | Bewijs | Besluit of verwijzing |
|---|---|---|---|---|---|
| `N-ASVS-5.0` | 9.1.1 (L1): handtekening van het token valideren | Getoetst | `CommonGroundAuthenticationConverter` met `jwk-set-uri`-decoder | [Toetsing token exchange, 9.1.1](../development/research/asvs-5-l3-toetsing-token-exchange.md) | - |
| `N-ASVS-5.0` | 9.2.1 (L1): `exp` en `nbf` valideren | Getoetst | `JwtTimestampValidator` (Spring-standaard, 60 s skew) | [Toetsing token exchange, 9.2.1](../development/research/asvs-5-l3-toetsing-token-exchange.md) | - |
| `N-ASVS-5.0` | 10.4.10 (L2): clientauthenticatie met confidential client | Getoetst | Token exchange gebruikt `client_id` en `client_secret` in de form-body | [Toetsing token exchange, 10.4.10](../development/research/asvs-5-l3-toetsing-token-exchange.md) | - |
| `N-ASVS-5.0` | 9.2.3, 9.2.4, 10.3.1 (L2): `aud` valideren | Open | Property `spring.security.oauth2.resourceserver.jwt.audiences`; de app image zet die niet aan | - | [Toetsing token exchange](../development/research/asvs-5-l3-toetsing-token-exchange.md) (resultaat "Na inrichting") |
| `N-ASVS-5.0` | 10.3.5, 10.4.14, 10.4.16 (L3): sender-constrained tokens en sterke clientauthenticatie | Open | - | - | [Toetsing token exchange, open punten](../development/research/asvs-5-l3-toetsing-token-exchange.md) (besluit over DPoP/mTLS en `private_key_jwt` nodig) |
| `N-RFC-8693` | Token exchange zonder impersonation; machtiging is een eigen claimmodel | Getoetst | `CommonGroundAuthenticationConverterTokenExchangeTest` | [Onderzoek ASVS L3 token exchange](../development/research/asvs-5-l3-token-exchange.md) | - |
| `N-KEYCLOAK-TOKEN-EXCHANGE` | Standard token exchange V2 als enige pad | Getoetst | `CommonGroundAuthenticationConverterTokenExchangeTest` | [Onderzoek ASVS L3 token exchange](../development/research/asvs-5-l3-token-exchange.md) | - |
| `N-CG-5LAGEN` | Core hangt niet af van een feature-module | Afgedwongen | ArchUnit-regel `ArchitectureRules.coreDoesNotDependOnFeature` | `LayerArchitectureTest` (draait in `./gradlew build`) | - |
| `N-CG-5LAGEN` | Gedeelde infrastructuur hangt niet af van een feature-module | Afgedwongen | ArchUnit-regel `ArchitectureRules.sharedInfraDoesNotDependOnFeature` | `LayerArchitectureTest` | - |
| `N-CG-5LAGEN` | Alleen `app` en `app-dev` stellen de modules samen | Afgedwongen | ArchUnit-regel `ArchitectureRules.nothingDependsOnApp` | `LayerArchitectureTest` | - |
| `N-CG-5LAGEN` | Modules hangen niet cyclisch van elkaar af | Afgedwongen | ArchUnit-regel `ArchitectureRules.cycleFree` | `LayerArchitectureTest` | - |

Aanvullende rijen komen erbij zodra een ticket een bron uitvoert; de overige eisen uit het
[onderzoek](../development/research/asvs-5-l3-token-exchange.md) staan in de
[toetsing](../development/research/asvs-5-l3-toetsing-token-exchange.md) en worden pas hier
opgenomen als ze een guardrail of besluit krijgen.
