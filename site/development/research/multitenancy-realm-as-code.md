# Realm-as-code via ArgoCD: keycloak-config-cli, Terraform, Crossplane of de Keycloak Operator

Geraadpleegd op: 2026-11-06. Onderzoek voor #208 (kaart #197), gebruikt door #205.

Uitgangspunt is het advies uit #199 ([multitenancy-keycloak-realms.md](https://github.com/jypma-prlg/nl-portal/blob/research/multitenancy-keycloak/site/development/research/multitenancy-keycloak-realms.md), branch `research/multitenancy-keycloak`): **één realm per tenant** op de publieke ID-broker, ~50 tenants, gegenereerd uit één template in Git en uitgerold via ArgoCD.

Peilversies (laatste releases zoals gezien via `gh api repos/<repo>/releases`):

| Component | Laatste release | Datum |
|---|---|---|
| Keycloak | 26.8.2 | 2026-11-05 |
| keycloak-config-cli (adorsys) | v6.5.1 | 2026-05-22 (main: `6.5.2-SNAPSHOT`, gebouwd tegen Keycloak 26.7.4) |
| terraform-provider-keycloak | v5.10.0 | 2026-10-05 (plus een `nightly`) |
| crossplane-contrib/provider-keycloak | v3.2.3 | 2026-10-19 |
| Crossplane | v2.4.3 | 2026-10-22 |
| Argo CD | v3.6.1 | 2026-11-04 |
| Terraform / OpenTofu | v1.17.1 / v1.13.9 | 2026-10-28 / 2026-11-05 |
| tofu-controller (Flux) | v0.16.6 | 2026-10-26 |

## Samenvatting

- **De Keycloak Operator (`KeycloakRealmImport`) valt af.** De CR "only supports creation of new realms and does not update or delete those". Een bestaand realm wordt niet overschreven. Voor beheer vanuit Git is dat onvoldoende; hooguit bruikbaar voor een eenmalige bootstrap.
- **Advies: keycloak-config-cli (kcc) als ArgoCD-hook-Job, één Job per tenant-realm**, met het realm-template als Helm-chart en de tenantparameters (OIN, gemeentedomein, IdP-variant) als values per tenant. kcc gebruikt het realm-exportformaat dat we al hebben (`docker-compose/imports/keycloak/nlportal-realm.json`), voegt geen nieuw platformcomponent toe en kan resources die het zelf heeft aangemaakt ook weer verwijderen (*remote state*).
- **Voorwaarden bij dat advies:** zet `import.cache.enabled=false` (anders slaat kcc een ongewijzigd bestand over en herstelt het geen drift), leg de Keycloak-minor vast op een versie die kcc in CI test, en draai elke Keycloak-upgrade eerst in dev met dezelfde Jobs. **kcc loopt achter:** de laatste release (6.5.1, mei 2026) is getest tot Keycloak 26.5/26.6; 26.7.4 staat alleen op `main`; **26.8 staat nog niet in de CI-matrix**. Dat is het grootste risico van deze keuze.
- **Crossplane provider-keycloak is het serieuze alternatief.** Het past het best bij ArgoCD: elk Keycloak-object is een Kubernetes-resource met health, continue drift-correctie (standaard elke 10 minuten) en echte verwijdering. Het is gegenereerd uit de Terraform-provider en volgt Keycloak dus actueel (26.8-fix in v3.2.2). De prijs: een Crossplane-control-plane erbij, ~1.000–2.000 managed resources bij 50 tenants, `ignoreDifferences` in ArgoCD en een klein project (74 sterren). Kies dit als het platformteam Crossplane al draait of wil draaien.
- **Terraform (de officiële provider, nu onder `github.com/keycloak`) is functioneel het rijkst en het meest actueel** (ondersteunt 26.8.0, write-only secrets), maar past slecht bij ArgoCD: er is een state-backend nodig en een runner (Job of tofu-controller van Flux). Alleen kiezen als Pink al een Terraform-straat heeft.
- **Least privilege:** gebruik nooit de master-`admin` (die token draagt de rollen van álle realms, keycloak/keycloak#26883, nog open). Scheid een **bootstrap-identiteit** met alleen `create-realm` in master (alleen bij onboarding) van een **service-account per tenant-realm** met beperkte `realm-management`-rollen voor het reguliere bijwerken. Vanaf Keycloak 26.6 kan dat service-account inloggen met een Kubernetes-service-account-token (*Signed JWT – Federated*), zonder statisch secret. In kcc zit dat alleen nog op `main` (niet in 6.5.1).
- **Secrets:** alle drie halen secrets uit Kubernetes Secrets die ESO aanmaakt; geen secrets in Git. Let op: kcc bewaart zijn remote state als realm-attribuut, standaard **onversleuteld** (zet `import.remote-state.encryption-key`); Terraform-state bevat secrets tenzij je write-only-argumenten gebruikt.
- **Verwijderen van een hele tenant-realm** doet geen van de opties vanzelf goed. kcc verwijdert nooit realms; Crossplane en Terraform wel, maar dan zijn alle gebruikers en sessies van die gemeente direct weg. Maak offboarding een expliciete, handmatige stap.

## 1. Bronnen

| Bron | Link | Opmerking |
|---|---|---|
| kcc README | https://github.com/adorsys/keycloak-config-cli (README) | Volledig gelezen: opties, variabelen, compatibiliteit, Docker-tags, Signed JWT |
| kcc `docs/MANAGED.md` | https://github.com/adorsys/keycloak-config-cli/blob/main/docs/MANAGED.md | Volledig gelezen: remote state, full/no-delete |
| kcc `docs/SKIP_SERVER_INFO.md`, `docs/IMPORT.md`, `docs/FEATURES.md` | idem, map `docs/` | Gelezen |
| kcc `CHANGELOG.md`, `pom.xml`, `.github/workflows/ci.yaml` | idem | Unreleased + 6.5.1 + 6.5.0 gelezen; CI-matrix Keycloak-versies |
| kcc `RealmImportService.java` | `src/main/java/de/adorsys/keycloak/config/service/RealmImportService.java` | Regel 172: import wordt overgeslagen als cache aan staat en checksum gelijk is |
| kcc open issues | https://github.com/adorsys/keycloak-config-cli/issues | Alleen titels: o.a. #1747 (first broker login flow), #1682 (groups full) |
| Terraform-provider README en `docs/index.md` | https://github.com/keycloak/terraform-provider-keycloak | Ondersteunde Keycloak-versies, authenticatie, rollen |
| Terraform-provider `docs/resources/*` | idem | Lijst resources; `saml_identity_provider`, `oidc_identity_provider` (`provider_id`, `extra_config`) |
| Terraform-provider releases | `gh api repos/keycloak/terraform-provider-keycloak/releases` | v5.10.0 en v5.9.0 release notes gelezen |
| provider-keycloak README | https://github.com/crossplane-contrib/provider-keycloak | Installatie, flags, ProviderConfig |
| provider-keycloak `docs/argo-cd-ignore-differences.md` | idem | Drift door write-back van referenties |
| provider-keycloak releases | `gh api .../releases` | v3.1.1 t/m v3.2.3 gelezen |
| Keycloak Operator realm import | https://github.com/keycloak/keycloak/blob/26.8.2/docs/guides/operator/realm-import.adoc | Volledig gelezen |
| Keycloak admin guide: master realm, Admin API-audience | `docs/documentation/server_admin/topics/admin-console-permissions/master-realm.adoc` en `admin-api-audience.adoc` (tag 26.8.2) | Gelezen |
| keycloak/keycloak#26883 | https://github.com/keycloak/keycloak/issues/26883 | "Returning a token for a master admin account does not scale when there are many realms", open |
| Argo CD sync-phases en hooks | https://github.com/argoproj/argo-cd/blob/master/docs/user-guide/sync-waves.md | Hooks, delete-policies, PreDelete |
| Argo CD health-customizations | https://github.com/argoproj/argo-cd/tree/master/resource_customizations | Mappen `_.crossplane.io`, `_.upbound.io`, `k8s.keycloak.org` (alleen `Keycloak`) |

Niet gelezen: de websites registry.terraform.io, marketplace.upbound.io en keycloak.org zelf; de bijbehorende bronbestanden in GitHub wel.

## 2. De vier opties in het kort

### 2.1 keycloak-config-cli (adorsys)

Een Spring Boot-applicatie die een of meer realm-bestanden (JSON/YAML in het Keycloak-exportformaat) via de Admin REST API op Keycloak zet. Draait als container of jar, typisch als Kubernetes-Job. Er is een Helm-chart in `contrib/charts/keycloak-config-cli`.

- **Bijwerken:** idempotent; "A Keycloak restart isn't required to apply the configuration."
- **Verwijderen:** met `import.remote-state.enabled=true` (standaard) verwijdert kcc alleen resources die het zelf eerder heeft aangemaakt; de administratie daarvan staat als **realm-attribuut** in de Keycloak-database. Voor een reeks typen (clients, client scopes, rollen, groepen, identity providers en hun mappers, authenticatieflows, components, required actions, workflows …) geldt *full managed*: definieer je het type in het bestand, dan verwijdert kcc wat er niet in staat. Per type uit te zetten met `import.managed.<type>=no-delete` (MANAGED.md).
- **Drift:** kcc slaat een import over als de checksum van het bestand gelijk is aan de vorige run, tenzij `import.cache.enabled=false` (`RealmImportService.java`, regel 172). Met cache aan herstelt kcc dus **geen** handmatige wijziging in de beheerconsole. Er is geen *plan*/*dry-run*; je ziet pas achteraf (in de log) wat er veranderde.
- **Realms verwijderen:** in README, FEATURES.md en MANAGED.md staat geen functie om een realm te verwijderen; de importservice bevat er geen. Een tenant uit Git halen laat het realm dus staan.
- **Templating:** variabele-substitutie (`$(env:NAAM)`, `$(file:UTF-8:/pad)`, en meer) staat standaard **uit** (`import.var-substitution.enabled`). Daarnaast optioneel JavaScript-evaluatie (`script-evaluation-enabled`), en lookups als `$(url:…)` en `$(dns:…)`. Substitutie gebeurt lokaal in de Job, dus de variabelen moeten in de Job-omgeving staan.
- **Compatibiliteit:** "keycloak-config-cli will support the latest 4 releases of keycloak, if possible" en "will cut the support if keycloak introduces some breaking changes". Docker-tags `a.b.c-x.y.z` zijn gebouwd tegen een specifieke Keycloak-versie. CI-matrix op `main`: 21.1.2 t/m 26.7.4; **26.8 ontbreekt**. Release 6.5.1 voegde baselines toe tot 26.5.7; de ondersteuning voor 26.7.4 staat onder *Unreleased*.
- **Authenticatie:** wachtwoord, client secret, mTLS (6.5.1), vooraf verkregen token, en op `main` (unreleased) Signed JWT en *Signed JWT – Federated* met een Kubernetes-service-account-token. Inloggen in een ander realm dan master kan met `keycloak.login-realm`, maar dan is `keycloak.skip-server-info=true` nodig, omdat `/admin/serverinfo` sinds 26.4 alleen voor master-admins is; geef dan ook `keycloak.version` mee (SKIP_SERVER_INFO.md).
- **Onderhoud:** 1.215 sterren, 54 open issues, laatste push 2026-11-02, Apache-2.0. Releases: 6.4.0 (2025-02), 6.4.1 (2026-01), 6.5.0 (2026-03), 6.5.1 (2026-05). Dus enkele releases per jaar, met een gat van bijna een jaar in 2025.

### 2.2 Terraform-provider voor Keycloak

De provider staat nu onder `github.com/keycloak/terraform-provider-keycloak` (957 sterren, 374 open issues, laatste push 2026-11-04). Ruim 110 resource-typen, inclusief `saml_identity_provider` en `oidc_identity_provider` met `provider_id` en `extra_config` voor eigen IdP-implementaties, `realm_keystore_*`, `realm_events`, organizations, workflows en FGAP v2-permissies.

- **Bijwerken/verwijderen/drift:** standaard Terraform: `plan` toont het verschil met de werkelijke staat, `apply` herstelt, en een resource die uit de config verdwijnt wordt verwijderd. Ook een `keycloak_realm` verdwijnt dan, met alle gebruikers.
- **Templating:** `for_each` over een map van tenants of een module per tenant; los te zetten in een state per tenant zodat één plan niet 50 realms raakt.
- **Compatibiliteit:** "officially support the latest three minor versions of Keycloak"; README noemt 26.8.0 (latest) tot 26.0.8. v5.9.0 liet Keycloak < 26 vallen en tolereert sinds dan een lege serverversie op 26.4+. v5.10.0 (2026-10-05) "Update to Keycloak 26.8.0 and 26.7.5".
- **Secrets:** write-only-argumenten voor o.a. het initiële gebruikerswachtwoord, de LDAP-bind-credential en `realm_keystore_rsa` (v5.10.0); andere gevoelige velden belanden in de state. De state moet dus versleuteld bewaard worden.
- **Authenticatie:** client secret, wachtwoord, Signed JWT (ook `jwt_token_file`, bruikbaar met een geprojecteerd Kubernetes-service-account-token), vooraf verkregen token, mTLS.
- **ArgoCD:** geen eigen integratie. Opties: een Job die `tofu apply` draait met een `kubernetes`-state-backend, de tofu-controller (Flux, v0.16.6), of Crossplane `provider-terraform`. ArgoCD ziet dan alleen de Job of de CR, niet het plan.

### 2.3 Crossplane provider-keycloak

Gegenereerd met Upjet uit de Terraform-provider (v3.2.0 bump naar terraform-provider-keycloak v5.10.0). Elk Keycloak-object wordt een managed resource (CRD) in Kubernetes, bijvoorbeeld `Realm`, `Client` in groep `openidclient.keycloak.crossplane.io`.

- **Bijwerken/verwijderen/drift:** continue reconciliatie; flag `--poll` (standaard `10m`) bepaalt hoe vaak elke resource op drift wordt gecontroleerd. Verwijderen van de CR verwijdert het object in Keycloak (Crossplane `deletionPolicy`, standaard `Delete`; niet nagelezen in deze repo).
- **Templating:** Helm/Kustomize per tenant, of een Crossplane-*Composition* die één `Tenant`-resource uitvouwt tot realm, clients, IdP's en mappers.
- **Schaal:** per realm tientallen objecten (clients, scopes, mappers, flows, executions, IdP-mappers). Bij 50 tenants ruwweg 1.000–2.000 managed resources (schatting, niet gemeten). De provider waarschuwt dat de onderliggende Terraform-client niet thread-safe is en gebruikt een pool (`--keycloak-client-pool-size`, standaard 5).
- **ArgoCD:** Argo CD heeft health-checks voor groepen onder `_.crossplane.io` en `_.upbound.io`. Of die wildcard ook `*.keycloak.crossplane.io` dekt, is **niet geverifieerd** in de Lua-code. De provider documenteert dat ArgoCD drift ziet door write-back van opgeloste referenties en raadt `ignoreDifferences` aan (`docs/argo-cd-ignore-differences.md`).
- **Compatibiliteit:** volgt de Terraform-provider; v3.2.2 (2026-10-15) "Handle Keycloak 26.8's 403 on client-secret reads for internal admin clients".
- **Onderhoud:** 74 sterren, 37 open issues, vier releases in oktober 2026, Renovate-bumps. Actief, maar een klein project in `crossplane-contrib`.

### 2.4 Keycloak Operator: `KeycloakRealmImport`

Uit `realm-import.adoc` (26.8.2):

- "If a Realm with the same name already exists in Keycloak, it will not be overwritten."
- "The Realm Import CR only supports creation of new realms and does not update or delete those."
- "Once the realm is imported you should delete the Realm Import CR" (het opruimen van Job en Pod).
- Placeholders uit Secrets via `spec.placeholders`, alleen in dezelfde namespace als de Keycloak-CR. Waarschuwing: "Anyone with the ability to create or edit KeycloakRealmImport CRs should be a namespace level admin" en placeholder-vervanging "gives access to all environment variables even sensitive ones".
- Argo CD heeft een health-check voor `k8s.keycloak.org/Keycloak`, niet voor `KeycloakRealmImport`.

Conclusie: geschikt voor een eenmalige aanmaak, niet voor beheer vanuit Git. Hetzelfde geldt voor `kc.sh import` bij opstarten.

## 3. Vergelijking

| Criterium | keycloak-config-cli | Terraform-provider | Crossplane provider-keycloak | Operator `KeycloakRealmImport` |
|---|---|---|---|---|
| **Bijwerken bestaand realm** | Ja, idempotent | Ja | Ja | **Nee** |
| **Resources verwijderen** | Ja, alleen eigen resources (remote state), per type instelbaar | Ja (state) | Ja (CR verwijderen) | Nee |
| **Realm zelf verwijderen** | Nee | Ja (riskant) | Ja (riskant) | Nee |
| **Drift (wijziging via console)** | Alleen hersteld als `import.cache.enabled=false` en de Job opnieuw draait | Zichtbaar in `plan`, hersteld bij `apply` | Continu, elke `--poll` (10 min) | Nee |
| **Diff vooraf zichtbaar** | Nee (geen plan) | Ja (`plan`) | Deels: ArgoCD toont de CR-diff | Nee |
| **Remote state / opslag** | Realm-attribuut in Keycloak, standaard onversleuteld | Externe state-backend (bevat secrets) | Kubernetes (CR-status) | n.v.t. |
| **Templating per tenant** | Helm rond het realm-bestand; runtime-substitutie voor secrets | `for_each`/modules | Helm/Kustomize of Composition | Helm rond de CR |
| **Formaat** | Keycloak realm-export (zoals de demo-import) | HCL, eigen resourcemodel | YAML-CR's, resourcemodel van Terraform | Realm-export |
| **Eigen DigiD-plugin / Signicat** | Elke `providerId` in de realm-representatie | `provider_id` + `extra_config` | Idem via Upjet | Elke `providerId` |
| **Secrets (ESO)** | Secret → env/bestand in de Job, `$(env:…)`/`$(file:…)` | Secret → `TF_VAR_`/bestand; write-only-argumenten deels | `*SecretRef` naar Kubernetes Secret (niet per veld nagelezen) | `spec.placeholders` → Secret |
| **ArgoCD-integratie** | Job als `Sync`/`PostSync`-hook; Job-health ingebouwd | Job of tofu-controller; geen native diff | Native CR's; health via Crossplane-customization; `ignoreDifferences` nodig | CR zonder health-check |
| **Keycloak 26.x-upgrades** | Laatste 4 releases "if possible"; **26.8 nog niet in CI**; release loopt maanden achter | Laatste 3 minors; 26.8.0 ondersteund (2026-10) | Volgt Terraform; 26.8-fix (2026-10) | Altijd gelijk (deel van Keycloak) |
| **Least privilege** | Per-realm login + `skip-server-info`; federated JWT pas op `main` | Per-realm service-account; Signed JWT uit bestand | ProviderConfig per realm ("scoping to a single realm") | Draait met de rechten van de operator |
| **Audit** | Git + ArgoCD-sync + Keycloak admin events; JSON-logging | Git + plan/apply-log + admin events | Git + ArgoCD + Kubernetes-events + admin events | Git + admin events |
| **Extra infrastructuur** | Geen (Job-image) | State-backend + runner | Crossplane-control-plane + provider | Keycloak Operator |
| **Volwassenheid** | 1,2k sterren, 4 releases in 21 maanden | Officieel Keycloak-org, maandelijkse releases | 74 sterren, zeer actief | Onderdeel van Keycloak |

## 4. Uitwerking per criterium

### 4.1 Templating van één realm per tenant

Het demo-realm `docker-compose/imports/keycloak/nlportal-realm.json` is in het exportformaat en bevat nu nog geen eigen IdP's. Voor kcc en de Operator is dat direct het startpunt; voor Terraform en Crossplane moet het opnieuw gemodelleerd worden.

Aanpak met kcc:

1. Een Helm-chart `keycloak-tenant-realm` met het realm-bestand als template. Values per tenant: `oin`, `realmNaam` (bijv. de OIN), `domein` (Frontend URL), `thema`, `idp.type` (`digid-plugin` | `signicat`) en de IdP-metadata-URL's.
2. Een ArgoCD `ApplicationSet` met een git-file-generator over `tenants/<oin>.yaml`. Elke tenant is een eigen Application met een eigen Job; een fout in één realm blokkeert de andere niet.
3. **Niet-geheime waarden via Helm, geheime via kcc-substitutie** (`$(file:UTF-8:/secrets/…)`), zodat secrets nooit in een gerenderd manifest of ConfigMap staan. Laat `script-evaluation-enabled` uit en gebruik geen `$(url:…)`/`$(dns:…)`; die vergroten het aanvalsoppervlak.

Let bij kcc op *full managed*: zodra het template `identityProviders`, `clients` of `authenticationFlows` bevat, verwijdert kcc alle andere objecten van dat type die het zelf heeft aangemaakt. Voor authenticatieflows en components moet je de Keycloak-standaarden in het bestand overnemen (MANAGED.md). Open issue #1747 meldt een fout bij het opnieuw aanmaken van een flow die als `firstBrokerLoginFlow` aan het realm hangt; relevant voor IdP-brokering (alleen titel gelezen).

### 4.2 Secrets via External Secrets Operator

- ESO maakt per tenant een Secret (DigiD/Signicat-clientgegevens, eventueel sleutelmateriaal). Die mount je in de kcc-Job als bestand.
- kcc bewaart zijn remote state als realm-attribuut, "If unset, state will be stored in plain" (`import.remote-state.encryption-key`). Zet die sleutel, ook via ESO.
- `logging.level.realm-config=trace` logt "the realm config including **sensitive information**"; nooit in acc/prod. Sinds 6.5.0 worden secrets in HTTP-debuglogs gemaskeerd.
- **Realmsleutels** (de DigiD-ondertekeningssleutel uit #199): beheer ze als `rsa`-keystore-component met de private key uit ESO, of laat Keycloak ze genereren en exporteer alleen het certificaat voor de DigiD-metadata. Welke route bij PKIoverheid past, valt buiten dit onderzoek.

### 4.3 Integratie met ArgoCD

- **Job als hook:** annotatie `argocd.argoproj.io/hook: PostSync` (na ESO-Secrets en eventuele Keycloak-rollout) met `hook-delete-policy: BeforeHookCreation` (standaard). Argo CD leest de Job om het resultaat te bepalen; gebruik geen `ttlSecondsAfterFinished` op hook-Jobs. Een mislukte Job maakt de sync rood.
- **Kanttekening:** "Hooks do not run during a selective sync operation." En ArgoCD toont alleen de diff van het template, niet die van Keycloak.
- **Drift:** een hook draait alleen bij een sync. Voor periodiek herstel kan een CronJob met dezelfde configuratie (cache uit), bijvoorbeeld 's nachts. Een CronJob heeft geen sync-status in ArgoCD; alarmeer op mislukte Jobs.
- **Volgorde:** sync-waves: eerst ESO-`ExternalSecret`, dan de Job.
- **Crossplane** heeft deze kanttekeningen niet: de CR's zijn gewone resources met health en diff. Wel `ignoreDifferences` voor teruggeschreven referenties. Gebruik `SkipDryRunOnMissingResource=true` op de eerste sync, zoals de provider-README doet.

### 4.4 Compatibiliteit met Keycloak 26.x-upgrades

- Keycloak brengt elke paar maanden een minor uit (26.7 → 26.8 in 2026). Elke upgrade kan de Admin API raken: 26.4 beperkte `/admin/serverinfo` tot master-admins; 26.8 eist voor eigen clients de client scope `kc-admin-api` (audience-check op de Admin API, `admin-api-audience.adoc`) en geeft een 403 bij het lezen van secrets van interne admin-clients (Crossplane v3.2.2).
- **kcc:** de laatste release is gebouwd tegen een oudere Keycloak; ondersteuning voor 26.7.4 zit alleen op `main`; 26.8 staat niet in de CI-matrix. Gevolg: Keycloak-upgrades wachten op kcc, of je draait een `edge`/zelfgebouwde image (tag `maven`/`edge-build` kan tegen een willekeurige `KEYCLOAK_VERSION` compileren).
- **Terraform/Crossplane:** volgen binnen weken.
- **Maatregel ongeacht keuze:** een upgradetest in dev die alle tenant-Jobs/CR's opnieuw toepast op de nieuwe Keycloak en een DigiD-preprod-login doet, vóór acc en prod.

### 4.5 Least privilege voor het automatiseringsaccount

Uit `master-realm.adoc`: de master-rol `admin` is superuser; `create-realm` mag realms aanmaken en "will be granted full access to any new realm they create". Per realm bestaat in master een client `<realm>-realm` met fijnmazige rollen (`manage-clients`, `manage-identity-providers`, `manage-realm`, …). keycloak/keycloak#26883: een master-admin-token bevat de rollen van alle realms en schaalt slecht.

Voorstel:

| Identiteit | Waar | Rechten | Wanneer |
|---|---|---|---|
| Bootstrap | master | `create-realm` (+ `query-realms`) | Alleen bij onboarding van een tenant; maakt het lege realm plus het tenant-service-account |
| Tenant-beheer | het tenant-realm zelf | `realm-management`: `manage-realm`, `manage-clients`, `manage-identity-providers`, `view-*`; geen `manage-users`, geen `impersonation` | Elke sync |

- Het tenant-service-account kan niet buiten zijn realm. Een gelekte credential raakt één gemeente.
- Met *Signed JWT – Federated* (Kubernetes-service-account-tokens; standaard aan vanaf Keycloak 26.6) is er geen statisch secret: de Job krijgt een kortlevend geprojecteerd token (`expirationSeconds: 600` in het README-voorbeeld). kcc ondersteunt dat alleen op `main`; Terraform via `jwt_token_file`. Voor Crossplane **niet geverifieerd**.
- Vanaf 26.8 moet zo'n eigen client de scope `kc-admin-api` hebben.
- Of `manage-realm` genoeg is voor alle onderdelen van het template (keystore-components, authenticatieflows, thema) is **niet getest**.
- Bij kcc met login in het tenant-realm: `skip-server-info=true` en `keycloak.version` expliciet zetten.

### 4.6 Auditbaarheid (BIO 2.x 8.32, ASVS 5.0 L3)

BIO 2.x volgt ISO/IEC 27002:2022; maatregel 8.32 is wijzigingsbeheer. De keten:

1. **Wat en waarom:** Git-commit en PR-review op het template of de tenant-values.
2. **Wanneer en door wie uitgerold:** ArgoCD-synchistorie (en Job-logs, kcc kan JSON-loggen met `SPRING_PROFILES_ACTIVE=json-log`).
3. **Wat er in Keycloak veranderde:** Keycloak *admin events* per realm aanzetten (met representatie), naar de centrale log. De actor is het tenant-service-account; zo is elke wijziging buiten de pipeline (consolebeheer) direct herkenbaar aan een andere actor.

Voor ASVS 5.0 L3 raakt dit vooral de hoofdstukken over configuratie (V13) en logging (V16); de afzonderlijke eisnummers zijn hier **niet** nagelopen. Een `plan` vooraf (Terraform) of een CR-diff (Crossplane) is een sterker wijzigingsbewijs dan kcc, dat alleen achteraf logt.

### 4.7 Volwassenheid en onderhoud

- **kcc:** breed gebruikt, actief op `main` (veel fixes voor organizations, FGAP, flows), maar met trage releases: 6.4.0 (2025-02) → 6.4.1 (2026-01) → 6.5.0 (2026-03) → 6.5.1 (2026-05), sindsdien niets. Commerciële support via adorsys (README).
- **Terraform-provider:** onder de Keycloak-organisatie, maandelijkse releases, veel externe bijdragers, actief bijgehouden met nieuwe Keycloak-versies. Het grote aantal open issues (374) past bij het brede gebruik.
- **Crossplane provider-keycloak:** klein, maar zeer actief en volgt de Terraform-provider automatisch.
- **Operator:** onderdeel van Keycloak, maar de functie is bewust beperkt.

## 5. Aanbeveling

1. **Kies keycloak-config-cli** voor het beheer van de tenant-realms, uitgerold als ArgoCD-`PostSync`-Job per tenant via een `ApplicationSet` en een Helm-chart rond het realm-template. Motivatie: het past op het bestaande exportformaat, voegt geen control-plane of state-backend toe, en met remote state worden verwijderde clients/IdP's/flows ook in Keycloak opgeruimd.
2. **Randvoorwaarden:**
   - `import.cache.enabled=false`, `import.remote-state.enabled=true` met `encryption-key`, `import.var-substitution.enabled=true` alleen voor secrets, script-evaluatie uit.
   - Een nachtelijke CronJob voor drift-herstel, met alarm bij falen.
   - Twee identiteiten: bootstrap met `create-realm` (onboarding) en een service-account per tenant-realm; nooit master-`admin`.
   - Keycloak-minor vastzetten op een versie die in de kcc-CI staat; upgrade pas na een groene upgradetest in dev.
   - Admin events per realm aan en naar de centrale log.
   - Offboarding (realm verwijderen) als expliciete, handmatige stap met bewaartermijn-afweging.
3. **Heroverweeg Crossplane** als (a) kcc Keycloak 26.8+ niet binnen een paar maanden in een release ondersteunt, of (b) het platformteam Crossplane toch introduceert. Het levert betere drift-correctie en ArgoCD-zichtbaarheid; het modelleren van het realm in CR's is wel eenmalig meer werk.
4. **Terraform** alleen als Pink al een Terraform/OpenTofu-straat met versleutelde state heeft.
5. **Gebruik `KeycloakRealmImport` niet** voor tenant-realms.

Een kleine proef vóór de ADR (#205) is zinvol: twee tenant-realms (één met de DigiD-plugin, één met Signicat) via kcc met een tenant-service-account, inclusief een upgrade van 26.x naar 26.x+1 in dev.

## 6. Niet geverifieerd / open

- Of de Argo CD-health-customization `_.crossplane.io` ook de groepen `*.keycloak.crossplane.io` dekt (Lua niet gelezen).
- Hoe provider-keycloak secrets per veld aanneemt (`*SecretRef`) en of het Kubernetes-federated-JWT ondersteunt.
- Of de rollen `manage-realm` + `manage-clients` + `manage-identity-providers` volstaan voor alle onderdelen van het template, en het effect van de `kc-admin-api`-audiencecheck in combinatie met een Frontend URL per realm (gemeentedomein).
- Of er Docker-images van kcc 6.5.1 bestaan die tegen 26.6/26.7 zijn gebouwd (Docker Hub-tags niet bekeken).
- Het aantal managed resources per realm bij Crossplane (schatting).
- Gedrag van kcc bij de Pink-DigiD-plugin (eigen `providerId`, eventueel eigen config-velden): niet getest.
- ASVS 5.0 L3-eisnummers voor configuratie- en wijzigingsbeheer.
