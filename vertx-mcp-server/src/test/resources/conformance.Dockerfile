FROM node:20-slim

WORKDIR /app

RUN npm install -g @modelcontextprotocol/conformance@0.1.7

RUN echo '#!/bin/bash' > /app/run.sh && \
    echo 'npx @modelcontextprotocol/conformance@0.1.7 server "$@"' >> /app/run.sh && \
    echo 'echo ""' >> /app/run.sh && \
    echo 'echo "=== DETAILED RESULTS ==="' >> /app/run.sh && \
    echo 'find results -name "checks.json" -exec sh -c '"'"'echo ""; echo "--- {} ---"; cat "{}"'"'"' \;' >> /app/run.sh && \
    chmod +x /app/run.sh

ENTRYPOINT ["/app/run.sh"]
