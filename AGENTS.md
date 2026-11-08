# AGENTS.md

Guidance for AI coding agents working in this repository. This assumes Claude Code with the
[aihero.dev](https://www.aihero.dev/) skill set (`grilling`, `domain-modeling`, `tdd`, `codebase-design`,
`diagnosing-bugs`, `code-review`, `simplify`, `pr`, `research`, `prototype`, `wayfinder`,
`improve-codebase-architecture`) installed. Start with [README.md](./README.md) for what NL Portal is and
how the monorepo is laid out.

Issue tracker: GitHub Issues on this repo (`gh issue ...`), using native sub-issues and blocking
relationships. Any skill that needs to find, file, or link a ticket — `wayfinder`'s map and tickets
included — looks here.

## Hard rules

Read these before anything else; every other section is reference.

1. **Skill first.** Match the task to the table under "Workflow" and call that skill before acting.
2. **Eigen branch per taak.** Elke taak krijgt een eigen branch vanaf `main`, ook als je al op een
   andere branch staat; changes reach `main` only through a PR (see "Branches").
3. **Hooks pass.** Run `git commit` and `git push` directly and let the Husky hooks pass; fix the
   cause when one fails, never `--no-verify` (see "Git hooks and `pnpm`").
4. **Mise for tasks.** Run scripts and repeatable tasks as `mise run <task>`; add a task when none
   exists. `mise tasks` lists what exists.
5. **Done means green.** Build and tests pass per "Before you call it done"; a security- or
   dependency-relevant change also passes the matching `mise run security:*` task.
6. **PR title is a Conventional Commit** and non-trivial work starts from an issue (see
   "Contributing").

## Agent skills

### Issue tracker

Issues live as GitHub Issues on this repo (`gh issue ...`). See
[docs/agents/issue-tracker.md](docs/agents/issue-tracker.md).

### Triage labels

Default canonical labels (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`,
`wontfix`), plus `epic` for an umbrella issue split into sub-tickets: work its sub-tickets, never the
epic itself. See [docs/agents/triage-labels.md](docs/agents/triage-labels.md).

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

From the repo root:

```shell
mise run build                    # compile and assemble backend and frontend
mise run test                     # unit tests of backend, frontend and scripts, ArchUnit included; no integration tests (those need Docker)
mise run lint                     # ktlint/Spotless, ESLint, Stylelint, typecheck, Prettier
mise run backend:pitest-changed   # only when backend/core changed: CI's 80% mutation gate
```

Narrow a loop with a domain task (`backend:test`, `frontend:lint`, `arch-check` for only the ArchUnit
rules, ...); `mise tasks` lists them all. Integration tests run with
`mise run backend:integration-test` when Docker is there.

Gradle hangs without output: stop stale daemons with `mise exec -- ./gradlew --stop` from `backend/`
and rerun.
Send long runs to a log file instead of piping through `grep`/`head`, which buffers until the end.

For a security- or dependency-relevant change, also run the applicable `mise run security:*` task
(see below) before considering it done.

### Branches: never commit on `main`

Before the first edit or commit of any task, run `git branch --show-current` and `git status --short`.
Cut a branch for the task from `main` (`git switch -c <type>/<short-name>-<issue>`, e.g.
`chore/remove-case-module-103`). Staat de huidige branch voor een ander ticket, of bevat de werkboom
wijzigingen die niet van jou zijn (bijvoorbeeld van een andere sessie), werk dan in een worktree:
`git worktree add -b <branch> ../<map> main`. Stage alleen je eigen paden (nooit `git add <map>` op
een vuile werkboom) en controleer `git diff --cached --stat` vóór het commit. Types: `feat`, `fix`, `docs`, `refactor`, `test`, `build`, `ci` and
`chore`; maintenance tasks (removals, cleanup, dependency and tooling upkeep) use `chore/`.
Never commit to or push `main` directly, also not "just locally":
changes reach `main` only through a PR. If a commit already landed on `main` by mistake and is not
pushed, move it: `git branch <new-branch> && git reset --hard origin/main && git switch <new-branch>`.

### Git hooks and `pnpm`

Draai `git commit` en `git push` direct: de Husky-hooks onder `frontend/.husky` draaien `pnpm` en
Gradle zelf via `mise exec` wanneer mise aanwezig is, dus ook vanuit een shell zonder mise-activatie.
Gebruik nooit `--no-verify`. Python is niet geïnstalleerd; gebruik `mise exec -- node` voor scripts.

Documentatie, commitberichten en ADR's schrijf je in het Nederlands (domeintaal); gespreksoutput mag Engels zijn.

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
- Use the `mise run security:*`, `sbom:*` and `deps:outdated*` tasks (`mise.toml`) for these jobs
  instead of hand-rolling `gradle`/`syft`/`grype`/`pnpm` invocations — they're what CI runs too.
- After any backend dependency bump, including the weekly Renovate PR, manually regenerate and
  commit `backend/gradle/verification-metadata.xml` via `mise run security:verify-dependencies` —
  this is not automated, and CI fails closed until it's done.

## Contributing

Open an issue before a non-trivial PR, and give the PR title a valid
[Conventional Commit](https://www.conventionalcommits.org/) form — it becomes the squash-merge
message. See [CONTRIBUTING.md](CONTRIBUTING.md) for the full branch model and review process.
