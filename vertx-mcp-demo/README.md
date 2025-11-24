# Vert.x MCP Server Demo

A task management MCP server demonstrating all major Model Context Protocol features.

## Features

- **Tools** - Create, list, update tasks; bulk import with progress; LLM summarization
- **Resources** - Static and dynamic task access with completions
- **Prompts** - Daily standup and sprint planning templates
- **Sampling** - Request LLM responses from the client
- **Elicitation** - Request user input with schema validation
- **Progress** - Report progress during long-running operations
- **Logging** - Send log notifications to the client

## Running

```bash
mvn clean install
cd vertx-mcp-demo
mvn exec:java
```

Server starts at http://localhost:8080/mcp

Custom port: `mvn exec:java -Dport=9090`

## Testing

Use the MCP Inspector:

```bash
npx @modelcontextprotocol/inspector
```

Then connect to `http://localhost:8080/mcp` in the inspector UI.
