#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/../src"

cd "$PROJECT_DIR"
echo "Building and starting the Banking Transactions API on http://localhost:8080 ..."
mvn spring-boot:run
