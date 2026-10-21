# Similar Products API

## Overview

This Spring Boot service exposes `GET /product/{productId}/similar` on port `5000`. It fetches similar product IDs from an existing service, retrieves their details, and returns a JSON array ordered by similarity. An empty upstream ID list returns `[]`.

This repository is a fork of the [original backend assessment](https://github.com/dalogax/backendDevTest). The original [public OpenAPI contract](./similarProducts.yaml), [upstream API contract](./existingApis.yaml), Docker mocks, k6 assets, and Apache 2.0 license are preserved. Only the application component described by the assessment is implemented here.

## Tech Stack

- Java 21, Spring Boot 3.5, and Maven
- Spring Web `RestClient` with the JDK HTTP client
- JUnit 5, Spring Boot Test, and Mockito
- Supplied Docker Compose mocks, k6, InfluxDB, and Grafana

## Architecture

`SimilarProductsController` exposes the public route. `SimilarProductsService` coordinates the upstream calls through `HttpExistingProductsGateway`:

1. Fetch the ordered IDs from `GET /product/{productId}/similarids`.
2. Fetch each detail from `GET /product/{id}` using a shared, bounded executor.
3. Place each result at its original ID position, so completion order cannot change similarity order.

The executor has a fixed worker count and bounded queue. Each incoming request submits at most `PER_REQUEST_PARALLELISM` detail calls in a batch; later batches wait for earlier ones. On failure or deadline expiry, unfinished tasks are cancelled and queued tasks are removed where possible. No database, cache, or retry layer is used.

The supplied mock returns numeric similar IDs even though `existingApis.yaml` declares strings. Integral IDs are normalized to strings; duplicate IDs are removed while retaining their first position. Detail responses must contain valid `id`, `name`, `price`, and `availability` fields, and the returned ID must match the requested ID.

## Getting Started

Install Java 21, Maven, and Docker with Compose. Start the supplied mock and monitoring services:

```sh
docker compose up -d simulado influxdb grafana
curl http://localhost:3001/product/1/similarids
```

Start the application in another terminal:

```sh
mvn spring-boot:run
```

Then call the public endpoint:

```sh
curl -i http://localhost:5000/product/1/similar
```

The default upstream URL assumes the application runs on the host. If it runs in a container, set `UPSTREAM_BASE_URL` to an address reachable from that container.

## Configuration

All settings can be overridden with environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `PORT` | `5000` | HTTP listener |
| `UPSTREAM_BASE_URL` | `http://localhost:3001` | Existing API base URL |
| `UPSTREAM_CONNECT_TIMEOUT` | `500ms` | Connection timeout |
| `UPSTREAM_READ_TIMEOUT` | `6s` | Per-response read timeout |
| `UPSTREAM_REQUEST_TIMEOUT` | `8s` | Detail orchestration deadline |
| `MAX_DETAIL_CONCURRENCY` | `256` | Shared detail worker count |
| `DETAIL_QUEUE_CAPACITY` | `1024` | Shared waiting-task capacity |
| `PER_REQUEST_PARALLELISM` | `8` | Maximum detail calls submitted per batch |

## Error Handling

The public OpenAPI contract documents `200` and `404`. The additional statuses below are explicit implementation choices for upstream failures. Errors use JSON Problem Details; a failed detail request never produces a partial product array.

| Condition | HTTP response |
| --- | --- |
| Successful lookup with no similar IDs | `200 []` |
| Similar-ID or detail lookup returns 404 | `404` |
| Other upstream HTTP error, connection failure, or invalid payload | `502` |
| Upstream read timeout or orchestration deadline | `504` |
| Detail executor rejects work at capacity | `503` |

Timeouts and queue limits bound resource use, but overload can still produce `503` or `504`. There are no automatic retries.

## Testing & Performance

Run compilation, unit tests, HTTP integration tests, and packaging with:

```sh
mvn clean verify
```

The suite currently passes 23 tests covering response shape, ordering despite out-of-order completion, empty results, upstream failures, timeouts, overload, and task cleanup. Its HTTP integration tests use a local test server and do not require Docker.

With the Docker services and application running, execute the unchanged supplied load script:

```sh
docker compose run --rm k6 run scripts/test.js
```

Grafana results are available at [the supplied dashboard](http://localhost:3000/d/Le2Ku9NMk/k6-performance-test). The script runs five 200-VU scenarios, including expected 404, 502, and timeout responses.

In the recorded run, k6 made **11,977 requests** at **178.42 requests/s** with **460 ms mean** and **5.10 s p95** latency overall. Outcomes were **3,601 HTTP 200**, **3,977 HTTP 404**, **3,999 HTTP 502**, **400 HTTP 504**, and **0 HTTP 503**; no observed status was unexpected for its scenario. The slow-detail scenario returned **400/400 HTTP 200** responses, while the very-slow scenario returned **400 HTTP 504** responses.

These figures describe one local run of the supplied workload, not a general latency or availability guarantee. Different or heavier traffic can fill the bounded queue or exceed the request deadline.
