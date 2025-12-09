#!/bin/bash
# Wrapper script for running the MCP client conformance CLI
# Usage: conformance-client.sh <scenario> <server-url>

set -e

SCENARIO="$1"
SERVER_URL="$2"

if [ -z "$SCENARIO" ] || [ -z "$SERVER_URL" ]; then
  echo "Usage: conformance-client.sh <scenario> <server-url>"
  exit 1
fi

# Find the project root (assuming this script is in vertx-mcp-client/src/test/resources)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

echo "[wrapper] Project root: $PROJECT_ROOT"
echo "[wrapper] Scenario: $SCENARIO"
echo "[wrapper] Server URL: $SERVER_URL"

# Build the project if needed (or assume it's already built)
cd "$PROJECT_ROOT"

# Run the CLI using Maven exec plugin
mvn -q exec:java \
  -pl vertx-mcp-client \
  -Dexec.mainClass="io.vertx.tests.mcp.client.ConformanceCli" \
  -Dexec.args="$SCENARIO $SERVER_URL" \
  -Dexec.classpathScope=test
