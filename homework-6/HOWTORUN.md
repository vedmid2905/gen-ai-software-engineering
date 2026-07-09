# How to Run

Numbered steps from a clean checkout to a full demo (pipeline + REST API + frontend + tests + coverage gate).

## 1. Prerequisites

- **Java 21** (JDK 21)
- **Maven 3.6+**

## 2. Point the shell at JDK 21

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:PATH = "C:\Program Files\Java\jdk-21\bin;" + $env:PATH
```

## 3. Build

```powershell
cd src
mvn compile
```

## 4. Run the tests (and generate the coverage report)

```powershell
mvn test
```

- All 44 tests (unit + `MockMvc` controller tests + full pipeline integration test) should pass.
- JaCoCo runs automatically and **fails the build if line coverage drops below 80%** (see `pom.xml` → `jacoco-maven-plugin` → `check` execution). Current coverage is ~84%.
- Open the human-readable report at `src/target/site/jacoco/index.html`.

## 5. Run the application (REST API + pipeline + frontend)

```powershell
mvn spring-boot:run
```

Then open **http://localhost:8080/** — this serves the single-page frontend shell with two tabs:

- **Transactions** — create a deposit/withdrawal/transfer, list/filter transactions, look up an account's balance and summary.
- **Pipeline** — click **Run pipeline** to process `sample-transactions.json` through the full agent chain and see the summary tiles (total/approved/flagged/rejected) and per-transaction results table.

> If your environment has restricted network access and `mvn spring-boot:run` can't resolve the Spring Boot plugin's own dependencies, see [`src/README.md`](src/README.md) for a classpath-based fallback (`mvn dependency:build-classpath` + `java -cp ...`).

## 6. Run the pipeline from the command line (no web server)

```powershell
cd src
mvn spring-boot:run "-Dspring-boot.run.mainClass=banking.pipeline.Integrator" "-Dspring-boot.run.arguments=../sample-transactions.json,shared"
```

This prints a summary to the console and writes every stage's JSON messages under `src/shared/{input,processing,output,results}/`.

## 7. Use the Claude Code skills

From within a Claude Code session opened on this repository:

- `/write-spec` — regenerates `specification.md` from the template.
- `/run-pipeline` — clears `shared/`, runs the pipeline end-to-end, and reports the summary plus any rejected/flagged transactions.
- `/validate-transactions` — runs only the validator stage over `sample-transactions.json` (no fraud scoring, no file writes) and reports a valid/invalid table.

## 8. Coverage-gate hook

`.claude/settings.json` registers a `PreToolUse` hook (`.claude/hooks/check-coverage.sh`) that fires whenever a `git push` command is about to run. It executes `mvn -o test` and **blocks the push** if the tests fail or JaCoCo line coverage is below 80%, reporting the actual coverage percentage in the block reason.

To see it in action: drop coverage below 80% (e.g. temporarily raise the `<minimum>` in `pom.xml`'s `jacoco-maven-plugin` config past what's currently covered, or delete a test file) and then run `git push` — the push will be blocked with the coverage percentage in the message. Restore the change afterward.

## 9. Demo checklist

1. `mvn test` → all green, coverage report generated.
2. `mvn spring-boot:run` → open http://localhost:8080/.
3. In the **Pipeline** tab, click **Run pipeline** → confirm 8 total / 5 approved / 1 flagged / 2 rejected.
4. In the **Transactions** tab, create a transaction and look up its account balance/summary.
5. Inspect `src/shared/results/*.json` and `pipeline-summary.json` on disk to see the raw agent output.
