# 4-Agent Bug-Fix Pipeline Specification

## 1. High-Level Objective
Build a 4-agent pipeline — Bug Research Verifier, Bug Fixer, Security Vulnerabilities Verifier, and Unit Test Generator — that operates on a small seeded-bug sample application, runnable end-to-end via a single command, producing verifiable research, fixes, a security report, and generated unit tests.

## 2. Mid-Level Objectives
- Verify the Bug Researcher's output (`research/codebase-research.md`) by checking every file:line reference against the real source, grading the research using a dedicated quality-measurement skill, and writing `verified-research.md`.
- Execute `implementation-plan.md` step by step, applying each change to the sample app, running tests after every change, and writing a complete `fix-summary.md` (or stopping and documenting a failure).
- Scan only the files changed by the Bug Fixer for security issues (injection, hardcoded secrets, insecure comparisons, missing validation, unsafe dependencies, XSS/CSRF where relevant), rate each finding CRITICAL/HIGH/MEDIUM/LOW/INFO with file:line and remediation, and write `security-report.md` only — no code edits.
- Generate unit tests only for the code changed by the Bug Fixer, following a FIRST-principles skill (Fast, Independent, Repeatable, Self-validating, Timely), run them, and write `test-report.md`.
- Provide a minimal, self-contained sample application (`src/` + `tests/`) with at least 2 intentional bugs and at least 1 intentional security issue, runnable and testable in a few commands, so the pipeline has real, demonstrable work to do.

## 3. Implementation Notes
- Each agent is defined in its own file under `agents/` (`research-verifier.agent.md`, `bug-fixer.agent.md`, `security-verifier.agent.md`, `unit-test-generator.agent.md`) with an explicit model selection in its frontmatter; the choice (stronger reasoning model for research verification / security review, faster/cheaper model for routine fixes / test scaffolding) must be briefly justified in the homework `README.md`.
- Two skills are required and must be referenced by name from the agents that use them:
  - `skills/research-quality-measurement.md` (defines research-quality levels/labels; used by the Research Verifier).
  - `skills/unit-tests-FIRST.md` (defines FIRST; used by the Unit Test Generator).
- The full pipeline must run via one command (e.g. `npm run pipeline` or `./run-pipeline.sh`) that invokes the agents in order and auto-loads their skills — no manual per-agent invocation: Bug Researcher → Bug Research Verifier → Bug Planner → Bug Fixer → Security Verifier (changed code only) → Unit Test Generator (changed code only).
- The Security Verifier and Unit Test Generator must scope their work to files actually changed by the Bug Fixer (per `fix-summary.md`), not the whole codebase.
- The Security Verifier never edits code; it only produces `security-report.md`.
- Intermediate and final agent artifacts live under `context/bugs/XXX/`: `bug-context.md`, `research/codebase-research.md`, `research/verified-research.md`, `implementation-plan.md`, `fix-summary.md`, `security-report.md`, `test-report.md`.
- Keep the sample app's stack simple: single language, minimal dependencies, one runnable entry point, one test command.

## 4. Context
- **Beginning state**: `homework-4/` contains only `TASKS.md` — no agents, skills, sample app, or docs exist yet.
- **Ending state**: `homework-4/` contains:
  - `agents/` — the 4 agent definitions, each with explicit model choice.
  - `skills/` — `research-quality-measurement.md` and `unit-tests-FIRST.md`.
  - `src/` + `tests/` — the sample app, seeded bugs fixed, pipeline-generated tests passing.
  - `context/bugs/XXX/` — research, verified research, implementation plan, fix summary, security report, test report, referencing real files in `src/`.
  - `docs/screenshots/` — pipeline run, fixes applied, security scan, unit test run.
  - `README.md` (overview, how to run the pipeline and the app, model-choice justification, author/student info) and `HOWTORUN.md`.
- Target stack for the sample app: to be chosen small enough to seed and fix within one pipeline run (e.g. a small CLI, REST API, or single-page UI).

## 5. Low-Level Tasks

### Task: Sample Mini Application (seeded bugs + security issue)
- Prompt: "Create a minimal, single-language sample application with at least 2 intentional bugs and 1 intentional security issue, a runnable entry point, and a test command, documented in `context/bugs/XXX/bug-context.md`."
- Files to CREATE: `src/**` (app source), `tests/**` (baseline tests), `context/bugs/001/bug-context.md`
- Details: App must run locally in a few commands; `npm test` (or equivalent) must work before and after the pipeline runs; bugs and the security issue must be real, findable defects (not comments describing hypothetical ones).

### Task: Bug Research Verifier agent + research-quality skill
- Prompt: "Create a fact-checking agent that reads `research/codebase-research.md`, verifies every file:line reference and snippet against the real source, and writes `verified-research.md` using a research-quality skill."
- Files to CREATE: `agents/research-verifier.agent.md`, `skills/research-quality-measurement.md`
- Details: `verified-research.md` must contain Verification Summary (pass/fail + quality level per skill), Verified Claims, Discrepancies Found, Research Quality Assessment (level + reasoning), References. The skill must define discrete quality levels/labels the verifier applies consistently.

### Task: Bug Fixer agent
- Prompt: "Create an agent that reads `implementation-plan.md`, applies each change to the sample app exactly as specified, runs tests after every change, and writes `fix-summary.md`, stopping and documenting failure if a test run fails."
- File to CREATE: `agents/bug-fixer.agent.md`
- Details: `fix-summary.md` must list Changes Made (file, location, before/after, test result), Overall Status, Manual Verification steps, References back to the plan.

### Task: Security Vulnerabilities Verifier agent
- Prompt: "Create a read-only security-review agent that reads `fix-summary.md` and the changed files, scans for injection, hardcoded secrets, insecure comparisons, missing validation, unsafe dependencies, and XSS/CSRF where relevant, and writes `security-report.md` with no code edits."
- File to CREATE: `agents/security-verifier.agent.md`
- Details: Every finding needs a severity (CRITICAL/HIGH/MEDIUM/LOW/INFO), a file:line reference, and a remediation suggestion; agent must never modify source files.

### Task: Unit Test Generator agent + FIRST skill
- Prompt: "Create an agent that reads `fix-summary.md` and the changed files, generates unit tests only for the new/changed code following a FIRST-principles skill, runs the tests, and writes `test-report.md`."
- Files to CREATE: `agents/unit-test-generator.agent.md`, `skills/unit-tests-FIRST.md`
- Details: Skill defines Fast, Independent, Repeatable, Self-validating, Timely with concrete guidance; generated tests must satisfy it and must be scoped to changed code only, not the whole codebase.

### Task: Single-command pipeline runner
- Prompt: "Create a single entry-point script/command that runs the full agent chain in order — Bug Researcher, Research Verifier, Bug Planner, Bug Fixer, Security Verifier, Unit Test Generator — auto-loading each agent's required skills, with no manual invocation between steps."
- File to CREATE: `run-pipeline.sh` (or an `npm run pipeline` script entry)
- Details: Must fail fast and surface which stage failed; must pass the changed-files scope from the Bug Fixer stage into the Security Verifier and Unit Test Generator stages.

### Task: Documentation and evidence
- Prompt: "Document the pipeline, how to run it and the app, the model choice per agent, and capture screenshots of a full pipeline run."
- Files to CREATE: `README.md`, `HOWTORUN.md`, `docs/screenshots/*`
- Details: README must include author/student info per the repo root README, and a brief justification for each agent's model choice.
