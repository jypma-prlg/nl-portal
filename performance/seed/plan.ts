/** Deterministisch seed-plan: dezelfde invoer geeft altijd dezelfde gebruikers, zaken en taken. */

export interface PlanConfig {
  gebruikers: number;
  zakenPerGebruiker: number;
  takenPerGebruiker: number;
  documentenPerZaak: number;
}

export interface PlanGebruiker {
  username: string;
  bsn: string;
}

export interface PlanZaak {
  bsn: string;
  omschrijving: string;
  startdatum: string;
  documenten: number;
}

export interface PlanTaak {
  bsn: string;
  titel: string;
}

export interface Plan {
  gebruikers: PlanGebruiker[];
  zaken: PlanZaak[];
  taken: PlanTaak[];
}

// Ruim buiten de BSN's van de demodata (o.a. 999993847), zodat seed-data en demodata niet botsen.
const EERSTE_BSN = 900000000;

const heeftElfproef = (bsn: number): boolean => {
  const cijfers = String(bsn).padStart(9, "0").split("").map(Number);
  const som = cijfers.reduce((acc, c, i) => acc + (i < 8 ? c * (9 - i) : -c), 0);
  return som % 11 === 0;
};

/** Het n-de (1-gebaseerd) BSN dat de elfproef doorstaat, geteld vanaf een vaste start. */
export function bsnVoorGebruiker(nummer: number): string {
  let gevonden = 0;
  for (let kandidaat = EERSTE_BSN; ; kandidaat++) {
    if (heeftElfproef(kandidaat) && ++gevonden === nummer) {
      return String(kandidaat);
    }
  }
}

/** Vaste datum (YYYY-MM-DD) voor zaak `index` van gebruiker `nummer`; geen klok. */
export function zaakDatum(nummer: number, index: number): string {
  const dagen = ((nummer - 1) * 3 + (index - 1)) % 700;
  return new Date(Date.UTC(2025, 0, 1 + dagen)).toISOString().slice(0, 10);
}

const volgnummer = (n: number) => String(n).padStart(3, "0");

export function maakPlan(config: PlanConfig): Plan {
  for (const [naam, waarde] of Object.entries(config)) {
    const minimum = naam === "gebruikers" ? 1 : 0;
    if (!Number.isInteger(waarde) || waarde < minimum) {
      throw new Error(`${naam} moet een geheel getal van minimaal ${minimum} zijn, kreeg ${waarde}`);
    }
  }
  const plan: Plan = { gebruikers: [], zaken: [], taken: [] };
  for (let nummer = 1; nummer <= config.gebruikers; nummer++) {
    const username = `burger-${volgnummer(nummer)}`;
    const bsn = bsnVoorGebruiker(nummer);
    plan.gebruikers.push({ username, bsn });
    for (let i = 1; i <= config.zakenPerGebruiker; i++) {
      plan.zaken.push({
        bsn,
        omschrijving: `Perf-zaak ${volgnummer(i)} van ${username}`,
        startdatum: zaakDatum(nummer, i),
        documenten: config.documentenPerZaak,
      });
    }
    for (let i = 1; i <= config.takenPerGebruiker; i++) {
      plan.taken.push({ bsn, titel: `Perf-taak ${volgnummer(i)} van ${username}` });
    }
  }
  return plan;
}
