# 14. Feature flags via OpenFeature met een flagd-bestand als standaard

## Status

Geaccepteerd en deels uitgevoerd (#154, onderdeel van #143). Beslissing 7 (rolling update in plaats van
hot reload) is toegevoegd in #389. De keuze van een productie-provider is
bewust uitgesteld (spike #147). De afspraken over levenscyclus en privacy van de evaluatiecontext volgen
in een eigen ADR (#148).

## Context

Het gedrag van de backend (welke modules en koppelingen actief zijn) en de frontend-toggles werden
bepaald door statische configuratie die bij het opstarten wordt vastgelegd. Dat past niet bij een
product dat Pink als SaaS aanbiedt en later multi-tenant wil draaien, en het geeft geen uniforme manier
om ops-, permission-, experiment- en release-toggles te sturen. De code is EUPL; een andere
dienstverlener moet een eigen flag-systeem kunnen aansluiten. Daarnaast zat een vaste dev-omgeving
met credentials in versiebeheer (dat deel pakt #143 apart aan; flags zijn daar geen oplossing voor,
want credentials horen niet in een flag-systeem).

## Beslissing

1. De backend gebruikt **OpenFeature** (CNCF, Java server-SDK) als enige flag-API. Featurecode kent
   alleen `FeatureGate`; geen leverancierstypen buiten de provider-aansluiting.
2. De standaardprovider is **flagd in bestandsmodus**: werkt zonder flag-server en is ook de testprovider. Een andere OpenFeature-provider sluit je aan met een eigen
   `FeatureProvider`-bean.
3. De **productie-provider is nog niet gekozen**. Kandidaten staan in spike #147 (Unleash, Flagsmith).
4. Evaluatie gebeurt alleen in de backend; de frontend ontvangt de uitkomst via het bestaande
   frontend-config-endpoint, zodat de browser nooit met een flag-systeem praat.
5. Vier toggle-typen: ops, permission, experiment, release. Een permission-flag is fail-closed: hij kan
   niet standaard aan staan en weigert bij een fout.
6. Credentials en andere secrets staan nooit in een flag-systeem.
7. **Wijzigingen landen via een rolling update, niet via hot reload.** Dit volgt het afhankelijkheidsmodel
   en [ADR 0016](0016-stateless-configuratie-en-thema-via-bestanden.md): een gewijzigd flagbestand
   (ConfigMap) of een andere provider-configuratie wordt actief doordat pods één voor één herstarten
   (checksum-annotatie op de Deployment). Het bestand mag live veranderen, maar daar rekenen we niet op:
   de poll van de flagd-bestandsprovider (`nl-portal.feature-flags.poll-interval-ms`, standaard 5 s) is een
   eigenschap van de bibliotheek die zich niet netjes laat uitzetten, geen gedrag dat we beloven. Een
   externe provider (#147) mag wel runtime-evaluatie bieden; dat is een eigenschap van die provider, geen
   eis aan NL Portal. Er is geen nieuwe ADR: dit is een verduidelijking van deze beslissing.

## Overwogen alternatieven

- **Eigen properties met een minimale eigen provider**: eenvoudiger, maar laat een NL Portal-specifiek
  formaat achter dat we zelf moeten onderhouden en dat integrators niet kunnen vervangen.
- **Unleash direct**: open source (AGPL-3.0) maar geen CNCF-project; de OpenFeature-provider is alpha
  en de gratis versie beperkt (één project, twee omgevingen; SSO en fijnmazige rollen zijn enterprise).
  Blijft kandidaat voor productie (#147).
- **Flipt**: de serverlicentie is geen OSI-licentie, een risico voor een SaaS-aanbieder.
- **GO Feature Flag**: geen beheer-UI.
- **Flagsmith**: kandidaat voor de spike.

## Gevolgen

- Nieuwe dependencies: de flagd-provider trekt gRPC en protobuf mee. Dat dwong twee buildwijzigingen af:
  de legacy-repositories bevragen OpenFeature-artefacten niet meer (de provider gebruikt versiebereiken
  die falen bij een onbereikbare repository) en guava is overschreven vanwege een kwetsbaarheid.
- `OpenFeatureAPI` is een globale singleton. Tests in dezelfde JVM kunnen elkaars provider overschrijven.
- Bij een mislukte evaluatie valt een flag terug op de standaardwaarde in de code, niet op het
  meegeleverde bestand. Een permission-flag valt altijd terug op uit.
- Bestaande toggles en modules worden stapsgewijs gemigreerd (#156, #157 en volgende); tot die tijd
  bestaan oude properties en flags naast elkaar.
- Er is geen migratiepad voor de oude toggle-properties (ADR 0012).
