# Banking Transactions API

Spring Boot 3 / Java 21 in-memory REST API for banking transaction management.

## Frontend shell

A single self-contained page at [`src/main/resources/static/index.html`](src/main/resources/static/index.html)
is served automatically at **http://localhost:8080/** once the app is running
(`mvn spring-boot:run`). It has two tabs:

- **Transactions** — create a transaction, list/filter them, and look up an
  account's balance/summary, calling the endpoints below.
- **Pipeline** — trigger a run of the multi-agent banking pipeline (see the
  section further down) and view its summary tiles and per-transaction results.

No build step, no framework, no external CDN — plain HTML/CSS/JS talking to the
same-origin REST endpoints via `fetch`.

## Prerequisites

- **Java 21** (JDK 21) — records require Java 16+; Spring Boot 3.x requires Java 17+
- **Maven 3.6+**

## Build

```powershell
# Set JAVA_HOME to JDK 21 (required if JAVA_HOME points to an older JDK)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:PATH = "C:\Program Files\Java\jdk-21\bin;" + $env:PATH

# Compile
mvn compile "-Dmaven.wagon.http.ssl.insecure=true" "-Dmaven.wagon.http.ssl.allowall=true"

# Run tests
mvn test "-Dmaven.wagon.http.ssl.insecure=true" "-Dmaven.wagon.http.ssl.allowall=true"

# Run the application
mvn spring-boot:run "-Dmaven.wagon.http.ssl.insecure=true" "-Dmaven.wagon.http.ssl.allowall=true"
```

The `-Dmaven.wagon.http.ssl.insecure=true` flag is only needed if your JVM truststore doesn't include the Maven Central CA certificate.

## Package Structure

| Package                  | Purpose                                                  |
|--------------------------|----------------------------------------------------------|
| `banking`                | Application entry point (`BankingTransactionsApiApplication`) |
| `banking.controller`     | Spring MVC `@RestController` classes                     |
| `banking.service`        | Business logic services                                  |
| `banking.domain`         | Core domain entity (`Transaction` record)                |
| `banking.store`          | In-memory `ConcurrentHashMap`-backed storage             |
| `banking.validation`     | Input validation component                               |
| `banking.dto`            | Request and response DTO records                         |
| `banking.exception`      | Custom exception classes                                 |

## Endpoints

| Method | Path                              | Description              |
|--------|-----------------------------------|--------------------------|
| POST   | `/transactions`                   | Create a transaction     |
| GET    | `/transactions`                   | List transactions        |
| GET    | `/transactions/{id}`              | Get transaction by ID    |
| GET    | `/accounts/{accountId}/balance`   | Get account balance      |
| GET    | `/accounts/{accountId}/summary`   | Get account summary      |

---

## Multi-Agent Banking Transaction Pipeline

A separate, self-contained pipeline (see [../specification.md](../specification.md),
[../design.md](../design.md) and [../implementation-plan.md](../implementation-plan.md))
that validates, scores, and settles transactions from `sample-transactions.json`
through a file-based message protocol. It lives entirely under `banking.pipeline`
and does not depend on the REST API above.

### Pipeline package structure

| Package                     | Purpose                                                        |
|------------------------------|-----------------------------------------------------------------|
| `banking.pipeline`            | `PipelineOrchestrator` (chains the agents) and `Integrator` (CLI entry point) |
| `banking.pipeline.domain`     | `TransactionRecord`, `ProcessingMessage`, `ProcessingResult`, `FraudAssessment`, `PipelineSummary`, `AuditInfo` |
| `banking.pipeline.agent`      | `TransactionValidatorAgent`, `FraudDetectorAgent`, `ComplianceSettlementAgent`, `ReportingAgent` |
| `banking.pipeline.io`         | `SharedDirectoryService` — creates/resets `shared/{input,processing,output,results}` and reads/writes JSON messages |
| `banking.pipeline.validation` | `IsoCurrencyCodes` — supported ISO 4217 currency codes |

### Pipeline flow

```text
sample-transactions.json
        |
        v
Integrator  --writes--> shared/input/*.json   (ProcessingMessage envelopes)
        |
        v
TransactionValidatorAgent --writes--> shared/processing/*.json
        |                                   (rejected transactions stop here
        |                                    and are copied straight to shared/results/)
        v
FraudDetectorAgent --writes--> shared/output/*.json  (FraudAssessment)
        |
        v
ComplianceSettlementAgent --writes--> shared/results/*.json  (approved | flagged)
        |
        v
ReportingAgent --writes--> shared/results/pipeline-summary.json
```

### Running the pipeline over HTTP

`banking.pipeline.web.PipelineController` exposes the same orchestrator used by
`Integrator` as REST endpoints, so the frontend shell (or `curl`) can drive it
without a CLI:

| Method | Path                     | Description                                           |
|--------|--------------------------|--------------------------------------------------------|
| POST   | `/api/pipeline/run`      | Runs the full pipeline, returns the `PipelineSummary`   |
| GET    | `/api/pipeline/summary`  | Returns the last run's summary (404 if none yet)        |
| GET    | `/api/pipeline/results`  | Lists every per-transaction `ProcessingResult`          |

Configurable via `pipeline.shared-dir` / `pipeline.input-file` in
`application.properties` (both default to the same resolution `Integrator` uses).

### Running the pipeline from the command line

The pipeline has a plain `main()` method and only needs Jackson on the classpath
(already a transitive dependency of `spring-boot-starter-web`), so any of the
following work:

```powershell
# Option 1 — via Spring Boot's exec support (requires network access for the plugin itself)
mvn spring-boot:run "-Dspring-boot.run.mainClass=banking.pipeline.Integrator" "-Dspring-boot.run.arguments=../sample-transactions.json,shared"

# Option 2 — build a classpath file once, then invoke java directly
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/classes;$(cat cp.txt)" banking.pipeline.Integrator ../sample-transactions.json shared

# Option 3 — from an IDE, just run banking.pipeline.Integrator.main()
```

Arguments are optional: `Integrator [input-file] [shared-dir]`. With no arguments
it looks for `sample-transactions.json` in the current directory, then in the
parent directory, and writes to `./shared/`.

Sample output:

```
Pipeline run complete. Results written to .../shared/results
Total: 8 | Approved: 5 | Flagged: 1 | Rejected: 2

Transactions requiring attention:
  - TXN005 [flagged] flagged for manual review: very high value transfer, wire transfer channel
  - TXN006 [rejected] currency 'XYZ' is not a supported ISO 4217 code
  - TXN007 [rejected] amount must be greater than zero
```

### Tests

```powershell
mvn test -Dtest="banking.pipeline.**"
```

Covers each agent in isolation (validator, fraud detector, compliance/settlement,
reporting), the shared-directory JSON protocol, and a full end-to-end integration
test against the real `sample-transactions.json` — including a re-run check that
confirms `shared/` is reset and never accumulates stale files between runs.
