# Regeldekking en multitenancy-impact

Dit diagram laat zien hoe de backend is opgebouwd (Gradle-modules per laag), welke afhankelijkheden de
ArchUnit-regels bewaken en waar geen regel dekking geeft. Het wordt gegenereerd uit de bytecode, dus het
veroudert niet: een test faalt als het gecommitte diagram niet meer klopt met de code.

![Modules, lagen en regeldekking](./img/plantuml/modules.svg)

## Zo lees je het diagram

**Pijlen** (afhankelijkheden tussen modules):

| Pijl | Betekenis |
| --- | --- |
| grijs | Toegestaan: de afhankelijkheid wijst naar een lagere laag, zoals bedoeld. |
| groen, "bewaakt" | Beide modules vallen onder de cyclusregel (alle `zgw`-modules en `form`); een wederzijdse afhankelijkheid wordt gevangen. |
| oranje gestippeld, "gat" | Geen enkele regel bewaakt deze afhankelijkheid: de richting kan omslaan of er kan een cyclus ontstaan zonder dat een test faalt. |
| rood gestreept, "verboden" | Een regel verbiedt deze afhankelijkheid. Komt hij voor, dan faalt de build. |

**Modules**: onder de naam staan omvang (S/M/L) en risico van de multitenancy-migratie, met de vulkleur als
risico (rood hoog, geel middel, groen laag), plus de gedetecteerde signalen:

| Signaal | Betekenis |
| --- | --- |
| `config` | Een `@ConfigurationProperties`-singleton wordt buiten de autoconfig gebruikt; per tenant is een register nodig. |
| `webclient` | De module bouwt zelf een `WebClient`; hij hoort in de gedeelde client van de tenant. |
| `globaal` | JVM-globale toestand (`ThreadLocal`, `InMemorySecretKeyStore`, `OpenFeatureAPI`, `GlobalScope`). |

De knoop `tenancy-seam` in de kern is de nog niet gebouwde tenancy-laag (`TenantContext`, `TenantRegistry`). De
notitie eronder toont de vijf regels die het multitenancy-onderzoek (#146) voorstelt. Een regel staat als
"voorgesteld, niet actief" tot `ArchitectureRules` een methode met die naam krijgt; hij springt dan vanzelf naar
"actief".

## Hoe houd je het actueel

| Taak | Wat |
| --- | --- |
| `mise run arch:diagram` | Genereert `site/architectuur/img/plantuml/modules.puml` uit de code. |
| `mise run arch:diagram:render` | Rendert het `.puml` naar `modules.svg` (Docker, `plantuml/plantuml`). |
| `mise run arch-check` | Draait de ArchUnit-tests, inclusief de controle dat het diagram en de baseline actueel zijn. |

Commit na een wijziging het `.puml` en de `.svg` samen.

### De tenancy-baseline

Omvang, risico en de reden van elk bekend signaal staan in `docs/tenancy-baseline.yaml`. Alleen het oordeel
staat daar; de signalen zelf worden elke run uit de code afgeleid. De test faalt in beide richtingen:

- een **nieuw signaal** dat niet in de baseline staat: los het op, of voeg het met reden toe;
- een **baseline-signaal dat uit de code verdween**: haal het uit het bestand, de module is dan gemigreerd;
- een module zonder regel in de baseline, of een regel voor een module die niet meer bestaat.

De huidige waarden zijn voorlopig, overgenomen uit onderzoek #146, en nog niet door de maintainer beoordeeld.

## Wat dit diagram niet laat zien

- Alleen de backend; de frontend (`frontend/packages/*`, bewaakt via ESLint `package-boundaries`) volgt.
- Afhankelijkheden op moduleniveau, niet per package binnen een module.
- Anonieme inbound endpoints die tenantrouting nodig hebben; dat is geen bytecode-signaal en staat nog niet in het beeld.
- Of een regel de afhankelijkheid ook echt tegenhoudt: dat bewijst `ArchitectureRulesProofTest`.

Zie [#386](https://github.com/pinkroccade/nl-portal/issues/386) voor het ontwerp en de vervolgstappen.
