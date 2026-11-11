# 20. Levenscyclus van feature flags en privacy van de evaluatiecontext

## Status

Geaccepteerd (#148, onderdeel van [#143](https://github.com/pinkroccade/nl-portal/issues/143)). Vult
[ADR 0014](./0014-feature-flags-via-openfeature.md) aan; die legt het mechanisme vast, deze ADR de
afspraken eromheen. De CI-controle op vervaldatums en de eigenaar-controle zijn nog niet gebouwd.

## Context

ADR 0014 kiest OpenFeature met een flagd-bestand als standaard. Zonder afspraken groeit een flagbestand
vanzelf vol met vergeten release-schakelaars, en is het verleidelijk om voor targeting of experimenten
persoonsgegevens in de evaluatiecontext te stoppen. Die context kan bij een externe provider (#147)
buiten de eigen infrastructuur terechtkomen, en NL Portal verwerkt BSN en KVK-nummers. Een lezer die
alleen de code ziet, vraagt zich af waarom de context zo karig is en waarom flags een vervaldatum hebben.

## Beslissing

1. **Vier toggle-typen met een eigen foutgedrag.** *Permission*-flags zijn fail-closed: de code-default is
   uit en een fout of ontbrekende flag weigert. *Ops*-, *experiment*- en *release*-flags vallen terug op
   de waarde in het meegeleverde flagd-bestand, zodat de portal bij een storing van het flag-systeem
   blijft werken.
2. **Naamgeving `<type>.<module>.<naam>`**, bijvoorbeeld `ops.frontend.message-count`. Elke flag heeft
   precies één eigenaar (een team of rol, geen persoon) die bij de flagdefinitie is vastgelegd.
3. **Levensduur per type.** Release- en experiment-flags hebben een vervaldatum; een CI-controle faalt
   zodra een flag over datum is, zodat hij wordt opgeruimd (code en definitie). Ops- en permission-flags
   mogen lang leven en hebben geen vervaldatum.
4. **De evaluatiecontext bevat alleen**: tenant, omgeving, rol of machtigingsniveau en eventueel een
   opaque gebruikers-id. Dat id is de `sub` van de identity provider, of een HMAC (SHA-256) daarvan met
   een geheime sleutel per tenant. **Nooit** BSN, KVK-nummer, naam of ander persoonsgegeven. Een gewone
   hash van een BSN is niet toegestaan: de BSN-ruimte is klein genoeg om hem terug te rekenen.
   Zonder geconfigureerde sleutel laat de code het gebruikers-id weg.
5. **Credentials en andere secrets staan nooit in een flag-systeem**, ook niet als flagwaarde of
   in een flagvariant. De HMAC-sleutel is zelf een secret en komt dus niet uit de flags.
6. **Evaluatie gebeurt alleen in de backend.** De frontend krijgt alleen de uitkomst via het
   frontend-config-endpoint, nooit de context, de flagdefinitie of een flag-server-token.
7. **Het flagd-bestand is de zero-server default.** De productie-provider volgt uit spike #147; deze
   afspraken gelden voor elke provider.

## Overwogen alternatieven

- **Gewone hash (SHA-256) van het gebruikers-id of BSN als context**: voor een BSN is dat met een
  opzoektabel terug te rekenen, dus feitelijk een persoonsgegeven. Een HMAC met per-tenantsleutel is dat
  niet zonder de sleutel, en de sleutel verschilt per tenant zodat ids niet tussen tenants te koppelen zijn.
- **Naam of KVK-nummer als context voor gerichte uitrol**: handig voor targeting, maar een flagprovider
  hoort geen persoonsgegevens te verwerken. Targeten gaat op tenant of rol.
- **Geen vervaldatum, opruimen op gevoel**: dat is de standaardweg naar dode flags en onbedoelde
  codepaden. Een CI-controle kost weinig en maakt opruimen afdwingbaar.
- **Flags evalueren in de frontend (client-side SDK)**: zou de flagdefinitie en een provider-token naar
  de browser brengen; zie beslissing 6.

## Gevolgen

- Een nieuwe flag zonder eigenaar of (voor release/experiment) vervaldatum komt niet door CI. Daarvoor
  is een vervolgticket nodig; het formaat van eigenaar en vervaldatum in het flagd-bestand
  (bijvoorbeeld `metadata`) is dan nog te kiezen.
- Wie targeting op een ander kenmerk wil, moet dat via een nieuwe ADR of herziening van deze
  toestaan; de context blijft bewust klein.
- Bestaande tests (`FeatureContextFactoryTest`, `FeatureContextPrivacyTest`) bewaken beslissing 4 al
  deels; een nieuwe contextattribuut moet daar langs.
- Dezelfde regels gelden voor een externe provider, ook bij logging en telemetrie door die provider.
