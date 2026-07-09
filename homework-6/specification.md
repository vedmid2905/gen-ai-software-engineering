# Banking Transaction Processing Pipeline Specification

## 1. High-Level Objective
Build a multi-agent banking pipeline that ingests raw transaction records from sample-transactions.json, validates each transaction, scores it for fraud risk, routes it through compliance and settlement decisions, and writes a final result summary to shared/results/ for downstream review.

## 2. Mid-Level Objectives
- Validate required transaction fields, positive monetary values, and ISO 4217 currency codes before processing.
- Flag high-risk transactions when they exceed risk thresholds based on amount, unusual timing, or cross-border activity.
- Reject malformed or unsupported transactions with a structured reason code and preserve them in shared/results/.
- Record an audit trail for every agent action with timestamps, agent name, transaction ID, and outcome without logging plaintext PII.
- Produce a final pipeline summary report showing totals processed, accepted, rejected, and flagged transactions.

## 3. Implementation Notes
- Monetary values must use precise numeric types such as BigDecimal rather than floating point values.
- Currency codes must be validated against ISO 4217 standards (USD, EUR, GBP, JPY, etc.).
- All agent operations must be logged with ISO 8601 timestamps and sanitized context to avoid exposing account numbers or customer names.
- The pipeline should read input from sample-transactions.json and exchange messages through shared/ input, processing, output, and results directories.
- Each agent should emit JSON messages that are easy to inspect and test independently.

## 4. Context
- Beginning state: a sample-transactions.json file containing raw transactions with fields such as transaction_id, amount, currency, timestamp, source_account, destination_account, and metadata.
- Ending state: processed transaction results in shared/results/, a pipeline summary report, and test coverage of at least 90% for core agent behavior.
- The solution should be implemented in a maintainable language/framework choice, with Java/Spring Boot as the default target for this repository.

## 5. Low-Level Tasks

### Task: Transaction Validator Agent
- Prompt: "Create a Java class for a transaction validator agent that reads transactions from sample-transactions.json, checks required fields, validates positive amounts, confirms ISO 4217 currency values, and writes a structured JSON result for each transaction."
- File to CREATE: src/main/java/banking/agents/TransactionValidatorAgent.java
- Function to CREATE: validateTransaction(Transaction transaction) -> ProcessingResult
- Details: The agent must reject empty transaction IDs, missing timestamps, invalid currency codes, non-positive amounts, and malformed account references. It should emit a status of "validated" or "rejected" with a human-readable reason.

### Task: Fraud Detector Agent
- Prompt: "Create a Java fraud detection agent that evaluates transaction risk using amount thresholds, unusual timing patterns, and cross-border metadata, then returns a risk score and decision label."
- File to CREATE: src/main/java/banking/agents/FraudDetectorAgent.java
- Function to CREATE: scoreTransaction(Transaction transaction) -> FraudAssessment
- Details: The agent should assign higher risk scores to high-value transfers, off-hours activity, and international transfers. Transactions above the configured threshold should be marked for review.

### Task: Compliance and Settlement Agent
- Prompt: "Create a Java compliance and settlement agent that checks whether validated transactions are eligible for settlement, applies a final policy decision, and records the outcome in a JSON message."
- File to CREATE: src/main/java/banking/agents/ComplianceSettlementAgent.java
- Function to CREATE: evaluateSettlement(Transaction transaction, FraudAssessment assessment) -> ProcessingResult
- Details: The agent should decide whether a transaction is approved, flagged, or rejected based on validation and fraud outcomes, then prepare it for result persistence.

### Task: Reporting Agent
- Prompt: "Create a Java reporting agent that reads all agent output messages, writes a final JSON summary to shared/results/, and produces a concise pipeline summary report for the run."
- File to CREATE: src/main/java/banking/agents/ReportingAgent.java
- Function to CREATE: writeSummary(List<ProcessingResult> results) -> PipelineSummary
- Details: The agent should aggregate totals for accepted, rejected, and flagged transactions and generate a machine-readable summary for downstream tools and tests.
