/*
 * Copyright (c) 2011-2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.tests.server;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.json.schema.common.dsl.Schemas;
import io.vertx.mcp.common.LogLevel;
import io.vertx.mcp.common.Meta;
import io.vertx.mcp.common.completion.Completion;
import io.vertx.mcp.common.content.*;
import io.vertx.mcp.common.notification.LoggingMessageNotification;
import io.vertx.mcp.common.notification.ProgressNotification;
import io.vertx.mcp.common.prompt.PromptMessage;
import io.vertx.mcp.common.request.CreateMessageRequest;
import io.vertx.mcp.common.request.ElicitRequest;
import io.vertx.mcp.common.resources.BinaryResourceContent;
import io.vertx.mcp.common.resources.TextResourceContent;
import io.vertx.mcp.common.sampling.SamplingMessage;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.PromptHandler;
import io.vertx.mcp.server.ServerSession;
import io.vertx.mcp.server.feature.*;
import org.junit.Test;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * This test uses Testcontainers to run MCP conformance tests in a Docker container. It requires Docker to be installed and running.
 * <p>
 * Maven profile: mvn test -Pmcp-conformance
 */
public class ConformanceTest extends TestContainerTestBase {

  private ImageFromDockerfile conformanceImage;

  // Sample 1x1 red pixel PNG (base64)
  private static final String MINIMAL_PNG_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

  // Sample WAV file (base64)
  private static final String MINIMAL_WAV_BASE64 = "UklGRiYAAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YQIAAAAAAA==";

  @Override
  public void setUp(TestContext context) {
    super.setUp(context);

    ModelContextProtocolServer mcpServer = ModelContextProtocolServer.create(super.vertx);

    setupTools(mcpServer);
    setupLogging(mcpServer);
    setupPrompts(mcpServer);
    setupResources(mcpServer);
    setupCompletions(mcpServer);

    startServer(context, mcpServer);

    exposeHostPort();

    File dockerfile = new File("src/test/resources/conformance.Dockerfile");
    conformanceImage = new ImageFromDockerfile().withFileFromFile("Dockerfile", dockerfile);
  }

  private void setupLogging(ModelContextProtocolServer mcpServer) {
    LoggingServerFeature loggingFeature = new LoggingServerFeature();
    mcpServer.addServerFeature(loggingFeature);
  }

  private void setupTools(ModelContextProtocolServer mcpServer) {
    ToolServerFeature toolFeature = new ToolServerFeature();

    // test_simple_text - returns simple text content
    toolFeature.addUnstructuredTool(
      "test_simple_text",
      "Simple Text Tool",
      "Returns simple text content",
      Schemas.objectSchema(),
      args -> Future.succeededFuture(new Content[] {
        new TextContent("This is a simple text response for testing.")
      })
    );

    // test_image_content - returns image content
    toolFeature.addUnstructuredTool(
      "test_image_content",
      "Image Content Tool",
      "Returns image content",
      Schemas.objectSchema(),
      args -> {
        Buffer imageData = Buffer.buffer(Base64.getDecoder().decode(MINIMAL_PNG_BASE64));
        return Future.succeededFuture(new Content[] {
          new ImageContent("image/png", imageData)
        });
      }
    );

    // test_audio_content - returns audio content
    toolFeature.addUnstructuredTool(
      "test_audio_content",
      "Audio Content Tool",
      "Returns audio content",
      Schemas.objectSchema(),
      args -> {
        Buffer audioData = Buffer.buffer(Base64.getDecoder().decode(MINIMAL_WAV_BASE64));
        return Future.succeededFuture(new Content[] {
          new AudioContent("audio/wav", audioData)
        });
      }
    );

    // test_embedded_resource - returns embedded resource
    toolFeature.addUnstructuredTool(
      "test_embedded_resource",
      "Embedded Resource Tool",
      "Returns embedded resource content",
      Schemas.objectSchema(),
      args -> {
        TextResourceContent resource = new TextResourceContent()
          .setUri("test://embedded-resource")
          .setName("Embedded Resource")
          .setMimeType("text/plain")
          .setText("This is an embedded resource content.");
        return Future.succeededFuture(new Content[] {
          new EmbeddedResourceContent(resource)
        });
      }
    );

    // test_multiple_content_types - returns text, image, and resource
    toolFeature.addUnstructuredTool(
      "test_multiple_content_types",
      "Multiple Content Types Tool",
      "Returns multiple content types",
      Schemas.objectSchema(),
      args -> {
        Buffer imageData = Buffer.buffer(Base64.getDecoder().decode(MINIMAL_PNG_BASE64));
        TextResourceContent resource = new TextResourceContent()
          .setUri("test://mixed-content-resource")
          .setName("Mixed Content Resource")
          .setMimeType("application/json")
          .setText(new JsonObject().put("test", "data").put("value", 123).encode());
        return Future.succeededFuture(new Content[] {
          new TextContent("Multiple content types test:"),
          new ImageContent("image/png", imageData),
          new EmbeddedResourceContent(resource)
        });
      }
    );

    // test_tool_with_logging - sends log notifications during execution
    toolFeature.addUnstructuredTool(
      "test_tool_with_logging",
      "Logging Tool",
      "Sends log messages during execution",
      Schemas.objectSchema(),
      args -> {
        // Get session from context
        Context context = Vertx.currentContext();
        ServerSession session = ServerSession.fromContext(context);

        if (session == null) {
          return Future.succeededFuture(new Content[] {
            new TextContent("Tool execution completed (no session available).")
          });
        }

        // Send 3 log notifications
        return session.sendNotification(new LoggingMessageNotification()
            .setLevel(LogLevel.INFO)
            .setLogger("test_tool")
            .setData("Tool execution started"))
          .compose(v -> {
            // Wait 50ms
            return Future.future(promise -> Vertx.currentContext().owner().setTimer(50, id -> promise.complete()));
          })
          .compose(v -> session.sendNotification(new LoggingMessageNotification()
            .setLevel(LogLevel.INFO)
            .setLogger("test_tool")
            .setData("Tool processing data")))
          .compose(v -> {
            // Wait 50ms
            return Future.future(promise -> Vertx.currentContext().owner().setTimer(50, id -> promise.complete()));
          })
          .compose(v -> session.sendNotification(new LoggingMessageNotification()
            .setLevel(LogLevel.INFO)
            .setLogger("test_tool")
            .setData("Tool execution completed")))
          .map(v -> new Content[] {
            new TextContent("Tool execution completed with logging.")
          });
      }
    );

    // test_error_handling - returns error with isError: true
    toolFeature.addUnstructuredTool(
      "test_error_handling",
      "Error Handling Tool",
      "Returns an error for testing",
      Schemas.objectSchema(),
      args -> Future.failedFuture(new RuntimeException("This tool intentionally returns an error for testing"))
    );

    // test_tool_with_progress - reports progress notifications
    toolFeature.addUnstructuredTool(
      "test_tool_with_progress",
      "Progress Tool",
      "Reports progress during execution",
      Schemas.objectSchema(),
      args -> {
        // Get meta and session from context
        Context context = Vertx.currentContext();
        JsonObject meta = Meta.fromContext(context);
        ServerSession session = ServerSession.fromContext(context);

        if (meta == null || session == null) {
          return Future.succeededFuture(new Content[] {
            new TextContent("Tool execution completed (no progress token available).")
          });
        }

        String progressToken = meta.getString("progressToken");
        if (progressToken == null) {
          return Future.succeededFuture(new Content[] {
            new TextContent("Tool execution completed (no progress token provided).")
          });
        }

        // Send progress notifications
        return session.sendNotification(new ProgressNotification()
            .setProgressToken(progressToken)
            .setProgress(0.0)
            .setTotal(100.0))
          .compose(v -> {
            // Wait 50ms
            return Future.future(promise -> Vertx.currentContext().owner().setTimer(50, id -> promise.complete()));
          })
          .compose(v -> session.sendNotification(new ProgressNotification()
            .setProgressToken(progressToken)
            .setProgress(50.0)
            .setTotal(100.0)))
          .compose(v -> {
            // Wait 50ms
            return Future.future(promise -> Vertx.currentContext().owner().setTimer(50, id -> promise.complete()));
          })
          .compose(v -> session.sendNotification(new ProgressNotification()
            .setProgressToken(progressToken)
            .setProgress(100.0)
            .setTotal(100.0)))
          .map(v -> new Content[] {
            new TextContent("Tool execution completed with progress reporting.")
          });
      }
    );

    // test_sampling - requests LLM sampling from client
    toolFeature.addUnstructuredTool(
      "test_sampling",
      "Sampling Tool",
      "Requests LLM sampling from client",
      Schemas.objectSchema().requiredProperty("prompt", Schemas.stringSchema()),
      args -> {
        // Get session from context
        Context context = Vertx.currentContext();
        ServerSession session = ServerSession.fromContext(context);

        if (session == null) {
          return Future.failedFuture(new RuntimeException("No session available"));
        }

        String prompt = args.getString("prompt", "Test prompt");

        // Create sampling request
        SamplingMessage message = new SamplingMessage()
          .setRole("user")
          .setContent(new JsonObject()
            .put("type", "text")
            .put("text", prompt));

        CreateMessageRequest request = new CreateMessageRequest()
          .setMessages(List.of(message))
          .setMaxTokens(100);

        // Send request to client
        return session.sendRequest(request)
          .map(response -> {
            // Extract response text
            JsonObject content = response.getJsonObject("content");
            String responseText = content != null ? content.getString("text", "No response") : "No response";
            return new Content[] {
              new TextContent("LLM response: " + responseText)
            };
          });
      }
    );

    // test_elicitation - requests user input from client
    toolFeature.addUnstructuredTool(
      "test_elicitation",
      "Elicitation Tool",
      "Requests user input from client",
      Schemas.objectSchema().requiredProperty("message", Schemas.stringSchema()),
      args -> {
        // Get session from context
        Context context = Vertx.currentContext();
        ServerSession session = ServerSession.fromContext(context);

        if (session == null) {
          return Future.failedFuture(new RuntimeException("No session available"));
        }

        String message = args.getString("message", "Please provide information");

        // Create elicitation request with schema
        JsonObject requestedSchema = new JsonObject()
          .put("type", "object")
          .put("properties", new JsonObject()
            .put("username", new JsonObject()
              .put("type", "string")
              .put("description", "User's response"))
            .put("email", new JsonObject()
              .put("type", "string")
              .put("description", "User's email address")))
          .put("required", new JsonArray().add("username").add("email"));

        ElicitRequest request = new ElicitRequest()
          .setMessage(message)
          .setRequestedSchema(requestedSchema);

        // Send request to client
        return session.sendRequest(request)
          .map(response -> {
            String action = response.getString("action", "unknown");
            JsonObject content = response.getJsonObject("content", new JsonObject());
            return new Content[] {
              new TextContent("User response: action: " + action + ", content: " + content.encode())
            };
          });
      }
    );

    // test_elicitation_sep1034_defaults - elicitation with default values for all primitive types
    toolFeature.addUnstructuredTool(
      "test_elicitation_sep1034_defaults",
      "Elicitation SEP-1034 Defaults Tool",
      "Requests elicitation with default values for all primitive types",
      Schemas.objectSchema(),
      args -> {
        // Get session from context
        Context context = Vertx.currentContext();
        ServerSession session = ServerSession.fromContext(context);

        if (session == null) {
          return Future.failedFuture(new RuntimeException("No session available"));
        }

        // Create elicitation request with schema containing default values for all primitive types
        JsonObject requestedSchema = new JsonObject()
          .put("type", "object")
          .put("properties", new JsonObject()
            // String with default
            .put("name", new JsonObject()
              .put("type", "string")
              .put("description", "User's name")
              .put("default", "John Doe"))
            // Integer with default
            .put("age", new JsonObject()
              .put("type", "integer")
              .put("description", "User's age")
              .put("default", 30))
            // Number with default
            .put("score", new JsonObject()
              .put("type", "number")
              .put("description", "User's score")
              .put("default", 95.5))
            // Enum with default
            .put("status", new JsonObject()
              .put("type", "string")
              .put("description", "User's status")
              .put("enum", new JsonArray().add("active").add("inactive").add("pending"))
              .put("default", "active"))
            // Boolean with default
            .put("verified", new JsonObject()
              .put("type", "boolean")
              .put("description", "Whether user is verified")
              .put("default", true)))
          .put("required", new JsonArray().add("name").add("age").add("score").add("status").add("verified"));

        ElicitRequest request = new ElicitRequest()
          .setMessage("Please provide your information")
          .setRequestedSchema(requestedSchema);

        // Send request to client
        return session.sendRequest(request)
          .map(response -> {
            String action = response.getString("action", "unknown");
            JsonObject content = response.getJsonObject("content", new JsonObject());
            return new Content[] {
              new TextContent("Elicitation completed: action=" + action + ", content=" + content.encode())
            };
          });
      }
    );

    mcpServer.addServerFeature(toolFeature);
  }

  private void setupResources(ModelContextProtocolServer mcpServer) {
    ResourceServerFeature resourceFeature = new ResourceServerFeature();

    // test://static-text - text resource
    resourceFeature.addStaticResource(
      "test://static-text",
      "Static Text Resource",
      "A static text resource for testing",
      () -> Future.succeededFuture(new TextResourceContent()
        .setUri("test://static-text")
        .setName("Static Text Resource")
        .setMimeType("text/plain")
        .setText("This is the content of the static text resource."))
    );

    // test://static-binary - binary resource (PNG)
    resourceFeature.addStaticResource(
      "test://static-binary",
      "Static Binary Resource",
      "A static binary resource for testing",
      () -> {
        Buffer imageData = Buffer.buffer(Base64.getDecoder().decode(MINIMAL_PNG_BASE64));
        return Future.succeededFuture(new BinaryResourceContent()
          .setUri("test://static-binary")
          .setName("Static Binary Resource")
          .setMimeType("image/png")
          .setBlob(imageData));
      }
    );

    // test://template/{id}/data - dynamic resource with template
    resourceFeature.addDynamicResource(
      "test://template/{id}/data",
      "Template Resource",
      "A template resource with parameter substitution",
      params -> {
        String id = params.get("id");
        JsonObject data = new JsonObject()
          .put("id", id)
          .put("templateTest", true)
          .put("data", "Data for ID: " + id);
        return Future.succeededFuture(new TextResourceContent()
          .setUri("test://template/" + id + "/data")
          .setName("Template Resource " + id)
          .setMimeType("application/json")
          .setText(data.encode()));
      }
    );

    // test://watched-resource - for subscribe/unsubscribe tests
    resourceFeature.addStaticResource(
      "test://watched-resource",
      "Watched Resource",
      "A resource that can be subscribed to",
      () -> Future.succeededFuture(new TextResourceContent()
        .setUri("test://watched-resource")
        .setName("Watched Resource")
        .setMimeType("text/plain")
        .setText("This resource can be watched for updates."))
    );

    mcpServer.addServerFeature(resourceFeature);
  }

  private void setupPrompts(ModelContextProtocolServer mcpServer) {
    PromptServerFeature promptFeature = new PromptServerFeature();

    // test_simple_prompt - simple prompt (no arguments)
    // Required by prompts-get-simple scenario
    promptFeature.addPrompt(
      "test_simple_prompt",
      "Simple Prompt",
      "A simple prompt for testing",
      Schemas.arraySchema(),
      args -> {
        List<PromptMessage> messages = new ArrayList<>();
        messages.add(new PromptMessage()
          .setRole("user")
          .setContent(new TextContent("This is a simple prompt for testing.").toJson()));
        return Future.succeededFuture(messages);
      }
    );

    // test_prompt_with_arguments - prompt with arguments
    // Required by prompts-get-with-args scenario and completion-complete scenario
    promptFeature.addPrompt(PromptHandler.create(
      "test_prompt_with_arguments",
      "Prompt With Arguments",
      "A prompt that accepts arguments",
      Schemas.arraySchema().items(
        Schemas.objectSchema()
          .requiredProperty("arg1", Schemas.stringSchema())
          .requiredProperty("arg2", Schemas.stringSchema())
      ),
      args -> {
        String arg1 = args.getString("arg1", "");
        String arg2 = args.getString("arg2", "");
        List<PromptMessage> messages = new ArrayList<>();
        messages.add(new PromptMessage()
          .setRole("user")
          .setContent(new TextContent("Prompt with arguments: arg1='" + arg1 + "', arg2='" + arg2 + "'").toJson()));
        return Future.succeededFuture(messages);
      },
      (argument, context) -> {
        // Provide completion values for arg1
        if ("arg1".equals(argument.getName())) {
          String prefix = argument.getValue() != null ? argument.getValue() : "";
          List<String> values = new ArrayList<>();
          // Add some sample completions that start with the prefix
          for (String option : List.of("test", "testing", "tested", "tester", "text", "template")) {
            if (option.startsWith(prefix.toLowerCase())) {
              values.add(option);
            }
          }
          return Future.succeededFuture(new Completion()
            .setValues(values)
            .setTotal(values.size())
            .setHasMore(false));
        }
        return Future.succeededFuture(new Completion()
          .setValues(new ArrayList<>())
          .setTotal(0)
          .setHasMore(false));
      }
    ));

    // test_prompt_with_embedded_resource - prompt with embedded resource
    // Required by prompts-get-embedded-resource scenario
    promptFeature.addPrompt(
      "test_prompt_with_embedded_resource",
      "Prompt With Embedded Resource",
      "A prompt that includes an embedded resource",
      Schemas.arraySchema().items(
        Schemas.objectSchema()
          .requiredProperty("resourceUri", Schemas.stringSchema())
      ),
      args -> {
        String resourceUri = args.getString("resourceUri", "test://default");
        List<PromptMessage> messages = new ArrayList<>();
        // Message with embedded resource
        TextResourceContent resource = new TextResourceContent()
          .setUri(resourceUri)
          .setName("Embedded Resource")
          .setMimeType("text/plain")
          .setText("Embedded resource content for testing.");
        messages.add(new PromptMessage()
          .setRole("user")
          .setContent(new EmbeddedResourceContent(resource).toJson()));
        messages.add(new PromptMessage()
          .setRole("user")
          .setContent(new TextContent("Please process the embedded resource above.").toJson()));
        return Future.succeededFuture(messages);
      }
    );

    // test_prompt_with_image - prompt with image content
    // Required by prompts-get-with-image scenario
    promptFeature.addPrompt(
      "test_prompt_with_image",
      "Prompt With Image",
      "A prompt that includes an image",
      Schemas.arraySchema(),
      args -> {
        Buffer imageData = Buffer.buffer(Base64.getDecoder().decode(MINIMAL_PNG_BASE64));
        List<PromptMessage> messages = new ArrayList<>();
        messages.add(new PromptMessage()
          .setRole("user")
          .setContent(new ImageContent("image/png", imageData).toJson()));
        return Future.succeededFuture(messages);
      }
    );

    mcpServer.addServerFeature(promptFeature);
  }

  private void setupCompletions(ModelContextProtocolServer mcpServer) {
    CompletionServerFeature completionFeature = new CompletionServerFeature(mcpServer);
    mcpServer.addServerFeature(completionFeature);
  }

  @Test
  public void testAllScenarios(TestContext context) throws Throwable {
    String output = executeConformance(null).await(60, java.util.concurrent.TimeUnit.SECONDS);

    System.out.println("[mcp-conformance] Full output: " + output);

    // Count passed and failed
    int passedCount = 0;
    int failedCount = 0;

    // Extract totals from output
    if (output.contains("Total:")) {
      String[] lines = output.split("\n");
      for (String line : lines) {
        if (line.contains("Total:")) {
          // Parse "Total: X passed, Y failed"
          String[] parts = line.split(",");
          for (String part : parts) {
            if (part.contains("passed")) {
              passedCount = Integer.parseInt(part.replaceAll("[^0-9]", ""));
            } else if (part.contains("failed")) {
              failedCount = Integer.parseInt(part.replaceAll("[^0-9]", ""));
            }
          }
        }
      }
    }

    System.out.println("[mcp-conformance] Passed: " + passedCount + ", Failed: " + failedCount);

    context.assertTrue(failedCount == 0, "Expected 0 failed scenarios, got " + failedCount + ". Output: " + output);
  }

  private Future<String> executeConformance(String scenario) {
    List<String> command = new ArrayList<>();
    command.add("--url=http://" + HOST_INTERNAL + ":" + port + "/mcp");

    if (scenario != null) {
      command.add("--scenario=" + scenario);
    }

    return executeInContainer(conformanceImage, command, 60);
  }
}
