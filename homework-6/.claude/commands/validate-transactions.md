# Validate Transactions

Validate all transactions in `sample-transactions.json` without running the full pipeline (no fraud scoring, no settlement decision — validator only).

Steps:
1. Read `sample-transactions.json` from the repository root.
2. For each transaction, run only `banking.pipeline.agent.TransactionValidatorAgent#validateTransaction` — do not invoke the fraud detector, compliance/settlement, or reporting agents, and do not write anything to `shared/`. This can be done from a small ad-hoc script/test, or via `banking.pipeline.PipelineOrchestrator` stopped after the validator stage if you're wiring it up interactively.
3. Report:
   - total transaction count
   - valid count (`status == "validated"`)
   - invalid count (`status == "rejected"`)
   - the reason for each rejection
4. Show the results as a table: `transaction_id | status | reason`.
