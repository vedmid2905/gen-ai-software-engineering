# Run Pipeline

Run the multi-agent banking pipeline end-to-end.

Steps:
1. Check that `sample-transactions.json` exists at the repository root.
2. Clear the `shared/` directories under `src/` (`shared/input`, `shared/processing`, `shared/output`, `shared/results`) so the run starts from a clean state.
3. Run the pipeline: from `src/`, either
   - `mvn spring-boot:run "-Dspring-boot.run.mainClass=banking.pipeline.Integrator" "-Dspring-boot.run.arguments=../sample-transactions.json,shared"`, or
   - build a classpath once (`mvn dependency:build-classpath -Dmdep.outputFile=cp.txt`) and run `java -cp "target/classes;$(cat cp.txt)" banking.pipeline.Integrator ../sample-transactions.json shared`.
4. Show a summary of results from `shared/results/pipeline-summary.json` (total processed, approved, flagged, rejected).
5. Report any transactions that were rejected or flagged, with their reason, by reading the per-transaction files in `shared/results/`.
