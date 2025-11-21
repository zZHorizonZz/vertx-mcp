FROM node:20-slim

WORKDIR /app

# Install the MCP conformance test framework
RUN npm install -g @modelcontextprotocol/conformance

ENTRYPOINT ["npx", "@modelcontextprotocol/conformance", "server"]
