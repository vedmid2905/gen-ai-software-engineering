# 4-Agent Bug-Fix Pipeline

Created by **Kostiantyn Vedmid**

## What this is

A 4-agent pipeline that verifies bug research, fixes seeded bugs in a small sample
application, reviews the fix for security issues, and generates unit tests for the
changed code — runnable end-to-end with one command. The pipeline operates on
`TaskFlow`, a minimal in-memory task manager (`src/taskflow/`) seeded with 2
intentional bugs and 1 intentional security vulnerability, documented in
[`context/bugs/001/bug-context.md`](context/bugs/001/bug-context.md).

See [`specification.md`](specification.md), [`design.md`](design.md), and
[`implementation-plan.md`](implementation-plan.md) for the spec-first documents this
project was built from, and [`agents.md`](agents.md) for a short summary of every
agent's role. See [HOWTORUN.md](HOWTORUN.md) for step-by-step run instructions.

## Pipeline

```text
context/bugs/001/bug-context.md
        |
        v
Bug Researcher -----------------------> research/codebase-research.md
        |
        v
Bug Research Verifier (skill: research-quality-measurement)
        |                                -> research/verified-research.md
        v
Bug Planner --------------------------->  implementation-plan.md
        |
        v
Bug Fixer -------------------------------> [fixes applied to src/] + fix-summary.md
        |
        +------------------------------------------+
        v                                           v
Security Verifier                        Unit Test Generator (skill: unit-tests-FIRST)
        |                                           |
        v                                           v
security-report.md                        tests/test_bug_001_fixes.py + test-report.md
```

Run order: **Bug Researcher → Bug Research Verifier → Bug Planner → Bug Fixer →
Security Verifier (changed files only) → Unit Test Generator (changed files
only)**, driven by a single command (`./run-pipeline.sh`, see below) — no manual
per-agent invocation.

## Agent responsibilities and model choice

| Agent | File | Required? | Model | Why |
|---|---|---|---|---|
| Bug Researcher | `agents/bug-researcher.agent.md` | supporting | `claude-sonnet-5` | Balanced model — explores the codebase and cites file:line evidence; its output is fact-checked downstream anyway. |
| **Bug Research Verifier** | `agents/research-verifier.agent.md` | **required (Task 1)** | `claude-opus-4-8` | Strong reasoning — its entire job is catching subtle mismatches (wrong line numbers, paraphrased snippets, fabricated references) between claims and real source. A weaker model is more likely to rubber-stamp plausible-but-wrong claims, poisoning every later stage. |
| Bug Planner | `agents/bug-planner.agent.md` | supporting | `claude-sonnet-5` | Balanced model — turns verified research into a concrete plan; the plan is still checked by the Bug Fixer's own test runs. |
| **Bug Fixer** | `agents/bug-fixer.agent.md` | **required (Task 2)** | `claude-haiku-4-5-20251001` | Fast/cheap — by this stage the plan already specifies the exact file, before/after code, and test command; execution is well-specified transcription plus running a command. |
| **Security Vulnerabilities Verifier** | `agents/security-verifier.agent.md` | **required (Task 3)** | `claude-opus-4-8` | Strong reasoning — spotting injection paths and unsafe data flow needs reasoning about attacker-controlled input, not pattern matching; this is the last line of defense before the diff ships. |
| **Unit Test Generator** | `agents/unit-test-generator.agent.md` | **required (Task 4)** | `claude-haiku-4-5-20251001` | Fast/cheap — `fix-summary.md` already pins down the changed function and its correct behavior; writing tests against a known-correct target is well-specified scaffolding, and this stage runs on every pipeline execution. |

This matches the assignment's guidance: a stronger reasoning model for research
verification and security review, a faster/cheaper model for routine fixes and test
scaffolding. Full justification is also recorded in each agent's frontmatter.

## Skills

- [`skills/research-quality-measurement.md`](skills/research-quality-measurement.md)
  — defines 5 discrete research-quality levels (BLOCKED → UNRELIABLE → PARTIALLY
  VERIFIED → MOSTLY VERIFIED → VERIFIED) computed from reference accuracy and
  completeness; used by the Bug Research Verifier.
- [`skills/unit-tests-FIRST.md`](skills/unit-tests-FIRST.md) — defines Fast,
  Independent, Repeatable, Self-validating, Timely with project-specific guidance;
  used by the Unit Test Generator.

## Sample application: TaskFlow

`src/taskflow/` — pure Python standard library, no third-party dependencies. Run it
with `python run.py <command> ...`; test it with
`python -m unittest discover -s tests -v`.

Seeded issues (see [`bug-context.md`](context/bugs/001/bug-context.md) for full
repro steps), all fixed by this pipeline run:

1. **Bug** — `TaskStore.search` excluded a title that started with the search term
   (`find(...) > 0` instead of an `in` check) — `src/taskflow/store.py`.
2. **Bug** — `TaskStore.sorted_by_priority` sorted priorities as strings, so
   priority `2` outranked priority `10` — `src/taskflow/store.py`.
3. **Security (CWE-78, OS Command Injection)** — `export_tasks` interpolated
   unsanitized CLI arguments into a `subprocess.run(..., shell=True)` call —
   `src/taskflow/cli.py`. Manually verified exploitable (see bug-context.md) and
   fixed by removing the shell entirely in favor of a plain Python substring
   filter.

The Security Verifier's review of the fix (`security-report.md`) also found one
**HIGH** finding not part of the original seed — an unvalidated `export` filename
that permits writing to an arbitrary path — demonstrating the pipeline catching a
real issue beyond the one it was told to look for.

## Pipeline outputs (this run, bug 001)

All under [`context/bugs/001/`](context/bugs/001/):

- [`research/codebase-research.md`](context/bugs/001/research/codebase-research.md)
- [`research/verified-research.md`](context/bugs/001/research/verified-research.md) — Research Quality: **Level 4 — VERIFIED**
- [`implementation-plan.md`](context/bugs/001/implementation-plan.md)
- [`fix-summary.md`](context/bugs/001/fix-summary.md) — all 3 steps applied, 11/11 tests passing after each step
- [`security-report.md`](context/bugs/001/security-report.md) — command injection confirmed fixed; 1 HIGH finding (path traversal) and 1 INFO finding reported
- [`test-report.md`](context/bugs/001/test-report.md) — 9 new tests added, 20/20 total tests passing

## AI tools used

Built with Claude Code. The agent and skill definitions were authored as
`*.agent.md` / `*.md` prompt files under `agents/` and `skills/`; the pipeline run
recorded under `context/bugs/001/` was produced by executing each agent's defined
process (research → verify → plan → fix → security review → test generation)
against the seeded `TaskFlow` bugs, with every fix and generated test actually
executed via `python -m unittest` to confirm the reported results.
