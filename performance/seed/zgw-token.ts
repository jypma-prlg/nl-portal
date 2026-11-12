import { createHmac } from "node:crypto";

const base64url = (waarde: object) => Buffer.from(JSON.stringify(waarde)).toString("base64url");

/** Maakt het HS256-JWT waarmee Open Zaak een ZGW-client (client-id plus secret) authenticeert. */
export function zgwToken(clientId: string, secret: string, uitgifteTijd: number): string {
  const kop = base64url({ alg: "HS256", typ: "JWT" });
  const inhoud = base64url({
    iss: clientId,
    iat: uitgifteTijd,
    client_id: clientId,
    user_id: "nl-portal-perf-seed",
    user_representation: "nl-portal-perf-seed",
  });
  const handtekening = createHmac("sha256", secret).update(`${kop}.${inhoud}`).digest("base64url");
  return `${kop}.${inhoud}.${handtekening}`;
}
