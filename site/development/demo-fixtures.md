# Demo-fixtures

De demo-data van de lokale compose-stack (zaken, klanten, objecten, producten) staat als Django-fixtures
onder `docker-compose/imports/<service>/fixtures`. Deze pagina legt uit hoe ze geladen worden, wanneer je
ze gebruikt, waar ze automatisch worden geladen en hoe je ze actueel houdt. De compose-kant (poorten,
profielen, wachtwoord-variabelen per image) staat in [docker-compose/README.md](https://github.com/pinkroccade/nl-portal/blob/main/docker-compose/README.md).

## Werking

Alle demo-data zijn model-geserialiseerde Django-fixtures, geladen met `manage.py loaddata` nadat
`migrate` is gedraaid. Er is geen SQL-seeding.

Per service draaien twee containers:

1. De app-container draait de eigen `/start.sh` van het image: migraties en de `admin`/`admin`-superuser.
2. Een `*-import`-sidecar (zelfde image) mount `imports/<service>/fixtures` en
   `imports/<service>/init`, wacht tot `manage.py migrate --check` slaagt en voert dan
   `imports/<service>/init/init.sh` uit. Dat script doet alleen `loaddata`.

| Service | Sidecar | Geladen fixtures |
|---|---|---|
| Open Zaak | `nl-portal-openzaak-import` | `configuration catalogi documenten zaken` |
| Objecten | `nl-portal-open-object-import` | `configuration objecttypes objects` |
| Open Notificaties | `nl-portal-open-notificaties-import` | `configuration kanalen` |
| OpenKlant 2 | `nl-portal-openklant-2-import` | `admin_user configuration partijen klantcontacten` |
| Open Product | `nl-portal-openproduct-import` | UPL-referentielijst (`load_upl`), dan `configuration demodata` |

Bron: `docker-compose/docker-compose.yaml` (de sidecars) en `docker-compose/imports/*/init/init.sh`.
Elk `init.sh` draait met `set -e`: faalt een `loaddata`, dan eindigt de sidecar met een exitcode
ongelijk aan 0 in plaats van toch "Done" te melden (dat gebeurde in [#451](https://github.com/pinkroccade/nl-portal/pull/451)).

## Wanneer zinvol

Gebruik ze voor:

- **Lokaal ontwikkelen** met `docker compose --profile zgw up -d`: je hebt direct zaken, klanten en producten om
  tegenaan te werken.
- **Handmatig testen** van de portal met de demogebruiker (bijvoorbeeld BSN `999993847`, zie
  `frontend/packages/app-dev/README.md`).
- **Performancetests**: de perf-stack start dezelfde fixtures, zie hieronder.

Niet voor:

- **Productie of een gedeelde omgeving.** De fixtures bevatten bekende wachtwoorden en gegevens (`admin`/`admin`)
  en zijn bedoeld voor een weggegooide lokale stack.
- **Veel data of realistische volumes.** Daarvoor is het perf-seed-script.
- **Integratietests.** De OpenKlant-integratietests gebruiken een eigen kopie onder
  `backend/docker-resources/imports/openklant-2/` (en `backend/zgw/openklant/docker-compose-override.yml`).

## Waar automatisch geladen

Fixtures worden geladen zodra een profiel met een import-sidecar start; er is geen aparte stap.

| Situatie | Fixtures? |
|---|---|
| Profiel `zgw` (`docker compose --profile zgw up -d`) | Alle vijf de sidecars |
| Profielen `openzaak`, `objecten`, `openklant`, `openproduct` los | Alleen de sidecar van dat profiel |
| Profielen `haalcentraal`, `clamav`; alleen Keycloak | Geen: dat zijn mocks of services zonder fixtures |
| `RUN_MODE=sources`, `local` of `remote` (`compose.localhost-*.yaml`) | Geen verschil: alleen de plek van de portal-app verandert |
| Perf-overlay (`mise run perf:stack-up`, `compose.perf.yaml`, profiel `zgw`) | Ja, dezelfde fixtures. `mise run perf:seed` voegt daar een eigen, deterministische dataset aan toe (testgebruikers `burger-001`..., eigen BSN's buiten de demodata, zaken en taken via de API's). Dat seed-script gebruikt de fixtures niet. |
| `mise run dev:setup` ([#151](https://github.com/pinkroccade/nl-portal/issues/151)) | Nee: genereert alleen secrets, de Keycloak-realms (`docker-compose/.generated`), `docker-compose/.env` en `.env.properties`. De fixtures komen mee met `docker compose up`. |

Een tweede `docker compose up` op een bestaande stack laadt de fixtures opnieuw over de bestaande data heen;
voor een schone staat gebruik je `docker compose down -v`.

## Bijwerken en actueel houden

Fixtures zijn gecommitte artefacten en horen bij de versie van het image waarmee ze zijn gemaakt.

**Wanneer opnieuw genereren?** Na een image-bump van een service met een schemawijziging (een Renovate-PR die
een tag in `docker-compose/docker-compose.yaml` verhoogt) of als de demodata inhoudelijk wijzigt. Een
minor-bump die alleen nullable kolommen toevoegt laadt meestal ongewijzigd.

**Wie en wanneer?** Degene die de image-bump merget (of de demodata wijzigt) draait eerst
`mise run fixtures:check`. Faalt die, dan regenereert diezelfde persoon de fixtures in dezelfde PR.

**Procedure** (uit [docker-compose/README.md](https://github.com/pinkroccade/nl-portal/blob/main/docker-compose/README.md#regenerating-fixtures)):

```shell
docker compose --profile <profiel> up -d
docker compose exec <service> python /app/src/manage.py dumpdata <apps> --indent 2 -o <fixture.json>
```

Kopieer het resultaat naar `imports/<service>/fixtures/` en commit het. Voor `admin_user.json` van
OpenKlant 2 staat de aparte procedure (vaste `date_joined`, `last_login` leeg) in de README; houd de kopie
onder `backend/docker-resources/imports/openklant-2/fixtures/` gelijk.

### Controleren dat ze laden

```shell
mise run fixtures:check
```

De taak (`mise-tasks/fixtures/check`) start op een schone stack (eigen compose-project, daarna `down -v`) alleen de vijf
import-sidecars met hun afhankelijkheden, wacht tot ze klaar zijn en faalt zodra een sidecar met een
exitcode ongelijk aan 0 eindigt, of niet binnen `FIXTURES_CHECK_TIMEOUT` seconden (standaard 900) klaar is.
Docker is nodig; de taak weigert te starten als er al `nl-portal-*`-containers bestaan, omdat die vaste namen
hebben. Een run haalt enkele grote images op en duurt minuten.

**Waarom geen CI-stap?** De taak is bewust niet aan CI gehangen: elke run haalt vijf grote linux/amd64-images
met databases op en duurt minuten, en dat vraagt een eigen workflow met pad-filter op
`docker-compose/**` en gepinde acties. Dat is meer infrastructuur dan deze documentatiewijziging
rechtvaardigt; het is een logische vervolgstap (een workflow die `mise run fixtures:check` draait bij
wijzigingen onder `docker-compose/`).
