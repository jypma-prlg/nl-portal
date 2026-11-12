import { test } from "node:test";
import assert from "node:assert/strict";
import { zaakDatum, bsnVoorGebruiker, maakPlan } from "./plan.ts";

const isElfproef = (bsn: string) => {
  const cijfers = bsn.split("").map(Number);
  const som = cijfers.reduce((acc, c, i) => acc + (i < 8 ? c * (9 - i) : -c), 0);
  return bsn.length === 9 && som % 11 === 0;
};

test("gebruikersnamen lopen op van burger-001 en zijn vast", () => {
  const plan = maakPlan({ gebruikers: 3, zakenPerGebruiker: 1, takenPerGebruiker: 1, documentenPerZaak: 1 });
  assert.deepEqual(plan.gebruikers.map((g) => g.username), ["burger-001", "burger-002", "burger-003"]);
});

test("elke gebruiker krijgt een uniek BSN dat de elfproef doorstaat", () => {
  const bsns = Array.from({ length: 200 }, (_, i) => bsnVoorGebruiker(i + 1));
  assert.equal(new Set(bsns).size, 200);
  assert.ok(bsns.every(isElfproef), "alle BSN's voldoen aan de elfproef");
});

test("een BSN is onafhankelijk van het totaal aantal gebruikers (stabiel bij opschalen)", () => {
  const klein = maakPlan({ gebruikers: 2, zakenPerGebruiker: 1, takenPerGebruiker: 0, documentenPerZaak: 0 });
  const groot = maakPlan({ gebruikers: 5, zakenPerGebruiker: 1, takenPerGebruiker: 0, documentenPerZaak: 0 });
  assert.deepEqual(klein.gebruikers, groot.gebruikers.slice(0, 2));
});

test("BSN's botsen niet met de BSN's uit de standaard demodata", () => {
  const bsns = Array.from({ length: 1000 }, (_, i) => bsnVoorGebruiker(i + 1));
  assert.ok(!bsns.includes("999993847"));
});

test("twee keer hetzelfde plan geeft identieke uitkomst (geen klok of toeval)", () => {
  const config = { gebruikers: 4, zakenPerGebruiker: 3, takenPerGebruiker: 2, documentenPerZaak: 2 };
  assert.deepEqual(maakPlan(config), maakPlan(config));
});

test("het plan bevat het gevraagde volume", () => {
  const plan = maakPlan({ gebruikers: 4, zakenPerGebruiker: 3, takenPerGebruiker: 2, documentenPerZaak: 2 });
  assert.equal(plan.zaken.length, 12);
  assert.equal(plan.taken.length, 8);
  assert.equal(plan.zaken.every((z) => z.documenten === 2), true);
  assert.equal(plan.zaken.filter((z) => z.bsn === plan.gebruikers[0]!.bsn).length, 3);
});

test("zaakdata hangt alleen van gebruiker en volgnummer af", () => {
  assert.equal(zaakDatum(1, 1), "2025-01-01");
  assert.equal(zaakDatum(1, 2), "2025-01-02");
  assert.match(zaakDatum(7, 40), /^\d{4}-\d{2}-\d{2}$/);
});

test("een ongeldige configuratie wordt geweigerd", () => {
  assert.throws(() => maakPlan({ gebruikers: 0, zakenPerGebruiker: 1, takenPerGebruiker: 0, documentenPerZaak: 0 }), /gebruikers/);
});
