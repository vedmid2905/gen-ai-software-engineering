---
name: research-quality-measurement
description: Defines discrete quality levels for verified bug-research reports, so the Bug Research Verifier grades research consistently instead of using an ad-hoc "looks fine" judgment.
---

# Research Quality Measurement

Use this skill whenever you (the Bug Research Verifier) finish checking a
`codebase-research.md` file against the real source and need to state a
research quality level in `verified-research.md`.

## Why a fixed rubric

A one-line "looks good" verdict is not falsifiable and drifts between runs.
Grading against fixed, named levels makes the verdict reproducible and gives
the Bug Planner (the next consumer) a clear signal for how much to trust each
claim before turning it into an implementation plan.

## Quality Levels

Compute the following two numbers first, then map them to a level:

- **Reference accuracy** = (verified claims whose file:line and quoted
  snippet exactly match the current source) / (total file:line claims made).
- **Completeness** = whether the research covers every bug/vulnerability
  listed in the relevant `bug-context.md` (yes/no per item).

| Level | Label | Reference accuracy | Completeness | Meaning |
|-------|-------|--------------------|---------------|---------|
| 4 | **VERIFIED** | 100% | All items covered | Every claim checks out exactly against source; safe to plan fixes directly from this document. |
| 3 | **MOSTLY VERIFIED** | ≥ 90% | All items covered | Only minor drift (e.g. a line number off by one after a comment was added); still safe to use, but note the drift. |
| 2 | **PARTIALLY VERIFIED** | 60–89%, or | Some items missing | Some claims don't match source, or the research misses a seeded item entirely. Planner must re-check flagged claims before acting on them. |
| 1 | **UNRELIABLE** | < 60% | — | Most claims don't match the real code, or fabricated file:line references are present. Research must be redone before planning. |
| 0 | **BLOCKED** | — | — | The research document is missing, unreadable, or references a file/repo that does not exist. Verification could not proceed. |

## Required output when using this skill

In `verified-research.md`, the **Research Quality Assessment** section must
state, in this order:
1. The computed reference accuracy and completeness numbers.
2. The resulting **Level (N) — LABEL** from the table above.
3. One or two sentences of reasoning that name the specific claims that
   drove the score (not a generic restatement of the table).

## Non-negotiable rules

- Never assign VERIFIED or MOSTLY VERIFIED if any quoted code snippet does
  not byte-for-byte match the current source at the cited line.
- A single fabricated file:line reference (the file or line does not exist)
  caps the level at UNRELIABLE (1), regardless of how accurate the rest of
  the document is.
- Always show your accuracy/completeness arithmetic — a level with no
  supporting numbers is not acceptable output.
