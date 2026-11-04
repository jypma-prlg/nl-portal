// Minimal parser for the YAML subset used by docs/capabilities.yaml.
//
// The repo has no root package.json (and so no YAML dependency) and scripts here are
// dependency-free Node. The inventory only uses: comments, `key: scalar`, block mappings,
// block sequences of flow mappings or block mappings, and one-line flow mappings/sequences.
// A real YAML parser would reject some of its flow values (parentheses with commas inside a
// flow mapping), so this parser is deliberately paren-aware. Anything outside the subset
// throws instead of being guessed.

function scalar(raw) {
  const s = raw.trim();
  if (s === "") return null;
  if ((s.startsWith('"') && s.endsWith('"')) || (s.startsWith("'") && s.endsWith("'"))) {
    return s.slice(1, -1);
  }
  if (s === "true") return true;
  if (s === "false") return false;
  if (/^-?\d+$/.test(s)) return Number(s);
  return s;
}

function splitTopLevel(s, sep = ",") {
  const parts = [];
  let depth = 0;
  let quote = null;
  let cur = "";
  for (const ch of s) {
    if (quote) {
      if (ch === quote) quote = null;
    } else if (ch === '"' || ch === "'") {
      quote = ch;
    } else if ("({[".includes(ch)) {
      depth++;
    } else if (")}]".includes(ch)) {
      depth--;
    } else if (ch === sep && depth === 0) {
      parts.push(cur);
      cur = "";
      continue;
    }
    cur += ch;
  }
  if (cur.trim() !== "") parts.push(cur);
  return parts;
}

function splitKey(s) {
  const m = s.match(/^([A-Za-z0-9_-]+):(?: (.*))?$/);
  if (!m) throw new Error(`Cannot parse YAML line: ${s}`);
  return [m[1], m[2] ?? ""];
}

function value(raw) {
  const s = raw.trim();
  if (s.startsWith("{") && s.endsWith("}")) {
    const obj = {};
    for (const part of splitTopLevel(s.slice(1, -1))) {
      const [k, v] = splitKey(part.trim());
      obj[k] = value(v);
    }
    return obj;
  }
  if (s.startsWith("[") && s.endsWith("]")) {
    return splitTopLevel(s.slice(1, -1)).map(value);
  }
  return scalar(s);
}

export function parseCapabilitiesYaml(text) {
  const lines = text
    .split(/\r?\n/)
    .filter((l) => l.trim() !== "" && !l.trim().startsWith("#"))
    .map((l) => ({ indent: l.match(/^ */)[0].length, text: l.trim() }));
  let pos = 0;

  function parseBlock(indent) {
    return lines[pos].text.startsWith("- ") ? parseSeq(indent) : parseMap(indent);
  }

  function parseSeq(indent) {
    const out = [];
    while (pos < lines.length && lines[pos].indent === indent && lines[pos].text.startsWith("- ")) {
      const rest = lines[pos].text.slice(2);
      if (rest.startsWith("{")) {
        out.push(value(rest));
        pos++;
      } else {
        // `- key: v` opens a block mapping whose keys sit at indent + 2.
        lines[pos] = { indent: indent + 2, text: rest };
        out.push(parseMap(indent + 2));
      }
    }
    return out;
  }

  function parseMap(indent) {
    const obj = {};
    while (pos < lines.length && lines[pos].indent === indent && !lines[pos].text.startsWith("- ")) {
      const [k, v] = splitKey(lines[pos].text);
      pos++;
      if (v === "" && pos < lines.length && lines[pos].indent > indent) {
        obj[k] = parseBlock(lines[pos].indent);
      } else {
        obj[k] = value(v);
      }
    }
    if (pos < lines.length && lines[pos].indent > indent) {
      throw new Error(`Unexpected indentation at: ${lines[pos].text}`);
    }
    return obj;
  }

  const result = parseBlock(0);
  if (pos !== lines.length) throw new Error(`Unparsed YAML from: ${lines[pos].text}`);
  return result;
}
