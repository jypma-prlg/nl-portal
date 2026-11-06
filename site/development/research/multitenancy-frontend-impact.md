# Impact van een gedeelde runtime op de frontend

Geraadpleegd op: 2026-11-06. Onderzoek voor #200, onderdeel van de kaart #197 (multitenancy: gedeelde runtime). Levert input voor #201 en #205.

**Vraag:** wat moet er in de frontend (`frontend/packages/app`, nginx-image) veranderen zodat één frontend-deployment meerdere tenants op hun eigen gemeentedomein bedient?

**Uitgangspunten uit #197:** ~50 tenants, elk op een eigen gemeentespecifiek domein; tenantsleutel is het OIN; één gedeelde publieke Keycloak per omgeving; configuratie via Git/ArgoCD en OpenFeature; wijzigingen via rolling restart, nooit hot reload; ASVS 5.0.0 L3.

## Samenvatting

- **Vandaag is de frontend precies één tenant per container.** `entrypoint.sh` vult bij start één `config.js` met envsubst (OIDC-URL, -realm, -client, redirect-URI's, GraphQL/REST-URI). nginx bouwt de CSP uit dezelfde env-variabelen. Er is niets dat naar de host van het verzoek kijkt.
- **Het meeste wordt vanzelf host-onafhankelijk** als je drie dingen doet: (1) API's altijd same-origin aanspreken (`/api`, `/graphql`; nginx proxyt die al), (2) redirect-URI's afleiden van `window.location.origin`, (3) één gedeelde Keycloak-URL per omgeving. Dan is de CSP voor alle tenants gelijk en blijven per tenant alleen over: **Keycloak-realm/client**, **titel/favicon/manifest** en het **thema** (logo, CSS).
- **Aanbevolen richting (optie B + D):** nginx `map $host $tenant` met een expliciete allowlist van gemeentedomeinen, een `default_server` die onbekende hosts afwijst (444), en per tenant vooraf gegenereerde bestanden (`config.js`, `manifest.json`, iconen) die nginx via `$tenant` kiest. De tenantlijst komt uit Git (ConfigMap); een wijziging is een rolling restart. Thema en toggles blijven via `/api/public/frontend` komen, mits de backend de tenant uit dezelfde gevalideerde host afleidt (#146).
- **Host-header:** ASVS 5.0.0 heeft geen aparte eis voor de Host-header (gezocht in V1–V4 en V7–V16). Wel relevant: **4.1.3** (door de tussenlaag gezette headers niet overschrijfbaar), **3.5.4** (aparte applicaties op aparte hostnamen), **3.7.2** (alleen redirects naar allowlist), **10.4.1** (redirect-URI's exact vergeleken). Conclusie: host is pas te vertrouwen nadat ingress/nginx hem tegen een allowlist heeft gevalideerd; alles daarna leidt de tenant af uit die gevalideerde waarde, nooit uit vrije tekst.
- **Caching is geen groot risico:** browser- en CDN-caches sleutelen standaard op de volledige URL inclusief host, `config.js` heeft al `no-store`, en de backend zet via Spring Security standaard no-store-headers. Gehashte build-assets zijn tenantneutraal en mogen lang gecachet worden.
- **Bijvangst (los van tenancy, apart beleggen):** de CSP mist `object-src 'none'`, `base-uri 'none'`, `frame-ancestors` en een rapportagelocatie (ASVS 3.4.3, 3.4.6, 3.4.7); `connect-src` bevat `http://localhost:*` ook in productie; nginx zet twee `Access-Control-Allow-Origin`-headers op HTML-responses, waarvan één met een ongeldige waarde (`http://localhost:*`).

## 1. Bronnen

| Bron | Link | Opmerking |
|---|---|---|
| Code | `frontend/packages/app/public/config.template.js`, `entrypoint.sh`, `conf/nginx.conf`, `index.html`, `public/manifest.json`, `src/App.tsx`, `src/constants/config.ts`, `frontend/Dockerfile`, `frontend/window.d.ts` | Branch `main` op 775f21874 |
| Code | `frontend/packages/user-interface/src/utils/frontend-config.ts`, `.../contexts/AppContext.tsx`, `frontend/packages/app/src/components/CustomLayout.tsx` | Thema en logo |
| Code | `frontend/packages/authentication/src/context/OidcContext.tsx`, `.../utils/generate-redirect-uri.ts` | OIDC-client |
| Code | `backend/core/.../frontend/web/rest/FrontendConfigurationResource.kt`, `.../FrontendConfigurationService.kt`, `.../FrontendConfigurationProperties.kt`, `.../security/OauthSecurityAutoConfiguration.kt` | `/api/public/frontend`, headers, CORS |
| nginx `map` | https://nginx.org/en/docs/http/ngx_http_map_module.html | Gelezen via samenvattende fetch-tool, citaten letterlijk |
| nginx `add_header` | https://nginx.org/en/docs/http/ngx_http_headers_module.html | Idem |
| nginx `sub_filter` | https://nginx.org/en/docs/http/ngx_http_sub_module.html | Idem |
| nginx core, server-selectie | https://nginx.org/en/docs/http/ngx_http_core_module.html, https://nginx.org/en/docs/http/request_processing.html | Idem |
| nginx-docker entrypoint | https://github.com/nginx/docker-nginx/blob/master/entrypoint/20-envsubst-on-templates.sh | Hoe `/etc/nginx/templates` wordt gevuld |
| OWASP ASVS 5.0.0 | https://github.com/OWASP/ASVS/tree/v5.0.0_release/5.0/en (V1–V4, V7–V16) | Ruwe Markdown gedownload en eisentabellen gelezen |
| Keycloak OIDC-client | `docs/documentation/server_admin/topics/clients/oidc/con-basic-settings.adoc` en `.../threat/redirect.adoc` in `keycloak/keycloak` (main) | Via `gh api` gelezen |
| oidc-client-ts | https://github.com/authts/oidc-client-ts/blob/main/src/UserManagerSettings.ts | Default `userStore` |

**Correctie op de ticketcontext:** de frontend gebruikt **geen keycloak-js**, maar `react-oidc-context` met `oidc-client-ts` 3.5.0 (`frontend/packages/authentication/package.json`). Keycloak-js-documentatie is daarom niet gebruikt.

## 2. Huidige situatie

### 2.1 Runtimeconfiguratie

- `config.template.js` zet negen globals op `window` (`OIDC_URL`, `OIDC_REALM`, `OIDC_CLIENT_ID`, `OIDC_REDIRECT_URI`, `OIDC_POST_LOGOUT_REDIRECT_URI`, `OIDC_AUTO_IDLE_SESSION_LOGOUT`, `OIDC_IDLE_TIMEOUT_MINUTES`, `GRAPHQL_URI`, `REST_URI`). Getypeerd in `frontend/window.d.ts`.
- `entrypoint.sh` draait als `/docker-entrypoint.d/40-envsubst-config.sh` en schrijft `envsubst < config.template.js > /tmp/config.js` (één bestand, één tenant).
- `nginx.conf` wordt als `/etc/nginx/templates/nginx.conf.template` gekopieerd; het nginx-image vervangt bij start alle *gedefinieerde* env-variabelen en laat andere `$`-strings staan (`20-envsubst-on-templates.sh`: `defined_envs`, filterbaar met `NGINX_ENVSUBST_FILTER`). Daardoor blijven `$request_id` en `$csp` nginx-variabelen, maar worden `${OIDC_URL}`, `${GRAPHQL_URI}` en `${REST_URI}` in de CSP vaste tekst.
- `location = /config.js` serveert `/tmp/config.js` met `Cache-Control: no-store`.
- `index.html` laadt `<script src="/config.js">` vóór de app; `src/constants/config.ts` leest de globals één keer uit; `App.tsx` geeft ze aan `OidcProvider` en `ApiProvider`.

### 2.2 OIDC

- `OidcContext.tsx` bouwt `authority: ${url}/realms/${realm}`, `client_id`, `redirect_uri` en `post_logout_redirect_uri` uit die config.
- `generate-redirect-uri.ts` doet `new URL(redirectUri).pathname`: de redirect-URI moet nu een absolute URL zijn.
- `oidc-client-ts` bewaart de gebruiker standaard in `window.sessionStorage` (`UserManagerSettings.ts`). Die opslag is per origin; met een eigen domein per tenant zijn tokens van tenants in de browser dus al gescheiden (zie ASVS 3.5.4).
- `site/hosting-en-configuratie/keycloak.md` regel 35: de redirect-URI van de `nl-portal`-client moet overeenkomen met `OIDC_REDIRECT_URI`.

### 2.3 nginx

- Eén `server` op poort 8081 met `server_name nl-portal-app-frontend`. Er is geen `default_server` en geen hostfilter: elk Host-header komt in deze server (nginx: "If its value does not match any server name ... nginx will route the request to the default server for this port").
- `/graphql` en `/api` worden geproxied naar `http://localhost:8080` met `proxy_set_header Host $host`. `$host` is volgens nginx "host name from the request line, or host name from the "Host" request header field, or the server name matching a request". De backend krijgt dus de Host die de client stuurde.
- `X-Real-IP` en `X-Forwarded-For` worden overschreven met `$remote_addr` (goed voor ASVS 4.1.3); er wordt geen `X-Forwarded-Host` gezet.
- `location /` zet `Content-Security-Policy` met per-response nonce (`$request_id`, via `sub_filter "##NL_PORTAL_NONCE##" $request_id`; Vite zet `html.cspNonce` op die placeholder). Dat voldoet aan het L3-deel van ASVS 3.4.3 ("a per-response policy with nonces or hashes").

### 2.4 Titel, favicon, manifest

- `index.html`: vaste `<title>NL-Portal</title>`, `theme-color #ffffff`, favicons en ~40 Apple-splash-afbeeldingen uit `/pwa/` (56 bestanden).
- `public/manifest.json`: vaste `name`/`short_name` "NL Portal", vaste iconen en kleuren.
- `CustomLayout.tsx`: `aria-label` "NL Portal" vast; logo uit `features.theme.logo` als `<img src>`.

### 2.5 Thema via `/api/public/frontend`

- `AppContext.tsx` haalt na inloggen (`AppProvider` zit binnen `OidcProvider`) `${REST_URI}/public/frontend` op en roept `applyFrontendTheme` aan: die zet `features.theme.style` als `<style nonce=...>` in de `<head>` en voegt `properties.themeClass` toe aan `<html>`. `UnexpectedErrorPage.tsx` doet hetzelfde met `window.REST_URI`.
- De backend levert dit uit `FrontendConfigurationProperties` (`nl-portal.frontend.*`, één set per proces); alleen `messageCountEnabled` komt al uit OpenFeature met een `EvaluationContext` (`FrontendConfigurationService.getToggles`). Er is nog geen tenant in de context.
- `OauthSecurityAutoConfiguration` zet `h.cache(withDefaults())`; Spring Security schrijft dan no-cache/no-store-headers op backendresponses.

## 3. Wat varieert per tenant, wat niet

| Item | Nu | Per tenant nodig? | Toelichting |
|---|---|---|---|
| `OIDC_URL` | env | **Nee** | Eén gedeelde publieke Keycloak per omgeving (#197). |
| `OIDC_REALM`, `OIDC_CLIENT_ID` | env | **Ja**, afhankelijk van #199 | Realm per tenant, client per tenant of Organizations. Bij Organizations kan het één realm en één client zijn, met een tenanthint (`extraQueryParams` bestaat al in `OidcProvider`). |
| `OIDC_REDIRECT_URI`, `..._POST_LOGOUT_...` | env, absoluut | **Ja**, maar afleidbaar | `window.location.origin + "/oidc/callback"`; geen configuratie meer nodig. In Keycloak blijft per tenant een exacte redirect-URI nodig (ASVS 10.4.1). |
| `GRAPHQL_URI`, `REST_URI` | env, absoluut | **Nee**, als same-origin | nginx proxyt `/api` en `/graphql` al; relatieve paden `/api` en `/graphql` werken voor elke host. |
| Idle-timeout | env | Mogelijk | Kan per tenant verschillen (beleid gemeente); kan ook uit `/api/public/frontend`/OpenFeature. |
| CSP | uit env | **Nee**, als bovenstaande | `connect-src 'self' <keycloak>` is dan voor iedereen gelijk. Uitzondering: externe logo-URL's (`img-src 'self' data:` blokkeert die nu al). |
| Titel, favicon, manifest, splash | statisch | **Ja** | Zie sectie 6. |
| Thema-CSS, logo, toggles | backend, één set | **Ja** | Backend moet per tenant antwoorden (#146). |
| `aria-label` "NL Portal" | hardcoded | Ja (toegankelijkheid) | Gemeentenaam uit config. |

## 4. Opties voor configuratie per host

### Optie A: één deployment per tenant (frontend), gedeeld image

Status quo: hetzelfde image, per tenant een eigen Deployment/env via ArgoCD (bijv. ApplicationSet).

- **Voor:** geen codewijziging; volledige isolatie van config; nginx is klein (tientallen MB), 50 pods is technisch haalbaar.
- **Tegen:** botst met de keuze voor één gedeelde runtime; 50× rollout en monitoring; per tenant een ingress-regel en pod; canary per tenant in plaats van per release. Titel/manifest blijven alsnog statisch per image tenzij je ook die templated.

### Optie B: nginx `map $host $tenant` + vooraf gegenereerde bestanden per tenant (aanbevolen)

```nginx
map $host $tenant {
    hostnames;
    default            "";
    include /etc/nginx/tenants/hosts.map;   # mijn.gemeente-a.nl  00000001234567890000;
}

server {                                      # vangnet voor onbekende hosts
    listen 8081 default_server;
    server_name _;
    return 444;
}

server {
    listen 8081;
    server_name mijn.gemeente-a.nl mijn.gemeente-b.nl;  # gegenereerd uit dezelfde tenantlijst
    if ($tenant = "") { return 444; }                   # tweede vangnet als lijsten uiteenlopen
    location = /config.js     { alias /tmp/tenants/$tenant/config.js;     add_header Cache-Control "no-store"; }
    location = /manifest.json { alias /tmp/tenants/$tenant/manifest.json; add_header Cache-Control "no-cache"; }
    location /tenant-assets/  { alias /tmp/tenants/$tenant/assets/; }
}
```

De entrypoint (of een init-container) genereert per tenant `config.js` en `manifest.json` uit een tenantbestand in een ConfigMap uit Git.

- **Voor:** past op "Git + rolling restart, nooit hot reload": de map en bestanden zijn bij start vast. `map` is goedkoop ("variables are evaluated only when they are used"). Onbekende hosts krijgen niets (default server + 444, zoals de nginx-documentatie voorschrijft). De tenant komt uit een allowlist, niet uit vrije tekst; `$host` komt nooit direct in een pad (geen path traversal). Werkt zonder backend en vóór login (titel, favicon, manifest).
- **Tegen:** tweede plek waar de tenantlijst staat (naast backend en Keycloak); moet uit één bron gegenereerd worden. Een nieuwe tenant betekent rolling restart van de frontend. `map` met `hostnames` accepteert ook wildcards (`*.example.com`); die niet gebruiken.

### Optie C: alles via een backend-endpoint per host

`config.js` vervalt; de SPA haalt bij start `/api/public/frontend` (of `/api/public/bootstrap`) op en de backend bepaalt de tenant uit de Host.

- **Voor:** één bron van waarheid (de backend-tenantregistry uit #146/#201); nginx blijft tenantloos.
- **Tegen:** extra roundtrip en een harde afhankelijkheid op de backend vóór de OIDC-redirect (nu zit `AppProvider` binnen `OidcProvider`; de volgorde moet om). Titel, favicon en manifest staan in `index.html` en worden vóór JavaScript gelezen; die kun je niet goed vanuit een API zetten (zie sectie 6). Backend moet de Host zelf valideren; vertrouwen op `Host` van nginx is alleen veilig als nginx al heeft gefilterd.

### Optie D: afleiden uit `window.location` (aanvulling, geen zelfstandige optie)

Redirect-URI's uit `window.location.origin`, API's relatief. Wat overblijft is alleen realm/client (en bij Organizations misschien niets).

- **Voor:** minder configuratie, minder kans op fouten; CSP wordt tenantneutraal.
- **Tegen:** `generate-redirect-uri.ts` verwacht nu een absolute URL; kleine aanpassing. Keycloak moet per tenant nog steeds exacte redirect-URI's hebben.

### Optie E: `sub_filter` met map-variabelen in `index.html`

`sub_filter` accepteert variabelen in de vervanging ("The string to replace (1.9.4) and replacement string can contain variables"); het image gebruikt het al voor de nonce.

- **Voor:** titel en `theme-color` per tenant zonder extra bestanden.
- **Tegen:** alleen voor korte waarden; waarden belanden ongecodeerd in HTML (ASVS 1.2.1: context-encoding). Alleen geschikt als de waarden uit gereviewde Git-config komen en geen `<`, `"` of `&` bevatten (validatie bij genereren). Niet voor `config.js` gebruiken.

### Vergelijking

| | A per tenant | B map + bestanden | C backend | D afleiden | E sub_filter |
|---|---|---|---|---|---|
| Past bij gedeelde runtime | nee | ja | ja | ja | ja |
| Werkt vóór login / vóór JS | ja | ja | nee | ja | ja |
| Onbekende host afgewezen | ingress | nginx + ingress | backend | n.v.t. | n.v.t. |
| Bronnen van tenantlijst | ArgoCD | ConfigMap (gegenereerd) | backend | — | ConfigMap |
| Codewijziging frontend | geen | klein | middel | klein | geen |

**Advies:** B + D, eventueel E voor de titel. Genereer `hosts.map`, de per-tenantbestanden, de Keycloak-redirect-URI's en de ingress-hosts uit **één** tenantbestand in Git (sluit aan op het onboarding-punt in #197 "Not yet specified").

## 5. CSP, CORS en redirect-URI's per host

- **CSP:** met same-origin API's en één Keycloak is de policy voor alle tenants gelijk: `connect-src 'self' https://<keycloak>`. Een `map` voor de CSP is dan niet nodig. Kan toch nodig zijn als tenants externe logo's of fonts gebruiken; liever per tenant via `/tenant-assets/` same-origin serveren (ASVS 3.6.1 vraagt SRI voor extern gehoste assets). `add_header` mag variabelen bevatten, dus een per-tenant CSP via `map` is technisch mogelijk.
- **Let op `add_header`-overerving:** "inherited from the previous configuration level if and only if there are no add_header directives defined on the current level". Nieuwe `location`-blokken met eigen `add_header` (zoals `/config.js`) verliezen dus de headers van `server`-niveau. Gebruik `always` waar headers ook op foutresponses moeten staan. Nieuwere nginx (1.29.3) heeft `add_header_inherit merge`.
- **CORS:** bij same-origin API's is CORS voor de portal niet nodig. De huidige twee `add_header Access-Control-Allow-Origin` in `location /` leveren twee headers op HTML op; `http://localhost:*` is geen geldige origin. ASVS 3.4.2 vraagt een vaste waarde of validatie tegen een allowlist. Advies: weghalen. De backend-CORS (`CorsPathConfiguration`) is per pad, niet per host; met same-origin is die voor de portal niet nodig.
- **Keycloak:** "Exact (case sensitive) string matching is used to compare valid redirect URIs"; wildcards alleen aan het eind. Keycloak waarschuwt: "Registering vague redirect URIs ... can allow malicious clients to impersonate another client". Dus per tenant exacte URI's (`https://mijn.gemeente-a.nl/oidc/callback`) en exacte post-logout-URI's, geen `*`. "Web Origins" is alleen relevant voor CORS op Keycloak-endpoints (token, userinfo) die de SPA vanaf zijn eigen origin aanroept: per tenant de eigen origin of `+` (afgeleid uit de redirect-URI's). Welke client dit draagt hangt af van #199.

## 6. Titel, favicon en PWA-manifest

- Browsers lezen `<title>`, `<link rel="icon">` en `<link rel="manifest">` uit de HTML vóór de app draait. Per tenant variëren kan op drie manieren:
  1. **Vaste paden, nginx kiest het bestand via `$tenant`** (optie B): `href="/manifest.json"` blijft, nginx serveert `/tmp/tenants/$tenant/manifest.json`; iconen idem onder `/tenant-assets/`. `index.html` hoeft niet te veranderen behalve iconpaden.
  2. **`sub_filter`** voor titel en `theme-color` (optie E).
  3. **JavaScript** (`document.title`, favicon-link vervangen) na laden van config: werkt voor tabtitel, niet betrouwbaar voor installatie als PWA of voor de eerste weergave.
- Cache: als het bestand per tenant verschilt maar het pad gelijk is, sleutelen browser en CDN op de volledige URL inclusief host, dus geen vermenging tussen domeinen. Wel `no-cache` (revalideren) zetten zodat een nieuwe tenantversie na een rolling restart wordt opgehaald.
- 40 splash-afbeeldingen per tenant is veel beheer. Overweeg ze neutraal te houden of weg te laten.

## 7. Runtime-thema via `/api/public/frontend`

- Dit is al het juiste kanaal voor thema en toggles; het hoeft niet naar nginx. Vereist wel dat de backend de tenant per request bepaalt (#146) en `FrontendConfigurationProperties` per tenant levert, en dat de OpenFeature-`EvaluationContext` een tenantattribuut (OIN) krijgt (#155 heeft de context per request al).
- `theme.style` is willekeurige CSS die met een geldige nonce wordt geïnjecteerd. Met tenantconfig uit gereviewde Git is dat acceptabel, maar het is wel een vertrouwensgrens: CSS kan via attribuutselectors data lekken. Advies: alleen Pink-beheerde thema's, geen tenant-self-service zonder review, of overstappen op alleen design-tokens (CSS-variabelen) in plaats van vrije CSS.
- `theme.logo` als `<img src>`: met de huidige `img-src 'self' data:` moet het logo same-origin of `data:` zijn. Same-origin per tenant via `/tenant-assets/` past daarbij.
- De endpoint is anoniem en zou na inloggen geladen worden; als branding vóór login nodig is (foutpagina, landingspagina), komt die beter uit optie B.

## 8. Caching (browser en CDN)

- `config.js`: al `Cache-Control: no-store`; laten zo.
- `index.html`: bevat de per-response nonce, dus mag niet gedeeld gecachet worden; nu zet `location /` geen `Cache-Control`. Advies: `no-store` op HTML (ook nodig voor de nonce).
- Gehashte assets (`/assets/*-<hash>.js`): tenantneutraal, `immutable`, lang cachen.
- `/api/public/frontend`: Spring Security-cacheheaders staan aan, dus niet gecachet. Bij een CDN ervoor: cachekey moet de host bevatten (standaard bij de grote CDN's, maar controleren); nooit een CDN die naar de origin met één vaste Host doorzet terwijl de tenant uit Host wordt bepaald.
- Een CDN is niet nodig voor ~50 tenants; als het er komt, eerst sectie 9 opnieuw toetsen.

## 9. ASVS 5.0.0 L3-aandachtspunten

ASVS 5.0.0 bevat **geen** eis die de Host-header bij naam noemt (gezocht in de eisentabellen van V1–V4 en V7–V16; V5 en V6 niet gedownload, die gaan over bestanden en authenticatie). De relevante eisen:

| Eis | Level | Wat het betekent voor de frontend |
|---|---|---|
| 4.1.3 | 2 | Headers die de tussenlaag zet (X-Forwarded-*, Host richting backend) mag de gebruiker niet kunnen overschrijven. nginx overschrijft `X-Forwarded-For` al; voor tenantbepaling in de backend: nginx zet een vaste header met de gevalideerde tenant of host, en de backend accepteert alleen verkeer via nginx. |
| 3.5.4 | 2 | Aparte applicaties op aparte hostnamen. Eigen gemeentedomein per tenant geeft per tenant een eigen origin: sessionStorage, cookies en CORS zijn per tenant gescheiden. Geen paden als `portal.pink.nl/<gemeente>`. |
| 3.7.2 | 2 | Alleen automatisch redirecten naar hosts op een allowlist. Redirect-URI's uit `window.location.origin` zijn alleen veilig omdat nginx onbekende hosts al heeft afgewezen en Keycloak exact vergelijkt. |
| 10.4.1 | 1 | Keycloak vergelijkt redirect-URI's exact per client; geen wildcards over tenants heen. |
| 10.5.3 | 2 | Client weigert metadata als issuer niet exact gelijk is aan de verwachte; de authority (`OIDC_URL` + realm) per tenant moet uit config komen, niet uit de Host. |
| 3.4.2 | 1 | ACAO vast of tegen allowlist; huidige `localhost:*` weg. |
| 3.4.3 | 2 (L3: per response nonce) | Nonce is er al; `object-src 'none'` en `base-uri 'none'` ontbreken. |
| 3.4.6, 3.4.7, 3.4.8 | 2, 3, 3 | `frame-ancestors`, rapportagelocatie en `Cross-Origin-Opener-Policy` ontbreken op HTML. |
| 3.4.1, 3.7.4 | 1, 3 | HSTS op alle responses; L3: domein op de HSTS-preloadlijst. Bij gemeentedomeinen die van de gemeente zijn moet de gemeente dat doen; afspraak nodig in onboarding. |
| 3.5.7 | 3 | Geen geautoriseerde data in scriptresponses: `config.js` bevat alleen publieke config, houden zo. |
| 1.2.1 | 1 | Bij `sub_filter`/genereren van `config.js` waarden contextgecodeerd invoegen. |
| 13.4.6, 13.4.7 | 3 | Geen versie-informatie (`server_tokens off`) en alleen bekende extensies serveren. Niet tenantspecifiek, wel in hetzelfde nginx-bestand. |

## 10. Wat moet er concreet veranderen (voorstel, nog geen besluit)

1. `REST_URI`/`GRAPHQL_URI` standaard relatief (`/api`, `/graphql`); redirect-URI's afleiden uit `window.location.origin` (`generate-redirect-uri.ts`, `config.ts`).
2. nginx: `default_server` met `return 444`; `map $host $tenant` uit een gegenereerd `hosts.map`; per-tenant `config.js`, `manifest.json` en assets via `$tenant`. CSP vast per omgeving.
3. Entrypoint of init-container genereert per tenant de bestanden uit één tenantbestand (ConfigMap uit Git); rolling restart bij wijziging.
4. `index.html`: iconpaden naar `/tenant-assets/`; titel via `sub_filter` of uit tenant-config.
5. Backend `/api/public/frontend` per tenant (afhankelijk van #146); OpenFeature-context met OIN.
6. Ingress: alleen bekende gemeentedomeinen; TLS bestaand proces.
7. Bijvangst apart beleggen: CSP-directives (3.4.3/3.4.6/3.4.7), COOP (3.4.8), dubbele ACAO, `localhost` in `connect-src`, `Cache-Control` op HTML.

## 11. Niet geverifieerd

- Hoe de frontend in k8s draait: `nginx.conf` proxyt naar `localhost:8080`, wat op een sidecar of één pod met backend wijst; geen Helm-chart in deze repo gevonden. Het advies gaat uit van een ingress vóór nginx.
- Of de ingress (welke controller) zelf al onbekende hosts weert en welke headers hij doorgeeft.
- Of Keycloak-Organizations (#199) één client voor alle tenants mogelijk maakt; dat bepaalt of realm/client per tenant in `config.js` moet.
- Of browsers een gewijzigd manifest na `no-cache` direct oppikken voor al geïnstalleerde PWA's (gedrag verschilt per browser; niet getest).
- De nginx-, Keycloak- en oidc-client-ts-citaten zijn deels via een samenvattende fetch-tool gelezen; de Keycloak-citaten via `gh api` op de bron.
