# Vert.x MCP Server Demo

This module demonstrates how to create an MCP (Model Context Protocol) server using Vert.x with tools and resources.

## Features

### Tools (6 available)

#### Structured Tools
- **calculator** - Performs arithmetic operations (add, subtract, multiply, divide)
- **uppercase** - Converts text to uppercase
- **lowercase** - Converts text to lowercase
- **reverse** - Reverses text

#### Unstructured Tools
- **greeting** - Generates personalized greetings in different styles (formal, casual, enthusiastic)
- **timestamp** - Returns current timestamp in specified format

### Resources (2 available)

- **resource://sample-data/users** - Sample dataset with user information (JSON)
- **resource://sample-data/products** - Sample dataset with product information (JSON)

## Running the Demo

### Prerequisites
- Java 21 or higher
- Maven 3.8+

### Build and Run

```bash
# From the project root
mvn clean install

# Run the demo
cd vertx-mcp-demo
mvn exec:java
```

The server will start on http://localhost:8080/mcp

To specify a different port:
```bash
mvn exec:java -Dport=9090
```

## Testing the Server

### Using curl

#### 1. Initialize Connection
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "capabilities": {},
      "clientInfo": {
        "name": "test-client",
        "version": "1.0.0"
      }
    }
  }'
```

#### 2. List Available Tools
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list",
    "params": {}
  }'
```

#### 3. Call the Calculator Tool
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "calculator",
      "arguments": {
        "operation": "multiply",
        "a": 6,
        "b": 7
      }
    }
  }'
```

Expected response:
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "structuredContent": {
      "result": 42.0,
      "operation": "multiply"
    },
    "isError": false
  }
}
```

#### 4. Call the Greeting Tool
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 4,
    "method": "tools/call",
    "params": {
      "name": "greeting",
      "arguments": {
        "name": "Alice",
        "style": "enthusiastic"
      }
    }
  }'
```

#### 5. List Resources
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 5,
    "method": "resources/list",
    "params": {}
  }'
```

#### 6. Read a Resource
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 6,
    "method": "resources/read",
    "params": {
      "uri": "resource://sample-data/users"
    }
  }'
```

#### 7. Convert Text to Uppercase
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 7,
    "method": "tools/call",
    "params": {
      "name": "uppercase",
      "arguments": {
        "text": "hello world"
      }
    }
  }'
```

#### 8. Get Current Timestamp
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 8,
    "method": "tools/call",
    "params": {
      "name": "timestamp",
      "arguments": {
        "format": "yyyy-MM-dd HH:mm:ss"
      }
    }
  }'
```

### Using Sessions

The server supports sessions. After initialization, you'll receive a session ID in the response headers:

```bash
# Initialize and capture session ID
curl -v -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{...}' 2>&1 | grep -i "x-mcp-session-id"

# Use session ID in subsequent requests
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "X-MCP-Session-ID: <session-id>" \
  -d '{...}'
```

## Code Structure

```
vertx-mcp-demo/
├── pom.xml
├── README.md
└── src/main/java/io/vertx/mcp/demo/
    └── MCPServerDemo.java
```

## Extending the Demo

### Adding a New Tool

```java
// In setupTools() method
JsonObject inputSchema = Schemas.objectSchema()
  .requiredProperty("input", Schemas.stringSchema())
  .toJson();

JsonObject outputSchema = Schemas.objectSchema()
  .property("output", Schemas.stringSchema())
  .toJson();

toolFeature.addStructuredTool(
  "my-tool",
  "My Tool",
  "Description of my tool",
  StructuredToolHandler.create(inputSchema, outputSchema, args -> {
    // Your tool logic here
    return Future.succeededFuture(new JsonObject()
      .put("output", "result"));
  })
);
```

### Adding a New Resource

```java
// In setupResources() method
resourceFeature.addStaticResource("my-resource", () ->
  Future.succeededFuture(new TextResourceContent()
    .setUri("resource://my-resource")
    .setName("my-resource")
    .setTitle("My Resource")
    .setDescription("Description of my resource")
    .setMimeType("text/plain")
    .setText("Resource content here"))
);
```

## MCP Protocol

This demo implements the Model Context Protocol (MCP) specification. The protocol uses JSON-RPC 2.0 for communication.

For more information about MCP, visit: https://modelcontextprotocol.io
