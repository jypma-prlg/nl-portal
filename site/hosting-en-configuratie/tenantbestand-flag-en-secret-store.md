# Tenantbestand, flag en secret store

> **Voorgesteld, nog niet geïmplementeerd.** Deze pagina beschrijft het doelbeeld uit
> [ADR 0017](../development/adr/0017-gedeelde-multi-tenant-runtime-met-cellen.md) (status: voorgesteld). Wat vandaag
> werkt, staat in de [Deployment guide](./deployment-guide.md): één deployment is nu één gemeente.

Als NL Portal als gedeelde dienst voor meerdere gemeenten (tenants) draait, staat de inrichting van een
tenant op drie verschillende plekken. Elke plek heeft een eigen soort inhoud en een eigen wijzigingsroute.
Het onderscheid is bewust: zo weet je altijd waar je een wijziging doet en wat die kost.

## De drie lagen

| | Tenantbestand | Flag | Secret store |
|---|---|---|---|
| **Wat staat erin** | Domeinen, token-issuer, URL's van koppelingen, thema, de cel waarin de tenant draait, en *verwijzingen* naar secrets (op naam) | Alleen aan of uit per module, per tenant | De secrets zelf: client secrets, API-sleutels, sleutelmateriaal |
| **Wat staat er nooit in** | Secrets en tokens | URL's, credentials, tenantconfiguratie, beveiligingsbesluiten | Niets anders dan secrets |
| **Waar leeft het** | Git, één bestand per tenant per omgeving | Het flagbestand (OpenFeature/flagd) | De secret store, ontsloten via External Secrets Operator |
| **Hoe wijzig je het** | Pull request, daarna uitrol door de gitops-tooling | Het flagbestand aanpassen | Roteren in de secret store |
| **Wanneer werkt het** | Na een rolling restart | Direct, zonder herstart | Na een rolling restart |

Een wijziging in het tenantbestand of de secrets herstart alleen de cel van die tenant, nooit de hele
dienst. Er is geen hot reload en er is geen Configuration Panel meer.

## Wat doe je waar

- **Een koppeling instellen of wijzigen** (bijvoorbeeld de URL van de Zaken API): in het tenantbestand.
  Dit kost een rolling restart van de cel.
- **Het thema, logo of de domeinnaam wijzigen**: in het tenantbestand, ook via een rolling restart.
  Zie [Eigen vormgeving](../gebruik-en-inrichting/eigen-vormgeving.md) voor de ontwerpkant.
- **Een secret toevoegen of roteren** (bijvoorbeeld het client secret van een koppeling): in de secret store.
  Het tenantbestand verwijst alleen naar de naam. De nieuwe waarde komt na een rolling restart van de cel in
  gebruik.
- **Een module voor een tenant aan- of uitzetten**: met de flag van die module. Dit werkt direct. Een module
  is alleen bruikbaar als de configuratie in het tenantbestand aanwezig is **én** de flag aanstaat. Een flag
  zonder configuratie doet niets (CI waarschuwt hiervoor).
- **Een module snel uitschakelen voor één tenant** (kill switch): de flag van die tenant uitzetten; er is
  geen herstart nodig.

## Een module uitrollen in twee stappen

1. Zet de configuratie van de module in het tenantbestand en rol uit via een rolling restart. De module blijft
   uit, want de flag staat nog uit. Zo zie je fouten voordat een burger iets merkt (een onvolledige tenant
   start niet, en de oude pods blijven draaien).
2. Zet de flag voor die tenant aan. Dit werkt direct.

## Waarom geen URL of secret in een flag

Een flag-systeem is bedoeld voor schakelaars. Een URL is zonder secret niet bruikbaar en secrets horen
nooit in een flag-systeem. Daarom bevat een flag alleen aan of uit. Flagwijzigingen werken live (het
flagbestand wordt periodiek herladen); dat is de enige bewuste uitzondering op "elke wijziging is een rolling
restart" en geldt alleen voor aan/uit-schakelaars.

## Controles

- **In CI:** elk tenantbestand wordt tegen een schema gevalideerd (onder andere gequote OIN's), domeinen en
  `keyId`'s moeten over alle tenants uniek zijn, en een overzicht voegt tenantbestanden en flags samen tot een
  modulematrix.
- **Bij opstarten:** de runtime weigert te starten bij een incomplete tenant, een dubbel domein of dubbele
  `keyId`, of een token-issuer die niet bij de publieke ID-broker hoort.

## Zie ook

- [ADR 0017](../development/adr/0017-gedeelde-multi-tenant-runtime-met-cellen.md), de beslissing en de afweging.
- [ADR 0016](../development/adr/0016-stateless-configuratie-en-thema-via-bestanden.md), configuratie via bestanden.
- [Deployment guide](./deployment-guide.md#feature-flags), hoe flags vandaag werken.
