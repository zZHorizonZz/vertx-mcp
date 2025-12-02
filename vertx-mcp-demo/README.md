# Vert.x MCP Demo

This module contains demonstration applications for the Vert.x Model Context Protocol (MCP) implementation.

## Demos

### 1. MCPServerDemo

A comprehensive MCP server that implements a task management system, showcasing all major MCP features:

- **Tools** - Task CRUD operations with structured/unstructured outputs
- **Resources** - Static and dynamic resource access with completions
- **Prompts** - Pre-built prompt templates with argument completions
- **Sampling** - Request LLM responses from the client
- **Elicitation** - Request user input with schema validation
- **Progress** - Report progress during long-running operations
- **Logging** - Send log notifications to the client

### 2. MCPClientDemo

A client application that connects to the MCP server and demonstrates:

- **Connection** - Connecting to an MCP server via HTTP
- **Tools** - Listing and calling tools
- **Resources** - Listing and reading resources
- **Prompts** - Getting prompts

## Running the Demos

### Prerequisites

Build the project:
```bash
mvn clean install
```

### Running the Server

Start the MCP server (defaults to port 8080):
```bash
mvn exec:java -pl vertx-mcp-demo -Dexec.mainClass="io.vertx.mcp.demo.MCPServerDemo"
```

Or specify a custom port:
```bash
mvn exec:java -pl vertx-mcp-demo -Dexec.mainClass="io.vertx.mcp.demo.MCPServerDemo" -Dport=9000
```

Server starts at http://localhost:8080/mcp

### Running the Client

In a separate terminal, start the MCP client:
```bash
mvn exec:java -pl vertx-mcp-demo -Dexec.mainClass="io.vertx.mcp.demo.MCPClientDemo"
```

Or connect to a custom server URL:
```bash
mvn exec:java -pl vertx-mcp-demo -Dexec.mainClass="io.vertx.mcp.demo.MCPClientDemo" -Dserver.url="http://localhost:9000/mcp"
```

### Testing with MCP Inspector

You can also use the MCP Inspector to test the server:

```bash
npx @modelcontextprotocol/inspector
```

Then connect to `http://localhost:8080/mcp` in the inspector UI.
