# AGENTS.md

Guidance for AI coding agents working in this repository. This assumes Claude Code with the
[aihero.dev](https://www.aihero.dev/) skill set (`grilling`, `domain-modeling`, `tdd`, `codebase-design`,
`diagnosing-bugs`, `code-review`, `simplify`, `pr`, `research`, `prototype`, `wayfinder`,
`improve-codebase-architecture`) installed. Start with [README.md](./README.md) for what NL Portal is and
how the monorepo is laid out.

Issue tracker: GitHub Issues on this repo (`gh issue ...`), using native sub-issues and blocking
relationships. Any skill that needs to find, file, or link a ticket — `wayfinder`'s map and tickets
included — looks here.

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

Start from [docs/workflows/spec-driven-development.md](docs/workflows/spec-driven-development.md)
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

## Architecture & domain

- [docs/arc42](docs/arc42) — arc42 architecture (Dutch); [docs/likec4](docs/likec4) — the C1/C2/C3
  model (`mise run likec4:start` to preview it).
- [GLOSSARY.md](GLOSSARY.md) — canonical domain terminology (Dutch), kept free of implementation
  details. Use `domain-modeling` to sharpen a term when its usage drifts from this, or to add one
  when a module gets named after it.
- [docs/adr](docs/adr) — architecture decision records. Only write one when the decision is hard to
  reverse, surprising without context, and the result of a real trade-off (see
  [docs/adr/README.md](docs/adr/README.md)); use `domain-modeling` to drive it.

## Dependencies & security

- [docs/workflows/dependency-updates.md](docs/workflows/dependency-updates.md) and
  [docs/workflows/security.md](docs/workflows/security.md) (Dutch) are the source of truth; see also
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
