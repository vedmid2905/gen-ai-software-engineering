# Banking Transaction Processing Pipeline

Created by **Kostiantyn Vedmid**

## What this is

This project is an AI-generated multi-agent banking transaction pipeline built for the Homework 6 capstone. It takes raw transaction records from `sample-transactions.json` and runs each one through a chain of cooperating agents — validation, fraud scoring, and compliance/settlement — before writing a structured, auditable result and a run-wide summary. Agents exchange messages as JSON files through `shared/` directories, so every step of the pipeline is independently inspectable and testable.

Alongside the pipeline, the same Spring Boot application also hosts a small in-memory Transactions REST API (deposit/withdrawal/transfer, balances, summaries) and a single-page frontend shell that drives both the API and the pipeline from a browser.

## Agent responsibilities

- **Transaction Validator Agent** — checks required fields, positive `BigDecimal` amounts, ISO 4217 currency codes, and account reference format; rejects malformed transactions with a reason.
- **Fraud Detector Agent** — scores every validated transaction for risk using amount thresholds ($10k / $50k), off-hours timing, cross-border metadata, and wire-transfer channel, producing a 0–100 score and a low/medium/high label.
- **Compliance & Settlement Agent** — takes the fraud assessment and decides the final outcome: `approved` or `flagged` for manual review, with a reason.
- **Reporting Agent** — aggregates every final result into a `PipelineSummary` (total/approved/flagged/rejected) and writes it to `shared/results/pipeline-summary.json`.
- **Integrator / PipelineOrchestrator** — loads `sample-transactions.json`, resets the `shared/` directories so every run starts clean, and drives the transactions through the chain above.

## Pipeline architecture

```text
                     sample-transactions.json
                              |
                              v
                    +-------------------+
                    |     Integrator    |   loads input, resets shared/,
                    | (PipelineOrchestrator) drives the chain below
                    +-------------------+
                              |
                              v  writes ProcessingMessage envelopes
                     shared/input/*.json
                              |
                              v
              +-------------------------------+
              |  Transaction Validator Agent   |
              +-------------------------------+
                              |
                 rejected? ---+--- yes --> shared/results/*.json  (status: rejected)
                              |
                              no
                              v  writes ProcessingResult
                     shared/processing/*.json
                              |
                              v
                  +-----------------------+
                  |  Fraud Detector Agent |
                  +-----------------------+
                              |
                              v  writes FraudAssessment
                     shared/output/*.json
                              |
                              v
              +----------------------------------+
              | Compliance & Settlement Agent     |
              +----------------------------------+
                              |
                              v  writes ProcessingResult (approved | flagged)
                     shared/results/*.json
                              |
                              v
                    +-------------------+
                    |  Reporting Agent  |
                    +-------------------+
                              |
                              v
              shared/results/pipeline-summary.json
              (total / approved / flagged / rejected)
```

## Tech stack

| Layer | Technology |
|---|---|
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.3 (Spring Web, embedded Tomcat) |
| Build | Maven |
| JSON | Jackson (snake_case wire format for pipeline messages) |
| Money | `java.math.BigDecimal` throughout — never `float`/`double` |
| Testing | JUnit 5, AssertJ, Mockito, jqwik (property-based), Spring `MockMvc` |
| Coverage | JaCoCo (line coverage gate ≥ 80%) |
| Frontend | Plain HTML/CSS/JS (no build step, no framework), served as a Spring Boot static resource |
| Agent tooling | Claude Code — skills (`write-spec`, `run-pipeline`, `validate-transactions`), a coverage-gate hook, and this specification-first workflow |

## Where things live

- `specification.md`, `design.md`, `implementation-plan.md`, `agents.md` — the spec-first documents this project was built from.
- `src/` — the Maven project (`src/src/main/java/banking/...`). See [`src/README.md`](src/README.md) for the full package layout and REST/pipeline endpoint reference.
- `sample-transactions.json` — the pipeline's input fixture.
- `.claude/commands/` — `write-spec.md`, `run-pipeline.md`, `validate-transactions.md` skills.
- `.claude/hooks/check-coverage.sh` + `.claude/settings.json` — the coverage-gate hook that blocks `git push` when JaCoCo line coverage drops below 80%.

See [HOWTORUN.md](HOWTORUN.md) for step-by-step setup and demo instructions.
