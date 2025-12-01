package io.vertx.mcp.client.transport.http;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.client.ClientResponse;
import io.vertx.mcp.client.ModelContextProtocolClient;
import io.vertx.mcp.client.impl.ClientSessionImpl;
import io.vertx.mcp.common.rpc.JsonResponse;

/**
 * Handles Server-Sent Events (SSE) from the MCP server.
 * Parses SSE data and dispatches individual responses to the client.
 * This is only used for streaming connections (GET with text/event-stream).
 */
public class StreamableHttpClientResponse implements Handler<Buffer> {

  private final ContextInternal context;
  private final io.vertx.core.http.HttpClientResponse httpResponse;
  private final ClientSessionImpl session;
  private final ModelContextProtocolClient client;
  private final StringBuilder buffer = new StringBuilder();

  public StreamableHttpClientResponse(
    ContextInternal context,
    io.vertx.core.http.HttpClientResponse httpResponse,
    ClientSessionImpl session,
    ModelContextProtocolClient client
  ) {
    this.context = context;
    this.httpResponse = httpResponse;
    this.session = session;
    this.client = client;
  }

  @Override
  public void handle(Buffer event) {
    String data = event.toString();
    buffer.append(data);

    // Process complete SSE messages
    String buffered = buffer.toString();
    String[] lines = buffered.split("\n");

    StringBuilder currentMessage = new StringBuilder();
    int lastProcessedIndex = -1;

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];

      if (line.isEmpty() || line.equals("\r")) {
        // End of SSE message
        if (currentMessage.length() > 0) {
          processMessage(currentMessage.toString());
          currentMessage.setLength(0);
          lastProcessedIndex = i;
        }
      } else if (line.startsWith("data:")) {
        // SSE data line
        String messageData = line.substring(5).trim();
        currentMessage.append(messageData);
      }
      // Ignore other SSE fields like event:, id:, retry:
    }

    // Keep unprocessed data in buffer
    if (lastProcessedIndex >= 0 && lastProcessedIndex < lines.length - 1) {
      buffer.setLength(0);
      for (int i = lastProcessedIndex + 1; i < lines.length; i++) {
        buffer.append(lines[i]);
        if (i < lines.length - 1) {
          buffer.append("\n");
        }
      }
    } else if (lastProcessedIndex == lines.length - 1) {
      buffer.setLength(0);
    }
  }

  /**
   * Processes a complete SSE message.
   * Creates a ClientResponse for each message and dispatches it to the client.
   *
   * @param message the message data
   */
  private void processMessage(String message) {
    try {
      JsonObject json = new JsonObject(message);
      JsonResponse jsonResponse = JsonResponse.fromJson(json);

      // Check if this is a response to a pending request
      Object requestId = jsonResponse.getId();
      if (requestId != null) {
        // Complete pending request
        if (jsonResponse.getError() != null) {
          session.failRequest(requestId, new RuntimeException(
            "RPC Error " + jsonResponse.getError().getCode() + ": " + jsonResponse.getError().getMessage()
          ));
        } else {
          session.completeRequest(requestId, jsonResponse.getResult());
        }
      }

      // Create a ClientResponse for this SSE event
      HttpClientResponse clientResponse = new HttpClientResponse(context, jsonResponse);
      clientResponse.init(session, null);

      // Dispatch to client for notification handling
      context.dispatch(clientResponse, client);

    } catch (Exception e) {
      // Log error but continue processing
      System.err.println("Failed to process SSE message: " + e.getMessage());
    }
  }

  /**
   * Gets the underlying HTTP client response.
   *
   * @return the HTTP client response
   */
  public io.vertx.core.http.HttpClientResponse getHttpResponse() {
    return httpResponse;
  }
}


