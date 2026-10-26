# 1. Inleiding en doelen

## 1.1 Opgave

NL Portal is een open source "Mijn Omgeving"-platform voor de Nederlandse overheid, gebaseerd op
[Common Ground](https://commonground.nl/). Het verzorgt de interactie tussen een **klant** (burger of
ondernemer) en **ketenpartners** (organisaties die bijdragen aan de afhandeling van een verzoek):
berichten, taken, inzicht in zaakdossiers, inzicht in eigen gegevens (BRP), track-and-trace van
zaken, en (in ontwikkeling) domein-specifieke gegevens.

NL Portal doet zelf geen zaakafhandeling, is geen formulierencomponent en geen CMS — het haalt data
op bij bronsystemen (primair via VNG's "ZGW API's voor Zaakgericht Werken") en toont die aan de
klant. Zie [`documentation/fundamentals/architectuur/README.md`](../../documentation/fundamentals/architectuur/README.md)
voor de volledige productscope.

Motto van het project: **"create once, use 340 times"** — iedere (Nederlandse) overheidsorganisatie
kan het vrij gebruiken en verbeteren. Dat motto is architecturaal relevant: het drijft de nadruk op
configureerbaarheid, standaarden (ZGW API's) in plaats van maatwerkkoppelingen, en het scheiden van
data (bij de bron) en applicatielogica (in NL Portal).

## 1.2 Kwaliteitsdoelen (voorstel)

Afgeleid uit de Common Ground-principes, het EUPL-licentiemodel en de huidige CI/beveiligingsopzet.
**Dit is een voorstel voor hoofdstuk 10's kwaliteitsboom, geen vastgestelde lijst — valideer met de
productverantwoordelijke(n):**

| # | Kwaliteit | Waarom dit prioriteit heeft |
|---|---|---|
| 1 | **Vertrouwd / beveiligd** | Verwerkt BRP-gegevens en overheids-identiteit (DigiD/eHerkenning); één van de vijf Common Ground-principes is expliciet "vertrouwd". |
| 2 | **Herbruikbaarheid / portabiliteit** | "340 keer hergebruiken" vereist dat het platform generiek blijft en niet vastgroeit aan één gemeente se databron. |
| 3 | **Interoperabiliteit** | Moet werken tegen de ZGW-API-standaard en tegen meerdere implementaties daarvan (Open Zaak, OpenKlant, ...), niet tegen één specifieke leverancier. |
| 4 | **Onderhoudbaarheid** | Open source met een bredere contributor-basis dan één team; lage instapdrempel voor nieuwe bijdragers is een expliciet doel (zie `CONTRIBUTING.md`). |
| 5 | **Testbaarheid** | Backend- en frontend-CI draaien verplicht tests; zie hoofdstuk 10 voor concrete scenario's. |

## 1.3 Stakeholders

| Rol | Belang |
|---|---|
| Burger / ondernemer (eindgebruiker) | Begrijpelijk, snel en betrouwbaar inzicht in eigen zaken en gegevens |
| Ketenpartner-medewerker | Zicht op voor hen relevante zaakinformatie en taken |
| Gemeente / overheidsorganisatie (afnemer) | Platform dat aansluit op hun eigen ZGW-landschap zonder vendor lock-in |
| pinkroccade (eigenaar/maintainer) | Kwaliteit en coherentie van het open source product bewaken |
| Bijdragende ontwikkelaars (community) | Begrijpelijke, goed gedocumenteerde codebase om in bij te dragen |
