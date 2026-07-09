#!/usr/bin/env bash
# PreToolUse hook: blocks `git push` when the JaCoCo coverage gate doesn't pass.
#
# The Maven project (homework-6/src) already has jacoco-maven-plugin wired
# into the `test` phase with a check goal enforcing >=80% line coverage, so
# `mvn test` itself fails whenever coverage drops below the gate (or a test
# fails). This script just runs that and translates the result into a
# PreToolUse permission decision.

set -uo pipefail

PROJECT_DIR="/d/gen-ai-software-engineering/homework-6/src"

if [ -d "/c/Program Files/Java/jdk-21" ]; then
  export JAVA_HOME="C:\\Program Files\\Java\\jdk-21"
  export PATH="/c/Program Files/Java/jdk-21/bin:$PATH"
fi

cd "$PROJECT_DIR" || {
  printf '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"Coverage gate: could not find project directory %s"}}\n' "$PROJECT_DIR"
  exit 0
}

mvn_output=$(mvn -o test 2>&1)
mvn_status=$?

csv_file="target/site/jacoco/jacoco.csv"
ratio_message=""
if [ -f "$csv_file" ]; then
  ratio_message=$(awk -F',' 'NR>1{missed+=$8; covered+=$9} END{if (covered+missed>0) printf "%.1f%% lines covered (%d/%d)", 100*covered/(covered+missed), covered, covered+missed}' "$csv_file")
fi

if [ $mvn_status -ne 0 ]; then
  reason="Coverage gate failed (mvn test exit $mvn_status). ${ratio_message:-Run mvn -o test in $PROJECT_DIR for details.} Push blocked until coverage is >=80% and tests pass."
  reason="${reason//\"/\\\"}"
  printf '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"%s"}}\n' "$reason"
  exit 0
fi

reason="Coverage gate passed: ${ratio_message:-mvn test succeeded}."
reason="${reason//\"/\\\"}"
printf '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"allow","permissionDecisionReason":"%s"}}\n' "$reason"
exit 0
