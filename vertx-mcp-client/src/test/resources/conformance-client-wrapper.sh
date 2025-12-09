#!/bin/bash
# Wrapper script that runs inside the Docker container
# The conformance framework calls this with: <scenario> <server-url>

SCENARIO="$1"
SERVER_URL="$2"

# Run the Java CLI with the full classpath
exec java -cp "/app/vertx-mcp-client.jar:/app/lib/*" \
  io.vertx.tests.mcp.client.ConformanceCli \
  "$SCENARIO" \
  "$SERVER_URL"
