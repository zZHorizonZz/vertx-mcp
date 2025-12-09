# Dockerfile for running MCP client conformance tests
# This container has both Node.js (for the conformance framework) and Java (for the client)

FROM eclipse-temurin:17-jdk

# Install Node.js
RUN apt-get update && \
    apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install the MCP conformance framework
RUN npm install -g @modelcontextprotocol/conformance@0.1.7

# Copy the built JAR and dependencies (will be added by test)
COPY target/lib /app/lib
COPY target/*.jar /app/vertx-mcp-client.jar

# Copy the client wrapper script
COPY conformance-client-wrapper.sh /app/conformance-client-wrapper.sh
RUN chmod +x /app/conformance-client-wrapper.sh

# The conformance framework command
# The SCENARIO env var is set by the test
CMD npx @modelcontextprotocol/conformance client \
    --command "/app/conformance-client-wrapper.sh" \
    --scenario "${SCENARIO:-initialize}" \
    --verbose \
    --timeout 60000
