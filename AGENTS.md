# AGENTS.md

Guidance for AI coding agents working in this repository. This assumes Claude Code with the
[aihero.dev](https://www.aihero.dev/) skill set (`grilling`, `domain-modeling`, `tdd`, `codebase-design`,
`diagnosing-bugs`, `code-review`, `simplify`, `pr`, `research`, `prototype`, `wayfinder`,
`improve-codebase-architecture`) installed. Start with [README.md](./README.md) for what NL Portal is and
how the monorepo is laid out.

Issue tracker: GitHub Issues on this repo (`gh issue ...`), using native sub-issues and blocking
relationships. Any skill that needs to find, file, or link a ticket — `wayfinder`'s map and tickets
included — looks here.

## Agent skills

### Issue tracker

Issues live as GitHub Issues on this repo (`gh issue ...`). See
[docs/agents/issue-tracker.md](docs/agents/issue-tracker.md).

### Triage labels

Default canonical labels (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`,
`wontfix`). See [docs/agents/triage-labels.md](docs/agents/triage-labels.md).

### Domain docs

Single-context: `GLOSSARY.md` + `site/development/adr/` at the repo root. See
[docs/agents/domain.md](docs/agents/domain.md).

## Workflow: use the matching skill

Don't freelance a task a skill already covers. Before acting, match the situation below and call that
skill first:

| Situation | Skill |
|---|---|
| Stress-testing a design or architecture decision before building it | `grilling` |
| Sharpening domain language in `GLOSSARY.md`, or writing an ADR | `domain-modeling` |
| Designing or deepening a module's interface | `codebase-design` |
| Scanning the codebase for deepening/architecture-improvement candidates | `improve-codebase-architecture` |
| Building a feature or fixing a bug | `tdd` |
| Investigating a hard bug or a performance regression | `diagnosing-bugs` |
| Reviewing changes before opening a PR | `code-review` (`security-review` for security-sensitive changes) |
| A quality pass that isn't a bug hunt | `simplify` |
| Writing the PR description | `pr` |
| Researching external facts or docs before deciding | `research` |
| Sanity-checking a design question with a throwaway prototype | `prototype` |

This is a **must**, not a default — the point is to avoid ad hoc changes. The only exception is a
trivial, mechanical change (a typo, a documentation fix, a dependency bump), matching the
exemption in [CONTRIBUTING.md](./CONTRIBUTING.md). If nothing in the table fits, don't improvise
either: run `research` or `grilling` first to pin down the approach, then build it.

## Adding new functionality

Start from [site/development/workflows/spec-driven-development.md](site/development/workflows/spec-driven-development.md)
(Dutch) rather than jumping straight to code — it ties the spec, threat-model, build and
architecture-doc-update steps together. Within that workflow: the build step is a `tdd` seam, and any
step that touches `GLOSSARY.md` or an ADR is a `domain-modeling` seam.

## Before you call it done

```shell
# backend, from backend/
./gradlew build -x integrationTest   # compiles, unit-tests, ktlint; integrationTest needs Docker

# frontend, from frontend/
pnpm -r build && pnpm lint && pnpm lint:css && pnpm typecheck && pnpm test && pnpm prettier
```

For a security- or dependency-relevant change, also run the applicable `mise run security:*` task
(see below) before considering it done.

### Branches: never commit on `main`

Before the first edit or commit of any task, run `git branch --show-current`. If it prints `main`,
create a branch first (`git switch -c <type>/<short-name>-<issue>`, e.g.
`chore/remove-case-module-103`). Never commit to or push `main` directly, also not "just locally":
changes reach `main` only through a PR. If a commit already landed on `main` by mistake and is not
pushed, move it: `git branch <new-branch> && git reset --hard origin/main && git switch <new-branch>`.

### Git hooks and `pnpm`

The Husky hooks under `frontend/.husky` (pre-commit, pre-push) need `pnpm`, which a plain shell
often lacks on `PATH` even though `mise` provides it. A "pnpm: command not found" failure means
run git through mise: `mise exec -- git commit ...` / `mise exec -- git push ...`. Never use
`--no-verify`. If `mise exec` hangs (it may try to install every tool in `mise.toml`), put the
installed tool on `PATH` instead: `PATH="$(mise where pnpm):$PATH" git commit ...`. Gradle needs
`JAVA_HOME="$(mise where java)"`.

## Repo-indeling

Geen root-`package.json`/`node_modules`. Node-tooling (Husky, lint-staged, commitlint,
pnpm-workspace) leeft onder `frontend/`. Backend-tooling (Gradle, Spotless/ktlint) leeft onder
`backend/`. Zoek dus per ecosysteem, niet op repo-root.

## Architecture & domain

- [site/architectuur/arc42](site/architectuur/arc42) — arc42 architecture (Dutch); [site/likec4](site/likec4) — the C1/C2/C3
  model (`mise run likec4:start` to preview it).
- [GLOSSARY.md](GLOSSARY.md) — canonical domain terminology (Dutch), kept free of implementation
  details. Use `domain-modeling` to sharpen a term when its usage drifts from this, or to add one
  when a module gets named after it.
- [site/development/adr](site/development/adr) — architecture decision records. Only write one when the decision is hard to
  reverse, surprising without context, and the result of a real trade-off (see
  [site/development/adr/index.md](site/development/adr/index.md)); use `domain-modeling` to drive it.
- All other documentation lives in one VitePress site under [site/](site/) (`mise run docs:dev` to
  preview it): [Architectuur](site/architectuur/index.md), [Gebruik & inrichting](site/gebruik-en-inrichting/index.md),
  [Hosting & configuratie](site/hosting-en-configuratie/index.md), [Development](site/development/index.md)
  and, footer-only, [Compliance](site/compliance/index.md). Look there once something isn't in this file.

## Dependencies & security

- [site/development/workflows/dependency-updates.md](site/development/workflows/dependency-updates.md) and
  [site/development/workflows/security.md](site/development/workflows/security.md) (Dutch) are the source of truth; see also
  [SECURITY.md](SECURITY.md) for vulnerability disclosure.
- Use the `mise run security:*`, `sbom:*` and `deps:check*` tasks (`mise.toml`) for these jobs
  instead of hand-rolling `gradle`/`syft`/`grype`/`pnpm` invocations — they're what CI runs too.
- After any backend dependency bump, including the weekly Renovate PR, manually regenerate and
  commit `backend/gradle/verification-metadata.xml` via `mise run security:verify-dependencies` —
  this is not automated, and CI fails closed until it's done.

## Contributing

Open an issue before a non-trivial PR, and give the PR title a valid
[Conventional Commit](https://www.conventionalcommits.org/) form — it becomes the squash-merge
message. See [CONTRIBUTING.md](CONTRIBUTING.md) for the full branch model and review process.
