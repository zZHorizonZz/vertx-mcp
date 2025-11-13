package io.vertx.mcp.demo;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.mcp.common.content.Content;
import io.vertx.mcp.common.content.TextContent;
import io.vertx.mcp.common.resources.TextResourceContent;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.StructuredToolHandler;
import io.vertx.mcp.server.UnstructuredToolHandler;
import io.vertx.mcp.server.impl.ResourceServerFeature;
import io.vertx.mcp.server.impl.ToolServerFeature;
import io.vertx.mcp.server.transport.http.HttpServerTransport;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Demo MCP Server with example tools and resources.
 *
 * This demo shows:
 * - Structured tools (calculator operations, text manipulation)
 * - Unstructured tools (content generation)
 * - Static resources (documentation, API info)
 */
public class MCPServerDemo {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    // Create MCP server with options
    ServerOptions serverOptions = new ServerOptions()
      .setServerName("vertx-mcp-demo")
      .setServerVersion("1.0.0")
      .setSessionsEnabled(true)
      .setStreamingEnabled(true);

    ModelContextProtocolServer mcpServer = ModelContextProtocolServer.create(serverOptions);

    // Setup tools and resources
    setupTools(mcpServer);
    setupResources(mcpServer);

    // Create HTTP transport
    HttpServerTransport transport = new HttpServerTransport(vertx, mcpServer);

    // Start HTTP server
    int port = Integer.parseInt(System.getProperty("port", "8080"));
    HttpServerOptions httpOptions = new HttpServerOptions()
      .setPort(port)
      .setHost("localhost");

    HttpServer server = vertx.createHttpServer(httpOptions);
    server.requestHandler(transport);

    server.listen()
      .onSuccess(s -> {
        System.out.println("========================================");
        System.out.println("MCP Server Demo Started!");
        System.out.println("========================================");
        System.out.println("Server:  " + serverOptions.getServerName());
        System.out.println("Version: " + serverOptions.getServerVersion());
        System.out.println("Port:    " + s.actualPort());
        System.out.println("URL:     http://localhost:" + s.actualPort() + "/mcp");
        System.out.println("========================================");
        System.out.println("\nAvailable Tools:");
        System.out.println("  - calculator: Perform arithmetic operations");
        System.out.println("  - uppercase: Convert text to uppercase");
        System.out.println("  - lowercase: Convert text to lowercase");
        System.out.println("  - reverse: Reverse text");
        System.out.println("  - greeting: Generate personalized greeting");
        System.out.println("  - timestamp: Get current timestamp");
        System.out.println("\nAvailable Resources:");
        System.out.println("  - resource://sample-data/users");
        System.out.println("  - resource://sample-data/products");
        System.out.println("  - resource://user/{id} (dynamic)");
        System.out.println("  - resource://product/{id} (dynamic)");
        System.out.println("========================================");
      })
      .onFailure(err -> {
        System.err.println("Failed to start server: " + err.getMessage());
        vertx.close();
      });
  }

  private static void setupTools(ModelContextProtocolServer server) {
    ToolServerFeature toolFeature = new ToolServerFeature();

    // Structured Tool: Calculator
    JsonObject calculatorInputSchema = Schemas.objectSchema()
      .requiredProperty("operation", Schemas.stringSchema()
        .withKeyword("enum", new JsonArray()
          .add("add").add("subtract").add("multiply").add("divide")))
      .requiredProperty("a", Schemas.numberSchema())
      .requiredProperty("b", Schemas.numberSchema())
      .toJson();

    JsonObject calculatorOutputSchema = Schemas.objectSchema()
      .property("result", Schemas.numberSchema())
      .property("operation", Schemas.stringSchema())
      .toJson();

    toolFeature.addStructuredTool(
      "calculator",
      "Calculator",
      "Performs arithmetic operations (add, subtract, multiply, divide)",
      StructuredToolHandler.create(calculatorInputSchema, calculatorOutputSchema, args -> {
        String operation = args.getString("operation");
        double a = args.getDouble("a");
        double b = args.getDouble("b");
        double result;

        switch (operation) {
          case "add":
            result = a + b;
            break;
          case "subtract":
            result = a - b;
            break;
          case "multiply":
            result = a * b;
            break;
          case "divide":
            if (b == 0) {
              return Future.failedFuture("Division by zero");
            }
            result = a / b;
            break;
          default:
            return Future.failedFuture("Unknown operation: " + operation);
        }

        return Future.succeededFuture(new JsonObject()
          .put("result", result)
          .put("operation", operation));
      })
    );

    // Structured Tool: Text Uppercase
    JsonObject textInputSchema = Schemas.objectSchema()
      .requiredProperty("text", Schemas.stringSchema())
      .toJson();

    JsonObject textOutputSchema = Schemas.objectSchema()
      .property("result", Schemas.stringSchema())
      .toJson();

    toolFeature.addStructuredTool(
      "uppercase",
      "Uppercase Text",
      "Converts text to uppercase",
      StructuredToolHandler.create(textInputSchema, textOutputSchema, args -> {
        String text = args.getString("text");
        return Future.succeededFuture(new JsonObject()
          .put("result", text.toUpperCase()));
      })
    );

    toolFeature.addStructuredTool(
      "lowercase",
      "Lowercase Text",
      "Converts text to lowercase",
      StructuredToolHandler.create(textInputSchema, textOutputSchema, args -> {
        String text = args.getString("text");
        return Future.succeededFuture(new JsonObject()
          .put("result", text.toLowerCase()));
      })
    );

    toolFeature.addStructuredTool(
      "reverse",
      "Reverse Text",
      "Reverses the input text",
      StructuredToolHandler.create(textInputSchema, textOutputSchema, args -> {
        String text = args.getString("text");
        String reversed = new StringBuilder(text).reverse().toString();
        return Future.succeededFuture(new JsonObject()
          .put("result", reversed));
      })
    );

    // Unstructured Tool: Greeting Generator
    JsonObject greetingInputSchema = Schemas.objectSchema()
      .requiredProperty("name", Schemas.stringSchema())
      .property("style", Schemas.stringSchema()
        .withKeyword("enum", new JsonArray()
          .add("formal").add("casual").add("enthusiastic")))
      .toJson();

    toolFeature.addUnstructuredTool(
      "greeting",
      "Greeting Generator",
      "Generates personalized greetings in different styles",
      UnstructuredToolHandler.create(greetingInputSchema, args -> {
        String name = args.getString("name");
        String style = args.getString("style", "casual");
        String greeting;

        switch (style) {
          case "formal":
            greeting = "Good day, " + name + ". It is a pleasure to make your acquaintance.";
            break;
          case "enthusiastic":
            greeting = "Hey " + name + "!!! So awesome to meet you! Let's do great things together!";
            break;
          default: // casual
            greeting = "Hi " + name + "! Nice to meet you.";
            break;
        }

        return Future.succeededFuture(new Content[] {
          new TextContent(greeting)
        });
      })
    );

    // Unstructured Tool: Timestamp
    JsonObject timestampInputSchema = Schemas.objectSchema()
      .property("format", Schemas.stringSchema())
      .toJson();

    toolFeature.addUnstructuredTool(
      "timestamp",
      "Timestamp Generator",
      "Returns the current timestamp in the specified format",
      UnstructuredToolHandler.create(timestampInputSchema, args -> {
        String format = args.getString("format", "yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        String timestamp = now.format(formatter);

        return Future.succeededFuture(new Content[] {
          new TextContent("Current timestamp: " + timestamp)
        });
      })
    );

    server.serverFeatures(toolFeature);
  }

  private static void setupResources(ModelContextProtocolServer server) {
    ResourceServerFeature resourceFeature = new ResourceServerFeature();

    // Sample data storage
    JsonArray users = new JsonArray()
      .add(new JsonObject()
        .put("id", 1)
        .put("name", "Alice Johnson")
        .put("email", "alice@example.com")
        .put("role", "Developer"))
      .add(new JsonObject()
        .put("id", 2)
        .put("name", "Bob Smith")
        .put("email", "bob@example.com")
        .put("role", "Designer"))
      .add(new JsonObject()
        .put("id", 3)
        .put("name", "Carol Williams")
        .put("email", "carol@example.com")
        .put("role", "Manager"));

    JsonArray products = new JsonArray()
      .add(new JsonObject()
        .put("id", 101)
        .put("name", "Laptop")
        .put("price", 999.99)
        .put("category", "Electronics")
        .put("inStock", true))
      .add(new JsonObject()
        .put("id", 102)
        .put("name", "Desk Chair")
        .put("price", 249.50)
        .put("category", "Furniture")
        .put("inStock", true))
      .add(new JsonObject()
        .put("id", 103)
        .put("name", "Notebook")
        .put("price", 12.99)
        .put("category", "Stationery")
        .put("inStock", false));

    // Static Resource: Sample Users Data
    resourceFeature.addStaticResource("sample-data/users", () ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://sample-data/users")
        .setName("sample-data/users")
        .setTitle("Sample Users Dataset")
        .setDescription("Example dataset containing user information")
        .setMimeType("application/json")
        .setText(users.encodePrettily()))
    );

    // Static Resource: Sample Products Data
    resourceFeature.addStaticResource("sample-data/products", () ->
      Future.succeededFuture(new TextResourceContent()
        .setUri("resource://sample-data/products")
        .setName("sample-data/products")
        .setTitle("Sample Products Dataset")
        .setDescription("Example dataset containing product information")
        .setMimeType("application/json")
        .setText(products.encodePrettily()))
    );

    // Dynamic Resource: Individual User by ID
    resourceFeature.addDynamicResource("resource://user/{id}", params -> {
      String idStr = params.get("id");
      try {
        int id = Integer.parseInt(idStr);

        // Find user by ID
        for (int i = 0; i < users.size(); i++) {
          JsonObject user = users.getJsonObject(i);
          if (user.getInteger("id") == id) {
            return Future.succeededFuture(new TextResourceContent()
              .setUri("resource://user/" + id)
              .setName("user-" + id)
              .setTitle("User: " + user.getString("name"))
              .setDescription("Details for user ID " + id)
              .setMimeType("application/json")
              .setText(user.encodePrettily()));
          }
        }

        return Future.failedFuture("User not found: " + id);
      } catch (NumberFormatException e) {
        return Future.failedFuture("Invalid user ID: " + idStr);
      }
    });

    // Dynamic Resource: Individual Product by ID
    resourceFeature.addDynamicResource("resource://product/{id}", params -> {
      String idStr = params.get("id");
      try {
        int id = Integer.parseInt(idStr);

        // Find product by ID
        for (int i = 0; i < products.size(); i++) {
          JsonObject product = products.getJsonObject(i);
          if (product.getInteger("id") == id) {
            return Future.succeededFuture(new TextResourceContent()
              .setUri("resource://product/" + id)
              .setName("product-" + id)
              .setTitle("Product: " + product.getString("name"))
              .setDescription("Details for product ID " + id)
              .setMimeType("application/json")
              .setText(product.encodePrettily()));
          }
        }

        return Future.failedFuture("Product not found: " + id);
      } catch (NumberFormatException e) {
        return Future.failedFuture("Invalid product ID: " + idStr);
      }
    });

    server.serverFeatures(resourceFeature);
  }
}
