# 16. Stateless configuratie en thema via bestanden, zonder configuration panel

## Status

Voorgesteld. Nog geen issue of implementatie; de uitwerking (spec en tickets) volgt na akkoord.

## Context

NL Portal (Pink Edition) is stateless (ADR 0013). Upstream kent een Ritense *configuration panel*: een
aparte service met database die via Spring Cloud Config modules, frontend-toggles, thema (logo en CSS)
en koppelingen aanlevert, en na een wijziging een refresh of restart aanroept. Pink richt dat panel
niet in. Elke omgeving moet wel qua logo en look & feel aanpasbaar zijn voor de herkenbaarheid van de
gemeente. Wijzigingen mogen via een rolling restart lopen; hot reload is niet nodig.

Bij onderzoek bleek dat het thema in de backend en de frontend al grotendeels bestaat:
`nl-portal.frontend.theme.style` en `.logo`, `properties.themeClass`, en `GET /public/frontend`. De
frontend gebruikt `theme.logo` al in `CustomLayout` (`app`, `app-dev`) en `UnexpectedErrorPage`, met het
statische NL Portal-logo als fallback. Wat ontbreekt is een bruikbare invulling: grote logo's en CSS als
property-string, alt-tekst, favicon, validatie en een manier om vooraf te zien hoe het eruitziet.

## Beslissing

1. **Eén bron per omgeving: config-files.** Thema, frontend-teksten en -URL's en module-instellingen
   komen uit Spring-properties in bestanden die per deployment worden geladen (`spring.config.import`,
   bijvoorbeeld uit een ConfigMap). Er is geen runtime-bron: geen database, config-server of panel.
   Compatibiliteit met de panel-sleutels is geen doel.
2. **Eén deployment is één gemeente.** De tenant blijft `default`; er komt geen tenant-resolutie per
   request. Meerdere gemeenten betekent meerdere deployments met elk een eigen set bestanden.
3. **Logo en stijl als bestand.** De backend leest `theme.logo-file` en `theme.style-file` bij opstart en
   serveert ze als eigen assets met cache-headers en een versie of hash. `/public/frontend` bevat alleen
   de URL's. Een `<link>` naar de stijl maakt de CSP-nonce voor thema-CSS overbodig.
4. **Fail-fast bij opstart.** Een geconfigureerd maar ontbrekend of ongeldig logo- of stijlbestand, of een
   ontbrekende verplichte koppeling-URL, laat de pod niet starten. Terugvallen op het standaardthema
   gebeurt alleen als het bestand niet is geconfigureerd.
5. **Secrets gescheiden.** Secrets komen uit een Kubernetes Secret of Vault, in aparte bestanden met
   eigen rechten, nooit in de ConfigMap met thema en teksten en nooit in een flag-systeem (ADR 0014).
6. **Geen hot reload.** De Helm-chart zet een checksum-annotatie van de config op de Deployment, zodat
   een wijziging een rolling restart afdwingt. Geen Reloader en geen `/actuator/refresh`. Module aan/uit
   blijft een opstart-keuze; #157 is hiervoor niet nodig.
7. **Onderhoudsmelding via flagd.** Ook deze wijziging landt via een rolling update (ADR 0014, besluit 7).
   Hij wordt een string- of JSON-flag in flagd; `FeatureFlag` wordt daarvoor uitgebreid voorbij
   boolean-only. De properties `overviewMaintenanceAlert*` vervallen (ADR 0012).
8. **Validatie in één component, op twee plekken.** Dezelfde validator draait bij opstart en in CI.
   Hij controleert verplichte sleutels, CSS (geen `@import` of externe `url()` vanwege de CSP) en
   logo-type en -grootte, en het WCAG-contrast (AA: 4,5:1 voor tekst, 3:1 voor UI-elementen en focus)
   van een vaste lijst kleurparen uit de design tokens. Een contrastfout faalt de build.
9. **Gerenderd contrast in CI.** Een axe-core-run (`color-contrast`) tegen `app-dev` met de config van de
   omgeving, op een vaste set kernpagina's. Faalt de build zodra de set stabiel is. Bestaat de
   axe-infrastructuur nog niet, dan is dit een eigen ticket en geldt in de tussentijd laag 8 plus de
   preview.
10. **Preview vóór deploy.** Een thema bekijk je lokaal of in een preview-omgeving door de backend en
    `app-dev` te starten met de config-file van die gemeente. Geen Storybook per thema.
11. **Reikwijdte look & feel.** Logo (met configureerbare alt-tekst en `aria-label`), `themeClass`,
    design tokens in CSS, favicon en paginatitel. Taal-overrides per gemeente vallen buiten deze ADR.
12. **Logo komt uit de context.** `Layout.tsx` leest het thema-logo zelf uit `AppContext`; de
    `headerLogo`-prop blijft als expliciete override. De dubbele fallback in `CustomLayout` (`app`,
    `app-dev`) en `UnexpectedErrorPage` verdwijnt, zodat ook eigen apps op de library het thema krijgen.

## Overwogen alternatieven

- **Een eigen configuratieservice met database (panel of nieuw):** geeft een UI en live wijzigen, maar
  breekt ADR 0013, voegt een kritieke afhankelijkheid toe en bewaart secrets en wijzigingshistorie
  buiten Git.
- **Thema in flagd:** flagd is voor gedrag, niet voor grote assets, en een thema hoort bij de omgeving,
  niet bij een gebruikerscontext.
- **Logo en CSS inline houden als property:** werkt, maar blaast `/public/frontend` op bij elke
  paginalading en maakt de config-file slecht leesbaar.
- **Reloader of hot reload:** meer bewegende delen; rolling restarts volstaan en zijn expliciet en
  controleerbaar.
- **Tenant-resolutie per request:** pas nodig bij één deployment voor meerdere gemeenten; dat is nu geen eis.

## Gevolgen

- Een kleine backend-uitbreiding: `theme.*-file`, asset-endpoints, validator en fail-fast.
- Wijzigingen aan thema of koppelingen lopen via een PR op de config-repository (Git is de audit-log) en
  een rolling restart. Er is geen UI om te wijzigen; het vervangt de panel-UI door CI-validatie en preview.
- Gemeenten leveren hun thema aan als bestanden (SVG of PNG, CSS met design tokens); dat vraagt een
  korte handleiding onder Gebruik & inrichting.
- Alle instellingen, de onderhoudsmelding inbegrepen, kosten een rolling update. Dat is bewust.
- De CSP wordt strenger voor thema-CSS: alleen eigen assets, geen externe bronnen.
- Open uitwerking voor de spec: formaat en locatie van de lijst contrastparen, welke kernpagina's axe
  afdekt, en of het asset-endpoint onder `/public` of een eigen pad komt.
