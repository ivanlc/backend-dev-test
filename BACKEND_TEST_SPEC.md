# BACKEND_TEST_SPEC.md — Similar products API

## 1. Context and scope
Technical assessment source: https://github.com/dalogax/backendDevTest (branch `main`). The requested deliverable is a **Spring Boot backend** exposing the agreed REST operation on port **5000**. The repository already supplies service mocks and benchmarking resources; implement only the component described upstream as `yourApp`.

This assessment is independent from the React frontend exercise. Do not integrate them.

## 2. Source material (authoritative)
- `readme.md`: instructions and evaluation criteria — clarity/maintainability, performance, resilience.
- `similarProducts.yaml`: public contract to implement.
- `existingApis.yaml`: contracts for supplied upstream services.
- `docker-compose.yaml`: infrastructure (mock service, k6, InfluxDB, Grafana).
- `shared/`: mock definitions and k6/Grafana resources.

Read upstream files **before implementation**. Keep these resources and the original license/attribution.

## 3. Required behavior
### API exposed by our service
- `GET /product/{productId}/similar`
- Server base URL: `http://localhost:5000`
- `productId` is a required **string** path parameter.
- On success (`200`), return `application/json` with an **array of product detail objects ordered by similarity**, with zero or more elements. Each object contains:
  - `id`: nonempty string
  - `name`: nonempty string
  - `price`: number
  - `availability`: boolean
- The public OpenAPI contract also documents HTTP `404` (`Product Not found`).

### Supplied upstream APIs (mock service)
Base URL: `http://localhost:3001` when running directly on the host; make this configurable so that local/Docker execution works.
1. `GET /product/{productId}/similarids` → ordered array of product IDs (`string[]`).
2. `GET /product/{productId}` → detail for one product with `id`, `name`, `price`, `availability`; the upstream detail contract documents `200` and `404`.

Orchestration: fetch ordered similar IDs for requested product; fetch corresponding details; return the resulting details **in that original order**. For empty IDs, return an empty array on successful lookup. Do not sort alphabetically or by price.

## 4. Architecture proposal — validate before building
Suggested stack: **Java 21 + Spring Boot**, Maven or Gradle (choose one and justify). Favor these responsibilities:
- REST controller for the incoming endpoint.
- Service coordinating retrieval and preserving similarity order.
- HTTP client/gateway for the two external API calls.
- Explicit typed DTOs matching the OpenAPI properties.
- Centralized exception mapping and configurable HTTP timeouts.

Prefer no database and no extra frontend. Configuration should include listen port `5000`, upstream base URL, and concurrency/timeouts. Avoid excessive layers or framework additions.

## 5. Performance and resilience
Performance is an evaluation criterion. Product detail requests are independent after receiving IDs, so consider executing them concurrently with a **bounded** concurrency limit. Preserve output order regardless of completion order; avoid shared mutable result accumulation. Define and justify connection/read/overall timeouts and resource limits.

Resilience is also evaluated, but the source contracts **do not specify all failure policies**. Before coding, propose explicit, testable choices for:
- Error or timeout when requesting the list of similar IDs.
- One or more product-detail requests returning 404, 5xx or timing out.
- Missing upstream data, duplicate IDs, malformed responses, empty results.
- Whether partial results are ever returned, and whether to use retries (bounded and justified).

Do not invent a required behavior; flag decisions as implementation assumptions and document them. Keep responses deterministic and consistent with the published API contract.

## 6. Tests and verification
Required development checks:
- Unit tests for orchestration, ordering (including out-of-order upstream completion), empty response, error policies, DTO mapping.
- HTTP/controller tests for correct route, content type, shape, `200` and documented `404` behavior.
- Integration tests with mocked HTTP upstream for successful and failing requests; validate timeouts as practical.
- Java build/test command and static checks if configured.
- Docker environment and k6 performance exercise using the provided scripts; report observed metrics without inventing results.

The source README documents these commands (modern `docker compose` can substitute for `docker-compose`, subject to local tooling):

```sh
docker compose up -d simulado influxdb grafana
curl http://localhost:3001/product/1/similarids
# Start the Spring Boot app separately on port 5000.
docker compose run --rm k6 run scripts/test.js
```

The provided Grafana dashboard is referenced by upstream at `http://localhost:3000/d/Le2Ku9NMk/k6-performance-test`.

Check the mock is reachable before troubleshooting the application. Avoid changing k6/mock files to make a failing solution appear successful.

## 7. Delivery milestones
**M0 — Preparation (no application code):** verify GitHub auth and fork, inspect upstream source and docs, place `AGENTS.md` + this spec in the root, present architecture and open decisions, wait for approval.

**M1 — Bootstrap:** Spring Boot project, port 5000 and external configuration, README run instructions, basic smoke test.

**M2 — Functional behavior:** HTTP upstream clients, orchestration, typed responses, order preservation, successful and empty list cases.

**M3 — Reliability:** bounded concurrency, timeouts, error mapping and tests of documented assumptions.

**M4 — Quality and delivery:** unit/integration tests, run Docker/k6, address observable issues, improve README, report limitations and changes; user approves final pushes.

Prefer separate meaningful commits for successive milestones (the existing repo history should remain intact).

## 8. Definition of done
- Correct endpoint reachable at port `5000`.
- Response contract and order meet `similarProducts.yaml`.
- Can talk to supplied mock services.
- Handles empty list and the agreed failure policies.
- Compilation/tests run with documented commands.
- Supplied k6 benchmark has been executed, or the precise blocker is recorded.
- README describes setup, test commands, architecture, assumptions and attribution.
- No credentials or internal files committed.
- User can explain the data flow, concurrency and error-handling decisions.

## 9. First Codex instruction
In the local workspace, read these two files and the upstream repository files. Verify `gh auth status` and whether `ivanlc/backend-dev-test` exists; if it does not, create the requested fork and clone it, preserving upstream history. Do not overwrite existing work. Present the architecture, milestones and decisions needing approval **before implementing application code**. Explicitly wait for approval.
