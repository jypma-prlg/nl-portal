export interface GraphqlRequest {
  body: string;
  headers: Record<string, string>;
}

export function buildGraphqlRequest(query: string, token: string, variables: object = {}): GraphqlRequest {
  return {
    body: JSON.stringify({ query, variables }),
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
  };
}

/** Geeft de foutmeldingen van een GraphQL-antwoord; een leeg resultaat betekent dat het antwoord goed is. */
export function graphqlErrors(responseBody: string): string[] {
  let parsed: { data?: unknown; errors?: { message?: string }[] };
  try {
    parsed = JSON.parse(responseBody);
  } catch {
    return ["antwoord is geen JSON"];
  }
  if (parsed.errors?.length) {
    return parsed.errors.map((fout) => fout.message ?? "onbekende GraphQL-fout");
  }
  if (parsed.data == null) {
    return ["antwoord bevat geen data"];
  }
  return [];
}
