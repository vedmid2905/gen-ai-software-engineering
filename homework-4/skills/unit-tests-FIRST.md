---
name: unit-tests-FIRST
description: Defines the FIRST properties (Fast, Independent, Repeatable, Self-validating, Timely) that every generated unit test must satisfy, used by the Unit Test Generator agent.
---

# FIRST Unit Test Principles

Use this skill whenever you (the Unit Test Generator) write a new unit test
for changed code. Before finalizing a test, check it against every letter
below; a test that fails any of them must be rewritten, not shipped.

## F — Fast
- The test must run in milliseconds, not seconds. No real network calls,
  no `sleep`, no waiting on external processes.
- For this project: use the in-memory `TaskStore` / CLI functions directly;
  never spawn a subprocess to exercise application logic (subprocess calls
  are themselves what the Security Verifier checks for — a slow *and*
  risky pattern).

## I — Independent
- Each test must set up its own state and must not depend on the order
  other tests ran in, or on state left behind by another test.
- Use `setUp()` (or an equivalent per-test fixture) to construct a fresh
  `TaskStore` for every test method; never share a module-level store
  across tests.

## R — Repeatable
- The test must produce the same result every time, in any environment
  (CI, a teammate's laptop, this sandbox).
- Do not depend on wall-clock time, machine locale, filesystem state left
  over from a previous run, or dictionary/set iteration order. Any test
  that writes a file must use a temporary directory and clean it up.

## S — Self-validating
- The test must assert a pass/fail outcome itself (`assertEqual`,
  `assertTrue`, etc.) — never a test that only prints output for a human
  to eyeball.
- Prefer the most specific assertion available (e.g. `assertEqual(list,
  [...])` over `assertTrue(len(list) > 0)`) so failures are diagnosable
  from the assertion message alone.

## T — Timely
- Write the test at the same time as (or immediately after) the fix it
  covers — never batch "write tests later" as separate follow-up work.
- Each generated test must map to exactly one entry in `fix-summary.md`;
  a change with no corresponding test is incomplete, not a "test debt"
  item to defer.

## Required output when using this skill

For every test file generated, `test-report.md` must state, per test:
- which changed file/function it covers,
- which FIRST letter(s) were most relevant to how it was written (e.g.
  "Independent: fresh TaskStore per test"),
- the pass/fail result from actually running it.

## Non-negotiable rules

- Do not generate a test for code that was not part of the diff described
  in `fix-summary.md` — out-of-scope tests are not "extra credit", they are
  scope creep that slows the Timely property for the current change.
- Do not mark a test as passing without having actually executed the test
  command and observed the result.
