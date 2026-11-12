/**
 * Seed voor de perf-stack: N testgebruikers in Keycloak (met BSN), de perf-only client en per gebruiker
 * vaste zaken, documenten en taken via de Open Zaak- en Objecten-API. Draai via `mise run perf:seed`.
 * Herhaald draaien is veilig: wat er al staat wordt overgeslagen.
 */
import { readFileSync } from "node:fs";
import { pathToFileURL } from "node:url";
import { maakPlan, type Plan, type PlanGebruiker } from "./plan.ts";
import { zgwToken } from "./zgw-token.ts";

type Env = Record<string, string | undefined>;

const zonderSlash = (url: string) => url.replace(/\/+$/, "");
const getal = (env: Env, naam: string, standaard: number) => Number(env[naam] ?? standaard);

// Secrets en tokens staan nergens als standaardwaarde: ze komen uit PERF_* of uit docker-compose/.env
// (gegenereerd door `mise run dev:setup`; `mise run perf:seed` laadt dat bestand).
function verplicht(env: Env, ...namen: string[]): string {
  for (const naam of namen) if (env[naam]) return env[naam];
  throw new Error(`Zet ${namen.join(" of ")} (draai eerst mise run dev:setup)`);
}

// De overige standaardwaarden zijn de lokale demo-gegevens uit docker-compose/imports (realm-import).
export function laadConfig(env: Env) {
  const openzaak = zonderSlash(env.PERF_OPENZAAK_URL ?? "http://localhost:8001");
  const objecten = zonderSlash(env.PERF_OBJECTEN_URL ?? "http://localhost:8010");
  return {
    plan: {
      gebruikers: getal(env, "PERF_SEED_GEBRUIKERS", 10),
      zakenPerGebruiker: getal(env, "PERF_SEED_ZAKEN", 5),
      takenPerGebruiker: getal(env, "PERF_SEED_TAKEN", 2),
      documentenPerZaak: getal(env, "PERF_SEED_DOCUMENTEN", 2),
    },
    wachtwoord: env.PERF_SEED_PASSWORD ?? env.PERF_PASSWORD ?? "perf-wachtwoord",
    keycloak: {
      url: zonderSlash(env.PERF_KEYCLOAK_URL ?? "http://localhost:8082/auth"),
      realm: env.PERF_REALM ?? "nlportal",
      adminGebruiker: env.PERF_KEYCLOAK_ADMIN ?? "admin",
      adminWachtwoord: verplicht(env, "PERF_KEYCLOAK_ADMIN_PASSWORD", "KEYCLOAK_ADMIN_PASSWORD"),
    },
    openzaak: {
      url: openzaak,
      clientId: env.PERF_ZGW_CLIENT_ID ?? "nl-portal",
      secret: verplicht(env, "PERF_ZGW_SECRET", "OPENZAAK_NLPORTAL_SECRET"),
      catalogi: `${openzaak}/catalogi/api/v1`,
    },
    objecten: {
      url: objecten,
      token: verplicht(env, "PERF_OBJECTEN_TOKEN", "OBJECTS_API_TOKEN"),
      taakTypeUrl: env.PERF_TAAK_TYPE_URL ?? `${objecten}/api/v2/objecttypes/b4a617af-6969-45e8-9afd-d51e170716d5`,
    },
  };
}
type Config = ReturnType<typeof laadConfig>;

// UUID's van het zaaktype, roltype, statustype en informatieobjecttype uit docker-compose/imports/openzaak.
const ZAAKTYPE = "5ffc767c-8a7e-45e1-ad06-37ddc7e9074d";
const ROLTYPE_AANVRAGER = "66d685fd-ed2f-45ee-ad42-835ac877cef2";
const STATUSTYPE_INTAKE = "4336fa38-9d19-4fe2-92c1-23bb70001276";
const INFORMATIEOBJECTTYPE = "36e40d95-e411-4884-b08f-e5864ec8167f";
const BRONORGANISATIE = "100000009";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
async function json(response: Response, wat: string): Promise<any> {
  if (!response.ok) {
    throw new Error(`${wat} mislukt: HTTP ${response.status} ${await response.text()}`);
  }
  const tekst = await response.text();
  return tekst ? JSON.parse(tekst) : undefined;
}

// --- Keycloak ---

async function adminToken(config: Config): Promise<string> {
  const { url, adminGebruiker, adminWachtwoord } = config.keycloak;
  const antwoord = await fetch(`${url}/realms/master/protocol/openid-connect/token`, {
    method: "POST",
    body: new URLSearchParams({
      grant_type: "password",
      client_id: "admin-cli",
      username: adminGebruiker,
      password: adminWachtwoord,
    }),
  });
  return (await json(antwoord, "Keycloak-admintoken ophalen")).access_token;
}

async function seedKeycloak(config: Config, gebruikers: PlanGebruiker[]) {
  const { url, realm } = config.keycloak;
  const kop = { Authorization: `Bearer ${await adminToken(config)}`, "Content-Type": "application/json" };
  const admin = `${url}/admin/realms/${realm}`;

  const client = JSON.parse(readFileSync(new URL("./keycloak/nl-portal-perf-client.json", import.meta.url), "utf8"));
  const bestaand = await json(await fetch(`${admin}/clients?clientId=${client.clientId}`, { headers: kop }), "Client zoeken");
  if (bestaand.length === 0) {
    await json(await fetch(`${admin}/clients`, { method: "POST", headers: kop, body: JSON.stringify(client) }), "Client aanmaken");
    console.log(`Keycloak-client ${client.clientId} aangemaakt`);
  } else {
    console.log(`Keycloak-client ${client.clientId} bestond al`);
  }

  let nieuw = 0;
  for (const { username, bsn } of gebruikers) {
    const gevonden = await json(
      await fetch(`${admin}/users?username=${username}&exact=true`, { headers: kop }),
      `Gebruiker ${username} zoeken`,
    );
    if (gevonden.length > 0) continue;
    const gebruiker = {
      username,
      enabled: true,
      emailVerified: true,
      firstName: "Burger",
      lastName: username.slice("burger-".length),
      email: `${username}@perf.example.org`,
      attributes: { bsn: [bsn], authenticationMethod: ["digid"] },
      credentials: [{ type: "password", value: config.wachtwoord, temporary: false }],
    };
    await json(
      await fetch(`${admin}/users`, { method: "POST", headers: kop, body: JSON.stringify(gebruiker) }),
      `Gebruiker ${username} aanmaken`,
    );
    nieuw++;
  }
  console.log(`Keycloak: ${nieuw} van ${gebruikers.length} gebruikers aangemaakt`);
}

// --- Open Zaak ---

function zgwClient(config: Config) {
  const aanroep = (pad: string, init: RequestInit = {}) =>
    fetch(`${config.openzaak.url}${pad}`, {
      ...init,
      headers: {
        Authorization: `Bearer ${zgwToken(config.openzaak.clientId, config.openzaak.secret, Math.floor(Date.now() / 1000))}`,
        "Content-Type": "application/json",
        "Accept-Crs": "EPSG:4326",
        "Content-Crs": "EPSG:4326",
      },
    });
  return {
    lees: async (pad: string, wat: string) => json(await aanroep(pad), wat),
    maak: async (pad: string, body: object, wat: string) =>
      json(await aanroep(pad, { method: "POST", body: JSON.stringify(body) }), wat),
  };
}

const zaakPad = (bsn: string) =>
  `/zaken/api/v1/zaken?rol__betrokkeneIdentificatie__natuurlijkPersoon__inpBsn=${bsn}&pageSize=100`;

async function seedZaken(config: Config, plan: Plan): Promise<Map<string, string>> {
  const zgw = zgwClient(config);
  const cat = config.openzaak.catalogi;
  const eerste = new Map<string, string>();
  let nieuw = 0;
  for (const { bsn } of plan.gebruikers) {
    const gewenst = plan.zaken.filter((z) => z.bsn === bsn);
    const bestaand = await zgw.lees(zaakPad(bsn), `Zaken van ${bsn} ophalen`);
    if (bestaand.count > 0 && bestaand.count !== gewenst.length) {
      throw new Error(`BSN ${bsn} heeft ${bestaand.count} zaken, het plan vraagt ${gewenst.length}: start met een lege stack`);
    }
    if (bestaand.count === 0) {
      for (const z of gewenst) {
        const zaak = await zgw.maak(
          "/zaken/api/v1/zaken",
          {
            zaaktype: `${cat}/zaaktypen/${ZAAKTYPE}`,
            bronorganisatie: BRONORGANISATIE,
            verantwoordelijkeOrganisatie: BRONORGANISATIE,
            startdatum: z.startdatum,
            omschrijving: z.omschrijving,
          },
          `Zaak ${z.omschrijving} aanmaken`,
        );
        await zgw.maak(
          "/zaken/api/v1/rollen",
          {
            zaak: zaak.url,
            betrokkeneType: "natuurlijk_persoon",
            roltype: `${cat}/roltypen/${ROLTYPE_AANVRAGER}`,
            roltoelichting: "Aanvrager",
            betrokkeneIdentificatie: { inpBsn: bsn },
          },
          `Rol bij ${z.omschrijving} aanmaken`,
        );
        await zgw.maak(
          "/zaken/api/v1/statussen",
          {
            zaak: zaak.url,
            statustype: `${cat}/statustypen/${STATUSTYPE_INTAKE}`,
            datumStatusGezet: `${z.startdatum}T09:00:00Z`,
          },
          `Status bij ${z.omschrijving} aanmaken`,
        );
        for (let d = 1; d <= z.documenten; d++) {
          const document = await zgw.maak(
            "/documenten/api/v1/enkelvoudiginformatieobjecten",
            {
              bronorganisatie: BRONORGANISATIE,
              creatiedatum: z.startdatum,
              titel: `Bijlage ${d} bij ${z.omschrijving}`,
              auteur: "perf-seed",
              taal: "nld",
              formaat: "text/plain",
              bestandsnaam: `bijlage-${d}.txt`,
              inhoud: Buffer.from(`Perf-bijlage ${d} bij ${z.omschrijving}\n`).toString("base64"),
              informatieobjecttype: `${cat}/informatieobjecttypen/${INFORMATIEOBJECTTYPE}`,
              vertrouwelijkheidaanduiding: "openbaar",
              status: "definitief",
            },
            `Document ${d} bij ${z.omschrijving} aanmaken`,
          );
          await zgw.maak(
            "/zaken/api/v1/zaakinformatieobjecten",
            { zaak: zaak.url, informatieobject: document.url },
            `Document ${d} aan ${z.omschrijving} koppelen`,
          );
        }
        nieuw++;
      }
    }
    const huidig = await zgw.lees(zaakPad(bsn), `Zaken van ${bsn} ophalen`);
    const sleutel = (z: { startdatum: string; omschrijving: string }) => `${z.startdatum} ${z.omschrijving}`;
    const eersteZaak = [...huidig.results].sort((a, b) => sleutel(a).localeCompare(sleutel(b)))[0];
    if (eersteZaak) eerste.set(bsn, eersteZaak.uuid);
  }
  console.log(`Open Zaak: ${nieuw} zaken aangemaakt (elk met rol, status en documenten)`);
  return eerste;
}

// --- Objecten API (taken) ---

async function seedTaken(config: Config, plan: Plan, zaakPerBsn: Map<string, string>) {
  const { url, token, taakTypeUrl } = config.objecten;
  const kop = {
    Authorization: `Token ${token}`,
    "Content-Type": "application/json",
    "Content-Crs": "EPSG:4326",
    "Accept-Crs": "EPSG:4326",
  };
  let nieuw = 0;
  for (const { bsn } of plan.gebruikers) {
    const gewenst = plan.taken.filter((t) => t.bsn === bsn);
    const zoek = new URLSearchParams({
      type: taakTypeUrl,
      data_attrs: `identificatie__value__exact__${bsn}`,
      pageSize: "100",
    });
    const bestaand = await json(await fetch(`${url}/api/v2/objects?${zoek}`, { headers: kop }), `Taken van ${bsn} ophalen`);
    if (bestaand.count > 0) {
      if (bestaand.count !== gewenst.length) {
        throw new Error(`BSN ${bsn} heeft ${bestaand.count} taken, het plan vraagt ${gewenst.length}: start met een lege stack`);
      }
      continue;
    }
    for (const t of gewenst) {
      const data = {
        soort: "url",
        titel: t.titel,
        status: "open",
        eigenaar: "perf-seed",
        url: { uri: "https://example.org/perf" },
        koppeling: { value: zaakPerBsn.get(bsn), registratie: "zaak" },
        verloopdatum: "2099-01-01T00:00:00.000Z",
        identificatie: { type: "bsn", value: bsn },
        verwerker_taak_id: "00000000-0000-4000-8000-000000000000",
      };
      await json(
        await fetch(`${url}/api/v2/objects`, {
          method: "POST",
          headers: kop,
          body: JSON.stringify({ type: taakTypeUrl, record: { typeVersion: 1, data, startAt: "2025-01-01" } }),
        }),
        `Taak ${t.titel} aanmaken`,
      );
      nieuw++;
    }
  }
  console.log(`Objecten API: ${nieuw} taken aangemaakt`);
}

async function main() {
  const config = laadConfig(process.env);
  const plan = maakPlan(config.plan);
  console.log(
    `Seed: ${plan.gebruikers.length} gebruikers (${plan.gebruikers[0]!.username} t/m ${plan.gebruikers.at(-1)!.username}), ` +
      `${plan.zaken.length} zaken, ${plan.taken.length} taken`,
  );
  await seedKeycloak(config, plan.gebruikers);
  const zaakPerBsn = await seedZaken(config, plan);
  await seedTaken(config, plan, zaakPerBsn);
  console.log("Seed klaar");
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((fout) => {
    console.error(fout instanceof Error ? fout.message : fout);
    process.exit(1);
  });
}
