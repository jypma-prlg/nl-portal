# BIO2 en ASVS 5.0 L3: eisen aan logische tenantscheiding

Geraadpleegd op: 2026-11-06. Onderzoek voor #198, onderdeel van de kaart #197 (gedeelde multi-tenant runtime). Gebruikt door #203 en #205.

## Vraag

Welke concrete eisen stellen BIO2 en OWASP ASVS 5.0.0 (level 3) aan een **gedeelde runtime** die gegevens van meerdere overheidsorganisaties (tenants, sleutel = OIN) verwerkt, zonder eigen database ([ADR 0013](../adr/0013-stateless-backend-zonder-database.md)), met BSN/KVK-gegevens uit ZGW- en Haal Centraal-bronnen per tenant? Welke eisen sluiten een gedeelde runtime uit of stellen er voorwaarden aan, en wat verwachten auditors als bewijs?

## Samenvatting

- **Geen van beide kaders sluit een gedeelde runtime uit.** BIO2 vraagt dat informatie met specifiek belang "fysiek en/of logisch" wordt geïsoleerd (overheidsmaatregel **8.03.01**). ASVS vraagt dat een multi-tenant applicatie "cross-tenant controls" heeft (**v5.0.0-8.4.1**, L2). Geen van beide eist een eigen proces, eigen namespace of eigen cluster per tenant.
- **De voorwaarden zitten in sleutels, tokens, logging en bewijs**, niet in de procesgrens:
  - **Sleutels en secrets per tenant**: niet overdelen (v5.0.0-11.1.1); secretsbeheer bij L3 **hardware-backed (HSM)** (v5.0.0-13.3.1); bij L3 alle cryptografische operaties in een geïsoleerde securitymodule (v5.0.0-13.3.3, hangt niet van tenancy af maar wordt in een gedeelde runtime wel scherper).
  - **Tokens tenantgebonden**: `aud`/`iss` per tenant valideren (9.2.3, 9.2.4, 10.3.1), gebruiker = `iss`+`sub` (10.3.3), tenant uit het token afleiden en niet alleen uit host of header (8.3.1, 4.1.3).
  - **Logging**: elke logregel met tenant (BIO 8.15.01 "Object"/"Actor"; v5.0.0-16.2.1); bij L3 elke autorisatiebeslissing en elke inzage van gevoelige gegevens loggen (16.3.2); loginzage per tenant beperkt (8.03.02, 16.4.2).
  - **Configuratiewijzigingen**: wijzigingsbeheer met risicoafweging en rollback-plan (BIO 8.32.01); in een gedeelde runtime raakt elke wijziging alle tenants, dus de risicoafweging moet de hele vloot dekken.
- **De gedeelde runtime is één "gescheiden groep"** in de zin van BIO 8.22.01 en moet daarom het beveiligingsniveau van de zwaarste tenant halen. Een gemeente met een hogere classificatie trekt alle tenants mee omhoog.
- **BIO2 verplicht de gemeente, niet Pink.** Pink valt eronder als leverancier via inkoopcontracten (BIO2 deel 1 §13; overheidsmaatregelen 5.19.01 t/m 5.23.01). Gevolg: jaarlijks bewijs van onafhankelijke derden (5.20.03), auditrecht van elke gemeente (5.20.04), transparantie over kwetsbaarheden en incidenten (5.20.05). Bij ~50 tenants op één runtime is een gedeeld assurancerapport praktisch noodzakelijk.
- **Bewijs dat auditors verwachten**: opzet, bestaan en werking (BIO2 deel 1 §4; Cyberbeveiligingsregeling sector overheid art. 5 lid 3), een penetratietest bij iedere release van een internetfacing systeem met blokkade bij hoog risico (8.08.05), gestructureerde, waar mogelijk geautomatiseerde acceptatietesten met verslag (8.29.01). ASVS adviseert hybride (broncode-gestuurde) pentests en applicatiespecifieke geautomatiseerde tests voor autorisatie (hoofdstuk *Assessment and Certification*). Cross-tenant tests en architectuurchecks zijn dus het bewijs van "werking" voor 8.03.01 en 8.4.1.

## 1. Bronnen en versie

| Bron | Link | Opmerking |
|---|---|---|
| BIO2 v1.3 (tekst als circulaire, herplaatsing) | https://zoek.officielebekendmakingen.nl/stcrt-2026-7416-n1.html | Staatscourant 2026, 7416 n1 (5 maart 2026). Deel 1 en deel 2 volledig gelezen. Versie 1.3 definitief, 09-01-2026. |
| BIO2 v1.3 (origineel) | https://zoek.officielebekendmakingen.nl/stcrt-2026-7416.html | Gelijk aan n1 voor deel 2; n1 bevat de volledige wijzigingstabel. |
| Cyberbeveiligingsregeling sector overheid | https://zoek.officielebekendmakingen.nl/stcrt-2026-27679.html | Staatscourant 2026, 27679. Art. 5 maakt BIO2 v1.3 wettelijk verplicht voor essentiële entiteiten in de sector overheid, met uitzondering van overheidsmaatregelen 5.32.01 t/m 5.34.01. |
| BIO2-productpagina | https://www.bio-overheid.nl/bio2/bio-producten/baseline-informatiebeveiliging-overheid-2-bio2/ | v1.3 vervangt sinds 5 maart 2026 BIO v1.04 en BIO2 v1.2. De PDF op bio-overheid.nl was niet op te halen (HTTP 403); de Staatscourant-tekst is dezelfde versie en is gebruikt. |
| ASVS 5.0.0 V2, V3, V4, V7 t/m V16 | https://github.com/OWASP/ASVS/tree/v5.0.0_release/5.0/en | Eisentabellen gelezen uit de tag `v5.0.0_release`. |
| ASVS 5.0.0 Assessment and Certification | https://github.com/OWASP/ASVS/blob/v5.0.0_release/5.0/en/0x04-Assessment_and_Certification.md | Volledig gelezen. |

Notatie: BIO-overheidsmaatregelen als `8.03.01` (de eerste drie cijfers verwijzen naar de beheersmaatregel in ISO/IEC 27002:2022, BIO2 deel 2, inleiding). ASVS-eisen als `v5.0.0-8.4.1` of kortweg `8.4.1` in tabellen; het niveau staat erbij.

## 2. Hoe BIO2 werkt (relevant voor het ontwerp)

- **Wie is verplicht.** BIO2 deel 1 en deel 2 "hebben een verplichtend karakter en worden altijd gevolgd" (deel 1 §3). De verplichting ligt bij de **entiteit** (de gemeente). Een leverancier valt eronder via de inkoop: "Een entiteit blijft zelf verantwoordelijk voor het behandelen van risico's die betrekking hebben op de uitbestede of ingekochte dienst" (deel 1 §13).
- **Wettelijk.** Voor essentiële entiteiten in de sector overheid (behalve waterschappen, die een eigen regeling hebben) is BIO2 v1.3 via de Cyberbeveiligingsregeling sector overheid art. 5 wettelijk verplicht. Overheidsmaatregelen buiten de Cbw-reikwijdte blijven "verplichtende zelfregulering" (deel 1 §11.1).
- **Overheidsmaatregelen zijn het minimum.** Ze "zijn altijd verplicht en kunnen ongeacht de risico-inschatting van de entiteit niet geaccepteerd worden, tenzij ze niet van toepassing kunnen zijn" (deel 1 §6.5). Bij een beheersmaatregel zónder overheidsmaatregel geldt de implementatierichtlijn uit ISO/IEC 27002; afwijken wordt met een risicoanalyse onderbouwd in de VvT (deel 2, inleiding).
- **Gevolg voor tenancy.** De beheersmaatregelen die het meest over scheiding gaan, hebben in BIO2 **geen** overheidsmaatregel: 8.11 (data masking), 8.12 (data leakage prevention), 8.9 (configuratiebeheer), 8.25/8.26/8.28 (veilige ontwikkeling) en 5.34 (privacy). Daar geldt de ISO-richtlijn plus de risicoanalyse van de gemeente. BIO2 schrijft dus geen technische vorm van tenantscheiding voor; de gemeente moet haar keuze verantwoorden.
- **Niet geverifieerd:** welke overheidsmaatregelen in de PDF grijs (buiten Cbw) of vet zijn gemarkeerd. De HTML-tekst van de Staatscourant toont die opmaak niet. Wel vast: art. 5 van de Cyberbeveiligingsregeling sluit 5.32.01 t/m 5.34.01 uit.
- **Niet geverifieerd:** de titels en implementatierichtlijnen van ISO/IEC 27002:2022 zelf (betaalde norm, niet gelezen). De titels hieronder zijn ter oriëntatie.

## 3. Tabel: control → eis → gevolg voor het ontwerp

Kolom "Scope" zegt of de eis een gedeelde runtime **uitsluit**, er een **voorwaarde** aan stelt, of **neutraal** is (geldt ongeacht tenancy, maar raakt het ontwerp).

### 3.1 Scheiding van gegevens en autorisatie

| Control | Eis (kort, bron) | Scope | Gevolg voor het ontwerp |
|---|---|---|---|
| BIO 8.03.01 (ISO 8.3 Information access restriction) | "Er zijn maatregelen genomen die het fysiek en/of logisch isoleren van informatie met specifiek belang waarborgen." | Voorwaarde | Logische isolatie is toegestaan. BSN/KVK-gegevens zijn "informatie met specifiek belang". Elke uitgaande call naar ZGW/Haal Centraal moet met de tenantconfiguratie van het verzoek lopen; nooit een tenant-loze of standaard-client. |
| BIO 8.03.02 | "Gebruikers kunnen alleen die informatie met specifiek belang inzien en verwerken die ze nodig hebben voor de uitoefening van hun taak." | Voorwaarde | Geldt voor burgers en voor beheerders. Ook Pink-beheerders mogen niet zonder meer logs/gegevens van alle tenants inzien. |
| BIO 8.22.01 (ISO 8.22 Segregation of networks) | "Alle gescheiden groepen hebben een gedefinieerd beveiligingsniveau." | Voorwaarde | De gedeelde runtime (en zijn namespace/netwerksegment) is één groep. Zijn beveiligingsniveau is het hoogste dat een tenant vraagt. Leg het niveau vast in de ADR. |
| BIO 5.12.01 (ISO 5.12 Classification) | Informatie wordt met een expliciete risicoafweging geclassificeerd. | Voorwaarde | Elke gemeente classificeert zelf. Pink moet één classificatie voor de gedeelde runtime afspreken die voor alle tenants volstaat, of een tenant met hogere eisen een eigen instance/cel bieden. |
| ASVS 8.4.1 (L2) | Multi-tenant applicaties gebruiken cross-tenant controls zodat operaties van een consumer nooit tenants raken waarmee hij geen rechten heeft. | Voorwaarde | Kern-eis. Tenant moet een verplicht onderdeel van elke autorisatiebeslissing zijn, centraal afgedwongen (core) en met architectuurchecks bewaakt. |
| ASVS 8.2.2 (L1) | Data-specifieke toegang alleen met expliciete rechten op dat data-item (IDOR/BOLA). | Neutraal | Een zaak-/document-ID van tenant A mag via tenant B niets opleveren, ook niet als de bron-API het ID kent. |
| ASVS 8.3.1 (L1) | Autorisatie in een vertrouwde servicelaag, niet op door de client manipuleerbare gegevens. | Voorwaarde | Tenantbepaling niet alleen uit `Host`, pad of een header die de gebruiker kan zetten. Afleiden uit het gevalideerde token (`iss`/`aud`) en controleren tegen het domein. |
| ASVS 4.1.3 (L2) | Headers die een tussenlaag zet, kan de eindgebruiker niet overschrijven. | Voorwaarde | Als ingress een tenantheader zet (bijv. `X-Forwarded-Host`), moet die niet van buiten te vervalsen zijn. |
| ASVS 8.3.3 (L3) | Toegang op basis van de rechten van de oorspronkelijke subject, niet van een tussenpersoon (bijv. machine-to-machine-token). | Voorwaarde | Een gedeelde m2m-identiteit naar ZGW/Haal Centraal voor alle tenants is bij L3 een afwijking. Minimaal: credentials per tenant; liefst token exchange met de burger als subject (zie [token-exchange-onderzoek](./asvs-5-l3-token-exchange.md)). |
| ASVS 8.1.1 (L1), 8.1.3 (L3) | Autorisatieregels en contextattributen voor beslissingen zijn gedocumenteerd. | Neutraal | Documenteer tenant (OIN) als contextattribuut en hoe het wordt vastgesteld. |
| ASVS 14.2.2 (L2) | Gevoelige gegevens niet cachen in servercomponenten, of veilig opschonen. | Voorwaarde | Elke (toekomstige) cache moet de tenant in de sleutel hebben. Nu zijn in de backend geen `@Cacheable`/Caffeine-caches gevonden. |
| ASVS 15.4.1 (L3) | Gedeelde objecten in multi-threaded code thread-safe benaderen. | Voorwaarde | Tenantcontext niet in statische of thread-local state; in WebFlux via Reactor-context propageren, zodat een ander verzoek op dezelfde thread de tenant niet erft. |
| ASVS 3.5.4 (L2) | Afzonderlijke applicaties op aparte hostnamen. | Voorwaarde (al voldaan in opzet) | Gemeentespecifiek domein per tenant past hierbij. |
| ASVS 3.3.3 (L2) | Cookies met `__Host-`-prefix tenzij bewust gedeeld. | Voorwaarde | Voorkomt dat cookies tussen tenantdomeinen of subdomeinen lekken. |

### 3.2 Sleutels en secrets

| Control | Eis (kort, bron) | Scope | Gevolg voor het ontwerp |
|---|---|---|---|
| ASVS 11.1.1 (L2) | Gedocumenteerd sleutelbeheerbeleid (bijv. NIST SP 800-57); sleutels niet overdelen (shared secrets met niet meer dan twee entiteiten, private keys met niet meer dan één). | Voorwaarde | Eén client secret of API-sleutel voor meerdere gemeenten (bijv. naar een ZGW-bron of Keycloak-client) is overdelen. Per tenant eigen secrets en eigen clients. |
| ASVS 13.3.1 (L2; L3 extra) | Secretsbeheer (key vault); geen secrets in broncode of build-artefact. Voor L3 "hardware-backed solution such as an HSM". | Voorwaarde | ESO is alleen de koppeling; de achterliggende store moet bij L3 HSM-backed zijn. Leg de keuze vast in #203. |
| ASVS 13.3.2 (L2) | Toegang tot secrets volgens least privilege. | Voorwaarde | Splits ESO-`SecretStore`/policies zo dat een fout in de config van tenant A niet de secrets van tenant B ontsluit. In één pod heeft de runtime per definitie alle tenantsecrets in geheugen; documenteer dat als restrisico. |
| ASVS 13.3.3 (L3) | Alle cryptografische operaties in een geïsoleerde securitymodule (vault/HSM). | Neutraal, maar zwaar | Geldt ook single-tenant. In-process HMAC (bijv. de pseudonieme gebruikers-ID in `FeatureContextFactory`) en private-key-JWT voor clientauthenticatie voldoen hier strikt niet aan. Besluit nodig: implementeren via vault-transit/HSM of gedocumenteerde afwijking. |
| ASVS 13.3.4 (L3), 13.1.4 (L3) | Secrets verlopen en roteren volgens een gedocumenteerd schema. | Voorwaarde | Rotatie per tenant zonder herstart van de hele vloot is lastig zonder hot reload. Met "altijd rolling restart" (#197) moet een rotatie van één tenant een gecontroleerde vlootbrede herstart zijn; documenteer dat. |
| ASVS 11.1.2 (L2) | Cryptografische inventaris van sleutels, algoritmen, certificaten, inclusief waar ze wel/niet gebruikt mogen worden. | Neutraal | Per tenant opnemen; BIO 8.24.02 vraagt hetzelfde. |
| BIO 8.24.01 / 8.24.02 (ISO 8.24 Use of cryptography) | Cryptografiebeleid (wie verantwoordelijk voor sleutelbeheer; registratie waar welke cryptografie); cryptografische maatregelen in de inventaris van bedrijfsmiddelen. "Bij communicatie tussen entiteiten wordt het beleid onderling vastgesteld." | Voorwaarde | Elke gemeente moet het sleutelbeheer voor haar portaal kunnen herleiden. Pink levert per tenant een overzicht van sleutels/certificaten. |
| BIO 5.14.02 | OV-certificaten (of beter) voor openbaar webverkeer met gevoelige gegevens. | Neutraal | Bestaand certificaatproces (buiten scope #197). |

### 3.3 Sessies en tokens

| Control | Eis (kort, bron) | Scope | Gevolg voor het ontwerp |
|---|---|---|---|
| ASVS 9.2.3 (L2), 10.3.1 (L2) | Alleen tokens accepteren die voor deze service bedoeld zijn (`aud` tegen allowlist). | Voorwaarde | Per tenant een eigen audience (of realm/issuer) en de runtime accepteert een token alleen als `aud`/`iss` past bij de tenant van het verzoek. Zonder dat kan een token van gemeente A bij gemeente B gebruikt worden. |
| ASVS 9.2.4 (L2) | Gebruikt een issuer dezelfde sleutel voor meerdere audiences, dan bevat het token een unieke audience-beperking. | Voorwaarde | Relevant bij één gedeelde publieke Keycloak: als tenants hetzelfde realm (dezelfde signing key) delen, is een unieke `aud` per tenant verplicht. Eigen realm per tenant geeft ook een eigen sleutel. Input voor #205. |
| ASVS 9.1.3 (L1) | Sleutelmateriaal alleen uit vooraf geconfigureerde, vertrouwde bronnen per issuer. | Voorwaarde | JWKS-URL per tenant uit config, nooit afgeleid uit het token. |
| ASVS 10.3.3 (L2) | Gebruiker identificeren op niet-herbruikbare claims, meestal `iss` + `sub`. | Voorwaarde | Bij meerdere realms is `sub` alleen niet uniek. Sleutel = (`iss`, `sub`) of (OIN, BSN/KVK). |
| ASVS 9.2.2 (L2) | Tokentype en doel controleren. | Neutraal | Zie bestaand token-exchange-onderzoek. |
| ASVS 10.3.5 (L3) | Sender-constrained access tokens (mTLS of DPoP). | Neutraal | Vermindert de schade van een gestolen token, ook tussen tenants. |
| ASVS 7.1.3 (L2), 7.6.1 (L2) | Federatieve sessiesystemen en hun levensduur/beëindiging zijn gedocumenteerd en gedragen zich zo. | Voorwaarde | Per tenant (realm/client) sessie-instellingen vastleggen; gedeelde Keycloak betekent gedeelde defaults die per tenant afwijken. |
| ASVS 7.4.5 (L2) | Beheerders kunnen sessies van één gebruiker of van iedereen beëindigen. | Voorwaarde | "Iedereen" moet per tenant kunnen, zonder andere tenants te raken. |
| ASVS 8.3.2 (L3) | Wijzigingen in autorisatiegegevens direct toepassen, of mitigeren bij self-contained tokens. | Neutraal | Intrekken van een tenant (offboarding) moet direct werken, ook voor nog geldige tokens. Tenantconfig is dus onderdeel van elke tokencontrole. |
| ASVS 10.1.1 (L2) | Tokens alleen naar componenten die ze strikt nodig hebben. | Neutraal | Geen tokens van tenant A naar bronnen van tenant B; geen tokens in logs. |

### 3.4 Logging, monitoring en audit per tenant

| Control | Eis (kort, bron) | Scope | Gevolg voor het ontwerp |
|---|---|---|---|
| BIO 8.15.01 (ISO 8.15 Logging) | Een logregel bevat minimaal actie, object, resultaat, oorsprong, actor en tijdstempel. | Voorwaarde | In een gedeelde runtime is de tenant (OIN) onderdeel van "object" en "actor". Elke logregel en trace moet de OIN dragen (MDC/Reactor-context). |
| BIO 8.15.02 | Een logregel bevat nooit gegevens die tot het doorbreken van de beveiliging kunnen leiden. | Neutraal | Geen tokens, secrets of volledige foutresponses van Keycloak in logs. |
| BIO 8.15.03, ASVS 16.1.1 (L2) | Overzicht van logbestanden; inventaris van logging per laag, opslag, toegang en bewaartermijn. | Voorwaarde | Inventaris moet ook zeggen hoe logs per tenant scheidbaar en inzichtelijk zijn. |
| BIO 8.15.04, 5.28.01 | Bewaartermijn logs/SIEM risicogericht; incidentinformatie inclusief logging minimaal drie jaar. | Voorwaarde | Bewaartermijn verschilt mogelijk per gemeente; de strengste geldt voor de gedeelde logstroom, of per tenant splitsen. |
| BIO 8.15.05, 8.15.06; ASVS 16.4.2 (L2) | Ongeautoriseerd wijzigen/verwijderen van logs melden als incident; periodieke integriteitstoets door een onafhankelijke functionaris; logs beschermd tegen ongeautoriseerde toegang en wijziging. | Voorwaarde | Loginzage per tenant beperken (zie ook BIO 8.03.02). Een gemeente mag niet de logs van een andere gemeente zien. |
| ASVS 16.2.1 (L2) | Elke logregel met metadata (wanneer, waar, wie, wat) voor onderzoek. | Voorwaarde | Zelfde als 8.15.01: tenant verplicht veld. |
| ASVS 16.3.2 (L2; L3 extra) | Mislukte autorisaties loggen. Voor L3: **alle** autorisatiebeslissingen loggen, ook inzage van gevoelige gegevens (zonder die gegevens zelf). | Voorwaarde | Elke opvraging van BRP/HR/zaakgegevens wordt gelogd met tenant, actor en object. Dat is de audittrail die een gemeente per tenant wil zien. Cross-tenant weigeringen (8.4.1) zijn een security-event. |
| ASVS 16.2.5 (L2), 14.1.2 (L2) | Loggen volgens beschermingsniveau van de gegevens. | Voorwaarde | BSN niet in plaintext in logs; maskeren of pseudonimiseren. |
| ASVS 16.4.3 (L2) | Logs naar een logisch gescheiden systeem. | Neutraal | Centrale logstack buiten de portal-namespace. |
| BIO 8.16.03, 8.16.02 | Detectie- en response-oplossing; regels voor rapportage aan management. | Voorwaarde | Detectie moet per tenant kunnen rapporteren, en een incident dat de runtime raakt aan alle betrokken gemeenten. |

### 3.5 Change management van configuratie en omgevingen

| Control | Eis (kort, bron) | Scope | Gevolg voor het ontwerp |
|---|---|---|---|
| BIO 8.32.01 (ISO 8.32 Change management) | Wijzigingsbeheer met administratie en testresultaten, risicoafweging inclusief rollback-plan, goedkeuringsprocedure. | Voorwaarde | Git/ArgoCD-PR's zijn de administratie. Elke PR op gedeelde runtimeconfig raakt alle tenants: risicoafweging en rollback-plan per wijziging, goedkeuring door een tweede persoon. |
| BIO 8.32.02 | Wijzigingsbeheer op basis van een algemeen geaccepteerd beheerraamwerk. | Neutraal | Pink-beheerproces (bijv. ITIL/BiSL) benoemen. |
| BIO 8.31.01, 8.31.02 (ISO 8.31 Separation of development, test and production environments) | In productie wordt niet getest; significante wijzigingen eerst testen. Afwijken alleen met voorafgaande goedkeuring proceseigenaar. | Voorwaarde | Canary in productie is geen test in productie mits na acceptatie. Lokale login alleen in acc (#197) past hierbij. Wie is "proceseigenaar" bij een gedeelde runtime? Leg vast dat Pink dat is voor de runtime en de gemeente voor haar tenant. |
| BIO 8.29.01 (ISO 8.29 Security testing) | Gestructureerde, waar mogelijk geautomatiseerde acceptatietesten met verslag. | Voorwaarde | Cross-tenant-isolatietests in de pipeline, met rapport als bewijs. |
| BIO 8.27.01 (ISO 8.27 Secure system architecture) | Architectuurprincipes zoals security by design/default vastgesteld, gedocumenteerd en toegepast. | Voorwaarde | ADR + architectuurchecks (`backend/architecture-test`) zijn de vastlegging en de "werking". |
| BIO 8.08.05 | Internetfacing systemen: verplichte, waar mogelijk geautomatiseerde pentest bij iedere nieuwe release of major update; bevindingen met hoog risico die niet anders te mitigeren zijn blokkeren productie; minimaal jaarlijks testen. | Voorwaarde | Eén release raakt ~50 portalen. Pentest moet cross-tenant-scenario's bevatten. |
| BIO 8.08.04 | Jaarlijkse technische nalevingscontrole; internetfacing waar mogelijk continu testen. | Neutraal | DAST/scan per tenantdomein. |
| BIO 5.08.01 | Expliciete risicoafweging bij nieuwe systemen en significante wijzigingen. | Voorwaarde | Onboarding van een nieuwe tenant op de gedeelde runtime is mogelijk een significante wijziging voor de bestaande tenants. |
| ASVS 13.4.5 (L2), 13.4.2 (L2) | Monitoring-/documentatie-endpoints niet blootstellen; debugmodi uit in productie. | Neutraal | `management.endpoint.refresh/restart` staan op `access: unrestricted` in `backend/app/src/main/resources/config/application.yml`. In een gedeelde runtime kan dat alle tenants raken. Valt onder "ASVS-bevindingen buiten tenancy" uit #197; apart beleggen. |
| BIO 8.20.02; ASVS 8.4.2 (L3) | Beheerinterfaces zo veel mogelijk gescheiden van het gebruikersnetwerk; beheerinterfaces met meerdere lagen beveiliging. | Voorwaarde (al in opzet) | Past bij "private broker nooit in de portal-runtime" (#197). |

### 3.6 Beschikbaarheid in een gedeelde runtime (noisy neighbour)

| Control | Eis (kort, bron) | Scope | Gevolg voor het ontwerp |
|---|---|---|---|
| ASVS 2.4.1 (L2) | Anti-automatisering tegen overmatige calls (datalek, quotumuitputting, DoS). | Voorwaarde | Rate limits per tenant en per gebruiker; anders put één tenant de quota van Haal Centraal/ZGW voor anderen uit. |
| ASVS 15.1.3 (L2), 15.2.2 (L2) | Zware functionaliteit documenteren en tegen beschikbaarheidsverlies beschermen. | Voorwaarde | Zie "Rate limiting per tenant" in #197. |
| ASVS 13.1.2 (L3), 13.1.3 (L3), 13.2.6 (L3) | Per externe service: max. connecties, gedrag bij limiet, timeouts, retries gedocumenteerd en toegepast. | Voorwaarde | Connectiepools per tenant per bron, zodat een trage ZGW van tenant A de pool van tenant B niet blokkeert. |
| ASVS 16.5.2 (L2) | Veilig blijven werken bij uitval van externe bronnen (circuit breakers, graceful degradation). | Voorwaarde | Circuit breaker per tenant + bron, niet globaal per bron. |
| ASVS 15.4.4 (L3) | Resource-allocatie voorkomt thread starvation. | Neutraal | WebFlux: blokkerende calls niet op de event loop; per tenant begrensd. |

### 3.7 Leveranciers en cloud (gezien vanuit de gemeente)

| Control | Eis (kort, bron) | Scope | Gevolg voor het ontwerp |
|---|---|---|---|
| BIO 5.19.01, 5.20.01 | Informatiebeveiligingseisen (BIV) in offerteaanvraag en contract, op basis van een expliciete risicoafweging. | Voorwaarde | Elke gemeente kan eigen eisen stellen. Pink heeft één standaardset nodig die de gedeelde runtime haalt. |
| BIO 5.20.03 | Leverancier toont jaarlijks aan dat hij aan alle eisen voldoet in opzet, bestaan en werking, op basis van onderzoek door onafhankelijke derden, met dekkende scope. | Voorwaarde | Eén assurancerapport (bijv. ISO 27001-certificaat met passende scope of een ISAE 3402/3000-rapport; vorm niet voorgeschreven in BIO2) voor de gedeelde runtime. |
| BIO 5.20.04 | De entiteit kan zelf onderzoek doen; externe audit contractueel mogelijk. | Voorwaarde | Bij ~50 tenants: een audit door gemeente A mag geen gegevens van gemeente B ontsluiten. Plan een gezamenlijke audit of een "pooled audit" en scheid auditbewijs per tenant. |
| BIO 5.20.05, 5.24.07 | Leverancier transparant over kwetsbaarheden en incidenten (datalekken); incidentprocedure met wettelijke meldtermijnen. | Voorwaarde | Een incident in de gedeelde runtime is een incident voor alle tenants. Per tenant kunnen bepalen of hij geraakt is (vereist tenant in logging, 3.4). |
| BIO 5.20.06 | Exit-strategie in het contract. | Voorwaarde | Zonder database is exit vooral config en sleutels per tenant; leg vast hoe een tenant de runtime verlaat. |
| BIO 5.21.02 t/m 5.21.05 | Inzicht in de keten van toeleveranciers, eisen doorgeven, wijzigingen melden. | Neutraal | Hosting, Keycloak, logstack zijn toeleveranciers in de keten. |
| BIO 5.23.01 (ISO 5.23 Cloud services) | Beleid voor inventariseren, classificeren, selecteren, beoordelen en beëindigen van cloud service providers. | Voorwaarde | Gemeenten zullen Pink als CSP behandelen. |
| BIO 5.33.01 | Bewaartermijnen per informatiesysteem vastgelegd en ingeregeld. | Neutraal | Geen database, dus vooral logs (zie 3.4). Buiten de Cbw-regeling (art. 5 sluit 5.32.01–5.34.01 uit), wel verplichtende zelfregulering. |

## 4. Wat sluit een gedeelde runtime uit?

Niets in de gelezen teksten. Specifiek:

- BIO2 noemt "fysiek en/of logisch isoleren" (8.03.01) en laat de vorm aan de risicoanalyse van de entiteit (deel 1 §6.5 en deel 2, inleiding).
- ASVS 5.0.0 behandelt multi-tenancy alleen in 8.4.1 en eist daar cross-tenant controls, geen procesisolatie. De isolatietechnieken in 15.2.5 (L3: "sandboxing, encapsulation, containerization or network level isolation") gaan over "dangerous functionality" en "risky components", niet over tenants.
- BIO 7.01.02 ("Kritieke informatie of informatiesystemen zijn nooit via één beveiligde zone te bereiken") hoort bij de fysieke beveiliging (ISO-thema 7) en gaat over gelaagdheid, niet over tenants.

**Wel feitelijke grenzen van de gedeelde runtime:**

1. **Hoogste gemene deler.** Door 8.22.01 en 5.12.01 bepaalt de strengste gemeente het niveau. Een gemeente die op basis van haar risicoanalyse fysieke scheiding eist, kan niet op de gedeelde runtime. Houd instance-per-tenant of een cel als uitwijkoptie beschikbaar in de ADR.
2. **Secrets in één proces.** Een gedeelde pod heeft per definitie de secrets van alle tenants in geheugen. Dat schuurt met least privilege (ASVS 13.3.2) en met de bedoeling van 11.1.1. Het is geen verbod, maar wel een restrisico dat in de VvT/ADR moet staan. Mitigatie: HSM-backed store (13.3.1 L3), korte levensduur, crypto-operaties in de vault (13.3.3 L3).
3. **Elke wijziging raakt iedereen.** Door 8.32.01 en 8.08.05 geldt elke release en elke gedeelde-configwijziging als wijziging voor alle tenants (pentest, risicoafweging, rollback-plan).

## 5. Wat verwachten auditors als aantoonbaarheid?

| Bewijs | Bron | Concreet voor NL Portal |
|---|---|---|
| Opzet, bestaan en werking per maatregel | BIO2 deel 1 §4 ("via self-assessments, audits, pentesten, redteam-testen"); Cyberbeveiligingsregeling sector overheid art. 5 lid 3 | Opzet = ADR + architectuurdocumentatie. Bestaan = code en config (core-seam, ESO, Keycloak). Werking = tests en monitoring. |
| VvT met overheidsmaatregelen en uitzonderingen | BIO2 deel 1 §7 | De gemeente stelt de VvT op; Pink levert per maatregel een verwijzing naar het eigen bewijs. Afwijkingen (bijv. 13.3.3) met risicoanalyse. |
| Jaarlijks onafhankelijk onderzoek met dekkende scope | BIO 5.20.03, 5.22.01 | Assurancerapport over de gedeelde runtime inclusief tenantscheiding. |
| Pentest per release, blokkade bij hoog risico; jaarlijks testen | BIO 8.08.05, 8.08.04 | Pentestscope moet cross-tenant-scenario's bevatten (token van tenant A op domein B, ID-enumeratie over tenants, header-spoofing). |
| Gestructureerde, geautomatiseerde acceptatietesten met verslag | BIO 8.29.01 | Cross-tenant-integratietests per release, rapport bewaren. |
| ASVS-verificatierapport met alle eisen, scope en niet-van-toepassing | ASVS 5.0.0 *Assessment and Certification*, "Verification reporting" en "Scope of Verification" | Rapport per eis in de vorm van de [token-exchange-toetsing](./asvs-5-l3-toetsing-token-exchange.md), uitgebreid met 8.4.1. |
| Hybride (bron- en documentatiegestuurde) pentest; applicatiespecifieke geautomatiseerde tests voor autorisatie | ASVS 5.0.0 *Assessment and Certification* ("documentation or source code-led (hybrid) penetration testing"; "testable using automation != running an off the shelf tool") | Architectuurchecks (ArchUnit/Konsist) die afdwingen dat uitgaande clients alleen via tenantgebonden configuratie gebouwd worden, plus tests op 8.4.1. |
| Logintegriteit periodiek getoetst door een onafhankelijke functionaris | BIO 8.15.06 | Proces bij Pink-beheer, niet bij het ontwikkelteam. |

## 6. Gevolgen voor de vervolgtickets

- **#203 (secrets/ESO):** HSM-backed secret store voor L3 (13.3.1); per tenant eigen secrets en clients (11.1.1); rotatieschema (13.3.4) passend bij "altijd rolling restart"; besluit over 13.3.3 (crypto in vault of afwijking).
- **#205 (IdP/tokens):** tenantbinding via `iss`/`aud` (9.2.3, 9.2.4, 10.3.1); gebruikerssleutel `iss`+`sub` (10.3.3); sessiedocumentatie per tenant (7.1.3, 7.6.1). Eén realm voor meerdere tenants vereist een unieke `aud` per tenant.
- **Core-seam en architectuurchecks:** tenant verplicht in elke autorisatiebeslissing en elke uitgaande client (8.4.1, 8.03.01); tenant in Reactor-context, niet thread-local (15.4.1); caches met tenant in de sleutel (14.2.2).
- **Observability (nog niet gespecificeerd in #197):** OIN in elke logregel en trace (8.15.01, 16.2.1); alle autorisatiebeslissingen en inzage loggen (16.3.2 L3); loginzage per tenant (8.03.02, 16.4.2); bewaartermijn minimaal drie jaar voor incidentinformatie (5.28.01).
- **Rate limiting (nog niet gespecificeerd):** per tenant (2.4.1), connectiepools en circuit breakers per tenant + bron (13.1.2, 13.2.6, 16.5.2).
- **Losse ASVS-bevinding:** `/actuator/refresh` en `/actuator/restart` op `unrestricted` (13.4.5, 13.4.2).

## 7. Niet geverifieerd en open punten

- **Grijze/vette markering** in de BIO2-PDF (welke overheidsmaatregelen buiten de Cbw vallen) is niet gezien; de PDF op bio-overheid.nl gaf HTTP 403 en de Staatscourant-HTML toont de opmaak niet.
- **ISO/IEC 27002:2022** (titels en implementatierichtlijnen) en **ISO/IEC 27017** (cloud, met o.a. een richtlijn over scheiding in virtuele omgevingen) zijn niet gelezen. BIO2 verwijst naar 27002 voor beheersmaatregelen zonder overheidsmaatregel; 27017 is door BIO2 niet verplicht gesteld.
- **Of Pink zelf onder de Cbw valt** (bijv. als aanbieder van beheerde diensten of cloudcomputingdiensten) is niet onderzocht. Dat bepaalt of Pink eigen zorgplicht- en meldplichten heeft naast de contractuele eisen van gemeenten.
- **Welke gemeenten essentiële entiteit zijn** onder de Cbw is niet uitgezocht; de Cyberbeveiligingsregeling sector overheid geldt voor essentiële entiteiten in de sector overheid (bijlage 1 bij de wet, niet gelezen). Voor de rest blijft BIO2 verplichtende zelfregulering, dus de eisen zijn in de praktijk dezelfde.
- **ASVS-niveaus** zijn overgenomen uit de eisentabellen van tag `v5.0.0_release`. Waar een eis een extra L3-zin bevat (13.3.1, 16.3.2), staat het basisniveau erbij met "L3 extra".
