# AGENTS.md — Backend technical assessment

## Goal and source of truth
Build the backend assessment from https://github.com/dalogax/backendDevTest, under the user's GitHub account `ivanlc`. This is a **separate exercise** from `frontend-dev-test`; do not attempt to integrate the two applications.

Read, in order:
1. `BACKEND_TEST_SPEC.md` (working specification and acceptance criteria).
2. The upstream repository's `readme.md`.
3. Upstream `similarProducts.yaml` and `existingApis.yaml` (authoritative API contracts).
4. Upstream `docker-compose.yaml` and files in `shared/` (provided mocks and test setup).

If this file or the spec conflicts with the actual upstream API contracts, report the discrepancy and follow the upstream contracts after getting clarification. Do not invent requirements or change supplied test resources casually.

## Initial setup — before code changes
1. Verify the local tools are available: `git`, `gh`, Java/JDK, build tools and Docker as needed. Check `gh auth status`; never display or ask for a token.
2. Check whether `ivanlc/backend-dev-test` already exists, and whether the current working directory has uncommitted changes. **Never overwrite or recreate an existing repository or delete user files.**
3. If the fork does not exist, create a public fork of `dalogax/backendDevTest` in `ivanlc` and clone it locally using `gh` (fork creation is expressly requested by the user). If the desired repository name is not supported or is already occupied, stop and ask; don't silently create an unrelated repo.
4. Preserve original repository history, contract files, supplied Docker/test assets and LICENSE. Attribute the upstream source in the README.
5. Ensure this `AGENTS.md` and `BACKEND_TEST_SPEC.md` are present in the fork's root. If working in an existing clone, first check remotes and repository identity.
6. Analyze the exercise and present: proposed architecture, class/module responsibilities, error policy alternatives, concurrency strategy, test strategy, implementation milestones and any open questions. **Stop and wait for explicit approval before implementing application code.**

## Development rules (after plan approval)
- Work incrementally and keep commits meaningful (bootstrap, API clients, orchestration, tests, documentation). Describe changes and validation after each milestone.
- Only implement the required backend API, not a user interface, database, authentication or unrelated features.
- Prefer straightforward maintainable Java 21 / Spring Boot patterns. Avoid speculative abstractions and unnecessary dependencies.
- Respect port `5000`, endpoint, method, response schema, and similarity order exactly.
- Consider bounded concurrency and connect/read/request timeouts. Avoid unbounded parallelism and hiding failures silently.
- Explicitly discuss ambiguities (e.g. partial upstream failure, similar IDs endpoint 404, duplicate IDs, empty list, error responses). State the chosen behavior in the README and test it; don't present choices as if required by the contract.
- Keep upstream mocks and benchmark configuration intact unless a demonstrable compatibility fix is needed and explained.
- Implement unit and integration tests; run the available k6 test if Docker can run. Never claim a command passed unless actually executed.
- Include a clear README with run/test commands, assumptions, architecture and known limitations.
- Do not publish a final deliverable, open a PR, modify unrelated repositories, or push application code without the user's explicit approval. Creation of the requested public fork is authorized; additional pushes require approval.
- At milestone completion, explain the key architectural decisions and the relevant code paths in Spanish so the user can defend the solution in an interview.

## Quality gate
Before declaring completion, verify (or identify what could not be verified): compilation, unit tests, integration tests, API contract, result order, empty list, errors/timeouts, Docker mock connectivity, k6 load exercise, README and clean Git status. Record exact commands and outcomes.
