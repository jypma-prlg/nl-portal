// Pure logic backing `mise run release:cut-rc` (see docs/adr/0004-changelog-release-proces.md).
// No git, filesystem or network I/O here - see release-cut-rc.mjs for that.

const CONVENTIONAL_COMMIT = /^(\w+)(?:\(([^)]+)\))?(!)?:\s*(.+)$/;
const BREAKING_FOOTER = /^BREAKING CHANGE:/m;

export function parseConventionalCommit({ hash, subject, body = "" }) {
  const match = subject.match(CONVENTIONAL_COMMIT);
  if (!match) return null;
  const [, type, scope, bang, description] = match;
  const breaking = Boolean(bang) || BREAKING_FOOTER.test(body);
  return { hash, type, scope: scope ?? null, breaking, description };
}

export function bumpFor(commit) {
  if (commit.breaking) return "major";
  if (commit.type === "feat") return "minor";
  if (commit.type === "fix" || commit.type === "refactor" || commit.type === "perf") return "patch";
  return "none";
}

const BUMP_RANK = { none: 0, patch: 1, minor: 2, major: 3 };

export function nextBump(commits) {
  return commits.reduce((best, commit) => {
    const bump = bumpFor(commit);
    return BUMP_RANK[bump] > BUMP_RANK[best] ? bump : best;
  }, "none");
}

export function applyBump(version, bump) {
  const [major, minor, patch] = version.split(".").map(Number);
  if (bump === "major") return `${major + 1}.0.0`;
  if (bump === "minor") return `${major}.${minor + 1}.0`;
  if (bump === "patch") return `${major}.${minor}.${patch + 1}`;
  return null;
}

const CATEGORY_BY_TYPE = { feat: "Added", fix: "Fixed", refactor: "Changed", perf: "Changed" };

function formatEntry({ scope, description, hash }) {
  const text = scope ? `**${scope}**: ${description}` : description;
  return hash ? `${text} (\`${hash}\`)` : text;
}

export function categorize(commits) {
  const sections = { Breaking: [], Added: [], Changed: [], Fixed: [] };
  for (const commit of commits) {
    if (commit.breaking) {
      sections.Breaking.push(formatEntry(commit));
      continue;
    }
    const category = CATEGORY_BY_TYPE[commit.type];
    if (category) sections[category].push(formatEntry(commit));
  }
  return sections;
}

const SECTION_ORDER = ["Breaking", "Added", "Changed", "Fixed"];
const SECTION_HEADING = {
  Breaking: "⚠ BREAKING CHANGES",
  Added: "Added",
  Changed: "Changed",
  Fixed: "Fixed",
};

export function renderSection(version, date, sections) {
  const lines = [`## [${version}] - ${date}`];
  for (const name of SECTION_ORDER) {
    const items = sections[name];
    if (!items.length) continue;
    lines.push("", `### ${SECTION_HEADING[name]}`, "");
    for (const item of items) lines.push(`- ${item}`);
  }
  return lines.join("\n") + "\n";
}

const HEADER = `# Changelog

All notable changes to NL Portal are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/). Sections are generated from Conventional
Commits by \`mise run release:cut-rc\` (see [ADR 0004](docs/adr/0004-changelog-release-proces.md))
at the moment a release is cut - not maintained incrementally between releases.`;

export function insertSection(existingContent, section) {
  if (!existingContent) return `${HEADER}\n\n${section}`;
  const index = existingContent.indexOf("## [");
  if (index === -1) return `${existingContent.trimEnd()}\n\n${section}`;
  return `${existingContent.slice(0, index)}${section}\n${existingContent.slice(index)}`;
}

export function extractSection(changelogContent, version) {
  const heading = `## [${version}]`;
  const start = changelogContent.indexOf(heading);
  if (start === -1) return null;
  const afterHeadingLine = changelogContent.indexOf("\n", start) + 1;
  const nextHeading = changelogContent.indexOf("\n## [", afterHeadingLine);
  const body =
    nextHeading === -1
      ? changelogContent.slice(afterHeadingLine)
      : changelogContent.slice(afterHeadingLine, nextHeading);
  return body.trim() + "\n";
}
