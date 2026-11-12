export interface PerfConfig {
  graphqlUrl: string;
  tokenUrl: string;
  clientId: string;
  username: string;
  password: string;
}

type Env = Record<string, string | undefined>;

const zonderSlashAchteraan = (url: string) => url.replace(/\/+$/, "");

function verplicht(env: Env, naam: string): string {
  const waarde = env[naam];
  if (!waarde) {
    throw new Error(`Omgevingsvariabele ${naam} is verplicht (zie performance/README.md)`);
  }
  return waarde;
}

/** Leest de perf-configuratie uit de omgeving; zonder opgave geldt de lokale compose-stack. */
export function loadConfig(env: Env): PerfConfig {
  const backend = zonderSlashAchteraan(env.PERF_BACKEND_URL ?? "http://localhost:8080");
  const keycloak = zonderSlashAchteraan(env.PERF_KEYCLOAK_URL ?? "http://localhost:8082/auth");
  const realm = env.PERF_REALM ?? "nlportal";

  return {
    graphqlUrl: `${backend}/graphql`,
    tokenUrl: `${keycloak}/realms/${realm}/protocol/openid-connect/token`,
    clientId: env.PERF_CLIENT_ID ?? "nl-portal",
    username: verplicht(env, "PERF_USERNAME"),
    password: verplicht(env, "PERF_PASSWORD"),
  };
}
