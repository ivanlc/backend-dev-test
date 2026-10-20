# Backend dev technical test
We want to offer a new feature to our customers showing similar products to the one they are currently seeing. To do this we agreed with our front-end applications to create a new REST API operation that will provide them the product detail of the similar products for a given one. [Here](./similarProducts.yaml) is the contract we agreed.

We already have an endpoint that provides the product Ids similar for a given one. We also have another endpoint that returns the product detail by product Id. [Here](./existingApis.yaml) is the documentation of the existing APIs.

**Create a Spring boot application that exposes the agreed REST API on port 5000.**

![Diagram](./assets/diagram.jpg "Diagram")

Note that _Test_ and _Mocks_ components are given, you must only implement _yourApp_.

## Testing and Self-evaluation
You can run the same test we will put through your application. You just need to have docker installed.

First of all, you may need to enable file sharing for the `shared` folder on your docker dashboard -> settings -> resources -> file sharing.

Then you can start the mocks and other needed infrastructure with the following command.
```
docker-compose up -d simulado influxdb grafana
```
Check that mocks are working with a sample request to [http://localhost:3001/product/1/similarids](http://localhost:3001/product/1/similarids).

To execute the test run:
```
docker-compose run --rm k6 run scripts/test.js
```
Browse [http://localhost:3000/d/Le2Ku9NMk/k6-performance-test](http://localhost:3000/d/Le2Ku9NMk/k6-performance-test) to view the results.

## Evaluation
The following topics will be considered:
- Code clarity and maintainability
- Performance
- Resilience

## Implementation

This repository is a fork of [dalogax/backendDevTest](https://github.com/dalogax/backendDevTest).
The original API contracts, mocks, benchmark assets and Apache 2.0 license are preserved.
The application uses Java 21, Spring Boot and Maven.

```sh
mvn test
mvn spring-boot:run
```

The application listens on port 5000 by default. `PORT` and `UPSTREAM_BASE_URL`
override the listener and mock base URL; the latter defaults to
`http://localhost:3001`. The remaining timeout and concurrency settings are
listed below.

### Data flow

`SimilarProductsController` exposes `GET /product/{productId}/similar`.
`SimilarProductsService` first fetches similar IDs through
`HttpExistingProductsGateway`, then retrieves their details on a shared,
bounded worker pool. Each result is placed at its original ID position, so
completion order cannot change similarity order. The gateway validates the
upstream JSON and maps it to `ProductDetail` without inventing missing values.
No database, cache, authentication or retry layer is used.

### Run and test

Java 21 and Maven are required. Start the supplied mock service with Docker:

```sh
docker compose up -d simulado influxdb grafana
curl http://localhost:3001/product/1/similarids
```

Start the application in a second terminal, then query it in a third:

```sh
mvn spring-boot:run
```

```sh
curl -i http://localhost:5000/product/1/similar
```

Run automated tests and build the executable jar with:

```sh
mvn test
mvn package
```

To run the unchanged upstream load test, keep the application running on the
host at port 5000, then execute:

```sh
docker compose run --rm k6 run scripts/test.js
```

For a Docker-hosted application, set `UPSTREAM_BASE_URL` to a hostname that
resolves to the mock service from that container; the default `localhost:3001`
is intended for running the application directly on the host.

### Configuration

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `PORT` | `5000` | Application listener |
| `UPSTREAM_BASE_URL` | `http://localhost:3001` | Supplied upstream service |
| `UPSTREAM_CONNECT_TIMEOUT` | `500ms` | TCP connection limit |
| `UPSTREAM_READ_TIMEOUT` | `6s` | Per-upstream-response limit |
| `UPSTREAM_REQUEST_TIMEOUT` | `8s` | Overall orchestration deadline |
| `MAX_DETAIL_CONCURRENCY` | `64` | Shared active detail workers |
| `DETAIL_QUEUE_CAPACITY` | `1024` | Shared waiting detail tasks |
| `PER_REQUEST_PARALLELISM` | `8` | Maximum details in one batch |

The shared pool and bounded queue prevent unbounded task creation. A request
submits at most eight detail calls at a time. When a batch fails or expires,
remaining tasks are cancelled and queued tasks are removed. These limits are
starting values, not load-test-tuned values.

### Failure policy

The public OpenAPI contract documents 200 and 404. The following additional
responses are implementation assumptions for unspecified upstream failures.
Responses use JSON Problem Details and never include partial product arrays.

| Situation | Response |
| --- | --- |
| Similar IDs lookup succeeds with `[]` | `200 []` |
| Similar IDs or detail lookup returns 404 | `404` |
| Upstream 5xx, other HTTP error, connection failure or malformed JSON | `502` |
| Read timeout or overall request deadline | `504` |
| Detail worker queue is full | `503` |

The supplied mock returns numeric similar IDs despite `existingApis.yaml`
declaring strings. Integral IDs are normalized to strings. Duplicate IDs are
removed while preserving their first position. Missing fields, invalid field
types and mismatched product IDs are errors. A failing detail makes the whole
request fail; there are no retries or partial results.

### Verification and limits

Unit and HTTP integration tests cover normal and empty responses, completion
order, 404/5xx/malformed responses, timeout, overload and queued-task cleanup.
The integration tests use a local HTTP server, so they do not require Docker.
The supplied k6 scenarios include slow upstream responses, and some requests
can receive 503 or 504 under their 200-concurrent-user load; the implementation
does not promise every overloaded request will succeed. Run k6 in an environment
with a working Docker daemon to measure actual throughput and latency.
