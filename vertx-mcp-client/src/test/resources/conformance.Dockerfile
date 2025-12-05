FROM node:20-slim

WORKDIR /app

RUN npm install -g @modelcontextprotocol/conformance@0.1.7

# This runs a scenario server for client testing
# The scenario server validates client behavior and outputs conformance checks
# Usage: docker run -e SCENARIO=initialize -p 3000:3000 image
CMD sh -c 'npx @modelcontextprotocol/conformance@0.1.7 run-scenario ${SCENARIO:-initialize} --port 3000'
