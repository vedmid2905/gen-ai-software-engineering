#!/usr/bin/env bash
# Single-command entry point for the 4-agent bug-fix pipeline.
#
# Usage: ./run-pipeline.sh [bug-id]
#   bug-id defaults to "001" (see context/bugs/001/bug-context.md)
#
# Runs, in order: Bug Researcher -> Bug Research Verifier -> Bug Planner ->
# Bug Fixer -> Security Verifier -> Unit Test Generator. Each stage is a
# real, separate `claude -p` invocation using the matching agents/*.agent.md
# file as its system prompt (so the model + skills are loaded automatically,
# per agent) and stops the whole run if any stage fails.
set -euo pipefail

BUG_ID="${1:-001}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUG_DIR="$ROOT_DIR/context/bugs/$BUG_ID"

mkdir -p "$BUG_DIR/research"

run_agent() {
  local stage="$1" agent_file="$2" model="$3" allowed_tools="$4" prompt="$5"
  echo
  echo "===== Stage: $stage (model: $model) ====="
  local system_prompt
  system_prompt="$(cat "$ROOT_DIR/$agent_file")"
  claude -p "$prompt" \
    --model "$model" \
    --append-system-prompt "$system_prompt" \
    --allowedTools "$allowed_tools" \
    --permission-mode acceptEdits \
    --add-dir "$ROOT_DIR"
}

require_file() {
  if [[ ! -f "$1" ]]; then
    echo "Pipeline stopped: expected output '$1' was not produced." >&2
    exit 1
  fi
}

if [[ ! -f "$BUG_DIR/bug-context.md" ]]; then
  echo "No bug-context.md found at $BUG_DIR/bug-context.md — nothing to run the pipeline on." >&2
  exit 1
fi

# Stage 1: Bug Researcher (supporting agent)
run_agent "1/6 Bug Researcher" "agents/bug-researcher.agent.md" "claude-sonnet-5" \
  "Read,Grep,Glob,Write" \
  "Investigate the issues described in $BUG_DIR/bug-context.md against the real source, and write your findings to $BUG_DIR/research/codebase-research.md as specified in your instructions."
require_file "$BUG_DIR/research/codebase-research.md"

# Stage 2: Bug Research Verifier (required, Task 1)
run_agent "2/6 Bug Research Verifier" "agents/research-verifier.agent.md" "claude-opus-4-8" \
  "Read,Grep,Glob,Write" \
  "Verify every claim in $BUG_DIR/research/codebase-research.md against the real source, grade it using the research-quality-measurement skill, and write $BUG_DIR/research/verified-research.md as specified in your instructions."
require_file "$BUG_DIR/research/verified-research.md"

# Stage 3: Bug Planner (supporting agent)
run_agent "3/6 Bug Planner" "agents/bug-planner.agent.md" "claude-sonnet-5" \
  "Read,Grep,Glob,Write" \
  "Turn $BUG_DIR/research/verified-research.md into a concrete implementation plan and write $BUG_DIR/implementation-plan.md as specified in your instructions."
require_file "$BUG_DIR/implementation-plan.md"

# Stage 4: Bug Fixer (required, Task 2)
run_agent "4/6 Bug Fixer" "agents/bug-fixer.agent.md" "claude-haiku-4-5-20251001" \
  "Read,Edit,Write,Bash" \
  "Execute $BUG_DIR/implementation-plan.md exactly as written, running tests after each change, and write $BUG_DIR/fix-summary.md as specified in your instructions."
require_file "$BUG_DIR/fix-summary.md"

# Stage 5: Security Vulnerabilities Verifier (required, Task 3) - changed files only
run_agent "5/6 Security Verifier" "agents/security-verifier.agent.md" "claude-opus-4-8" \
  "Read,Grep,Glob,Write" \
  "Read $BUG_DIR/fix-summary.md, review only the files it lists as changed, and write $BUG_DIR/security-report.md as specified in your instructions. Do not edit any code."
require_file "$BUG_DIR/security-report.md"

# Stage 6: Unit Test Generator (required, Task 4) - changed files only
run_agent "6/6 Unit Test Generator" "agents/unit-test-generator.agent.md" "claude-haiku-4-5-20251001" \
  "Read,Write,Bash" \
  "Read $BUG_DIR/fix-summary.md, generate FIRST-compliant unit tests (per the unit-tests-FIRST skill) only for the files it lists as changed, run the tests, and write $BUG_DIR/test-report.md as specified in your instructions."
require_file "$BUG_DIR/test-report.md"

echo
echo "===== Pipeline complete for bug $BUG_ID ====="
echo "Artifacts written under: $BUG_DIR"
