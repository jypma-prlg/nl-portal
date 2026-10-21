# Contributing to NL Portal

Thank you for your interest in contributing to NL Portal. This repository is a monorepo containing
the backend libraries + application, the frontend libraries + application, the local development
stack and the documentation. Backend and frontend release at a single, shared version.

## Ownership

This project is owned and maintained by pinkroccade. It was originally built by
[Ritense](https://ritense.com/) together with its community; this repository is now developed
independently.

## How to contribute

1. **Open an issue first.** Every pull request needs a linked issue in this repository's own
   Issues: an RFC for a bigger feature, a bug report or task for smaller work. For anything beyond
   a small fix, let a maintainer weigh in before you start building. A pull request without a
   linked issue may be closed without review. Typos, documentation fixes and dependency bumps are
   exempt.
2. **Fork, or branch.** External contributors work on a branch in their own fork; contributors
   with write access branch directly in this repository. See [Branch model](#branch-model) for
   which line your change starts from and how to name the branch.
3. **Write the code**, with tests, following the style of the surrounding code.
4. **Open the pull request** against the branch your change targets. Filling in the pull request
   template is mandatory, and the title must be a valid
   [Conventional Commit](https://www.conventionalcommits.org/) because it becomes the
   squash-merge commit. Link the issue in the `Closes #` line.
5. A maintainer reviews. Once approved and green, it is squash-merged.

## Branch model

NL Portal follows a Spring-style branch model:

- **`main`** - the development trunk and default branch. All new work targets `main`.
- **`release/<major>.x`** - a long-lived maintenance line per major version (for example
  `release/3.x`, `release/4.x`). Minor and patch releases are **tags** on these branches. At most
  two lines are kept alive at once: the current major and the previous one.
- **`rc/*`** - ephemeral release-candidate branches cut from `main` when preparing a release.

Contributor branches are cut from `main`:

- **`feature/*`** - new functionality (optionally split into short-lived `story/*` branches that
  merge back into the feature branch).
- **`bugfix/*`** - bug fixes targeting `main`.
- **`hotfix/*`** - urgent patches for a `release/<major>.x` line that cannot wait for the next
  version. A hotfix is authored on the maintenance line and then propagated as described in
  [Fix propagation](#fix-propagation).

### Fix propagation

A fix must reach every supported line it affects. How it travels depends on the major:

- **Current major** (the major `main` is still developing): author the fix on its
  `release/<major>.x` line, then **merge that line up into `main`**. The merge preserves the fix
  commit, which keeps the release line fast-forwardable at the next release. Maintainers perform
  this merge.
- **Previous major** (the older maintenance line): the fix stays on that line. If the same bug also
  affects the current major, it is **cherry-picked and adapted** onto the current line (and then
  follows the rule above). The two maintenance lines are otherwise independent.
- The one merge that is **not** allowed is merging a previous-major line into `main` - their
  histories have diverged too far, so such fixes are re-applied, not merged.

Security fixes must reference the relevant vulnerability identifier **in the fix commit on each
line** they apply to - that reference is how a vulnerability is confirmed patched everywhere it
matters.

## Commit messages

This repository uses [Conventional Commits](https://www.conventionalcommits.org/). Format:

```
<type>(<optional scope>): <description>
```

Common types: `feat`, `fix`, `docs`, `refactor`, `test`, `build`, `ci`, `chore`. Example:

```
fix(zgw): handle empty zaak result in case overview
```

Because pull requests are **squash-merged**, the PR title is what lands on the branch - make it a
valid Conventional Commit.

## Building and testing locally

- **Backend**: from `backend/`, `./gradlew build` (integration tests auto-provision a database via
  the docker-compose plugin; Docker must be running).
- **Frontend**: from `frontend/`, `pnpm install` then `pnpm -r build`, `pnpm test`, `pnpm lint` and
  `pnpm prettier`.

See `backend/README.md`, `frontend/README.md` and `docker-compose/README.md` for details.

## Copyright headers

Every backend `.kt` / `.kts` and frontend `.ts` / `.tsx` / `.js` / `.jsx` file
carries the EUPL 1.2 header. Enforcement:

- Backend: `./gradlew spotlessCheck` (CI-blocking, part of `check` / `build`).
  Apply locally with `./gradlew spotlessApply`.
- Frontend: `cd frontend && pnpm lint` (CI-blocking in `_stage-fe.yml`, husky
  pre-commit via lint-staged, husky pre-push via `.husky/pre-push`). Apply /
  auto-repair locally with `pnpm lint:fix`.

The frontend quality gate is normally Prettier, not ESLint. The `notice/notice`
rule is an intentional exception because ESLint is the only tool with a fixable,
dynamic-year header rule that plugs into the existing husky / lint-staged wiring.
`eslint-config-prettier` is included in the flat config so ESLint's stylistic
rules cannot collide with Prettier.

The header-consolidation rewrite is listed in `.git-blame-ignore-revs`. GitHub's
blame view and most IDEs (IntelliJ, VS Code + GitLens) honor this file
automatically. If your tooling doesn't, run
`git config blame.ignoreRevsFile .git-blame-ignore-revs` once locally so
`git blame` skips the rewrite.

## Code of Conduct

By participating you agree to abide by our [Code of Conduct](./CODE_OF_CONDUCT.md).

## Security

Please report security issues privately - see the [Security Policy](./SECURITY.md). Do not open a
public issue for vulnerabilities.
