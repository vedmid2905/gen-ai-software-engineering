# Project Agents Summary

## Overview
This project implements a 4-agent bug-fix pipeline that verifies bug research, applies fixes to a seeded sample application, reviews the changed code for security issues, and generates unit tests for the changed code — orchestrated end-to-end by a single command.

## Required Agent Roles
- **Bug Research Verifier**: fact-checks Bug Researcher output against the real source, grades research quality via the `research-quality-measurement` skill, writes `verified-research.md`.
- **Bug Fixer**: applies `implementation-plan.md` changes to the sample app, runs tests after each change, writes `fix-summary.md`.
- **Security Vulnerabilities Verifier**: scans only the files changed by the Bug Fixer for injection, hardcoded secrets, insecure comparisons, missing validation, and similar issues; writes `security-report.md` with no code edits.
- **Unit Test Generator**: generates FIRST-compliant unit tests for changed code only, runs them, writes `test-report.md`.

## Supporting Roles (not graded, needed for an end-to-end single-command run)
- **Bug Researcher**: produces the initial `research/codebase-research.md` that the Research Verifier checks.
- **Bug Planner**: turns `verified-research.md` into `implementation-plan.md` that the Bug Fixer executes.

## Model Selection
- Stronger reasoning model: Bug Research Verifier, Security Vulnerabilities Verifier (deep fact-checking and vulnerability analysis).
- Faster/cheaper model: Bug Fixer, Unit Test Generator (routine, well-specified execution against an already-approved plan).

## Project-Specific Context
The pipeline operates on a small seeded sample application under `src/` with at least 2 intentional bugs and 1 intentional security issue, documented in `context/bugs/XXX/bug-context.md`. All intermediate and final artifacts are file-based markdown under `context/bugs/XXX/`, making each stage inspectable and independently testable. Security Verifier and Unit Test Generator are scoped to only the files changed by the Bug Fixer, not the whole codebase.
