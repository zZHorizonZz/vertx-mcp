FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build
COPY . .
RUN mvn install -DskipTests -q

WORKDIR /conformance-client
COPY vertx-mcp-client/src/test/resources/conformance/pom.xml .
RUN mkdir -p src/main/java/io/vertx/tests/mcp/client
COPY vertx-mcp-client/src/test/java/io/vertx/tests/mcp/client/ConformanceClient.java src/main/java/io/vertx/tests/mcp/client/
RUN mvn package -q

FROM eclipse-temurin:17-jdk

RUN apt-get update && \
    apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
RUN npm install -g @modelcontextprotocol/conformance@0.1.7

COPY --from=builder /conformance-client/target/conformance-client.jar /app/conformance-client.jar

RUN echo '#!/bin/bash' > /app/run.sh && \
    echo 'npx @modelcontextprotocol/conformance@0.1.7 client --command "java -jar /app/conformance-client.jar ${SCENARIO:-initialize}" --scenario "${SCENARIO:-initialize}" --verbose --timeout 60000' >> /app/run.sh && \
    echo 'EXIT_CODE=$?' >> /app/run.sh && \
    echo 'echo ""' >> /app/run.sh && \
    echo 'echo "=== RESULTS ==="' >> /app/run.sh && \
    echo 'find results -name "checks.json" -exec sh -c '"'"'cat "{}"'"'"' \;' >> /app/run.sh && \
    echo 'exit $EXIT_CODE' >> /app/run.sh && \
    chmod +x /app/run.sh

ENTRYPOINT ["/app/run.sh"]
