# Contribution Guidelines

---

## 1. Branching Strategy

- `master` – always stable and deployable
- `dev` – integration branch for completed features
- `feat/<issue-number>-<short-description>` – used for developing individual features

Example: - `feat/7-login`

Direct pushes to `master` and `dev` are not allowed.
All changes must be submitted via Merge Requests.
Feature branches are created from `dev`. Once a feature is implemented and tested, a Merge Request is opened to merge it back into `dev`.
Before merging into `dev`, the code should be reviewed and checked to ensure that it does not break existing functionality.

---

## 2. Commit Message Convention

We follow the Conventional Commits format:

type(#issue number): short description

### Allowed Types

- `feat` – new feature
- `fix` – bug fix
- `refactor` – code restructuring without behavior change
- `test` – tests
- `docs` – documentation
- `chore` – build, CI, configuration

### Example

- feat(#20): implement JWT login
- fix(#53): prevent null pointer exception

Rules:
- Use imperative mood (e.g., “add”, not “added”)  

---

## 3. Definition of Done

A task is considered complete when:

- Code is implemented
- Changes are merged into `dev`
