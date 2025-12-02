package io.vertx.mcp.client.transport.http;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import io.vertx.mcp.client.ClientRequest;
import io.vertx.mcp.client.ClientResponse;
import io.vertx.mcp.client.ClientSession;
import io.vertx.mcp.client.impl.ClientSessionImpl;
import io.vertx.mcp.common.rpc.JsonResponse;

/**
 * Handles HTTP responses from the MCP server, including both regular JSON responses and Server-Sent Events (SSE) streams. This class is inspired by the Vert.x gRPC client
 * implementation.
 */
public class StreamableHttpClientResponse implements ClientResponse {

  private final ContextInternal context;
  private final io.vertx.core.http.HttpClientResponse httpResponse;
  private final StringBuilder sseBuffer = new StringBuilder();

  private ClientSession session;
  private ClientRequest request;
  private Handler<JsonObject> messageHandler;
  private Handler<Throwable> exceptionHandler;
  private Handler<Void> endHandler;
  private boolean isStreaming;
  private JsonResponse jsonResponse;

  public StreamableHttpClientResponse(
    ContextInternal context,
    io.vertx.core.http.HttpClientResponse httpResponse,
    ClientSession session,
    ClientRequest request
  ) {
    this.context = context;
    this.httpResponse = httpResponse;
    this.session = session;
    this.request = request;

    // Determine if this is a streaming response
    String contentType = httpResponse.getHeader(HttpHeaders.CONTENT_TYPE);
    this.isStreaming = contentType != null && contentType.contains("text/event-stream");
  }

  @Override
  public void init(ClientSession session, ClientRequest request) {
    this.session = session;
    this.request = request;

    // Set up the response handler
    if (isStreaming) {
      setupStreamingHandler();
    } else {
      setupRegularHandler();
    }
  }

  /**
   * Sets up the handler for streaming (SSE) responses.
   */
  private void setupStreamingHandler() {
    httpResponse.handler(buffer -> {
      if (messageHandler != null) {
        handleSSEData(buffer);
      }
    });

    httpResponse.endHandler(v -> {
      if (endHandler != null) {
        endHandler.handle(null);
      }
    });

    httpResponse.exceptionHandler(err -> {
      if (exceptionHandler != null) {
        exceptionHandler.handle(err);
      }
    });
  }

  /**
   * Sets up the handler for regular JSON responses.
   */
  private void setupRegularHandler() {
    httpResponse.body()
      .onSuccess(body -> {
        try {
          JsonObject json = new JsonObject(body);
          if (messageHandler != null) {
            messageHandler.handle(json);
          }
          if (endHandler != null) {
            endHandler.handle(null);
          }
        } catch (Exception e) {
          if (exceptionHandler != null) {
            exceptionHandler.handle(e);
          }
        }
      })
      .onFailure(err -> {
        if (exceptionHandler != null) {
          exceptionHandler.handle(err);
        }
      });
  }

  /**
   * Handles incoming SSE data by parsing and emitting complete messages.
   *
   * @param buffer the incoming data buffer
   */
  private void handleSSEData(Buffer buffer) {
    String data = buffer.toString();
    sseBuffer.append(data);

    // Process complete SSE messages
    String buffered = sseBuffer.toString();
    String[] lines = buffered.split("\n");

    StringBuilder currentMessage = new StringBuilder();
    int lastProcessedIndex = -1;

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];

      if (line.isEmpty() || line.equals("\r")) {
        // End of SSE message
        if (currentMessage.length() > 0) {
          emitSSEMessage(currentMessage.toString());
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
      sseBuffer.setLength(0);
      for (int i = lastProcessedIndex + 1; i < lines.length; i++) {
        sseBuffer.append(lines[i]);
        if (i < lines.length - 1) {
          sseBuffer.append("\n");
        }
      }
    } else if (lastProcessedIndex == lines.length - 1) {
      sseBuffer.setLength(0);
    }
  }

  /**
   * Emits a complete SSE message to the handler.
   *
   * @param message the message data
   */
  private void emitSSEMessage(String message) {
    try {
      JsonObject json = new JsonObject(message);

      // Handle as a JSON response if it contains RPC fields
      if (json.containsKey("id") || json.containsKey("result") || json.containsKey("error")) {
        JsonResponse response = JsonResponse.fromJson(json);

        // If this is a response to a pending request, complete it
        Object requestId = response.getId();
        if (requestId != null && session instanceof ClientSessionImpl) {
          ClientSessionImpl sessionImpl = (ClientSessionImpl) session;
          if (response.getError() != null) {
            sessionImpl.failRequest(requestId, new RuntimeException(
              "RPC Error " + response.getError().getCode() + ": " + response.getError().getMessage()
            ));
          } else {
            Object result = response.getResult();
            JsonObject resultJson = result instanceof JsonObject ? (JsonObject) result : null;
            sessionImpl.completeRequest(requestId, resultJson);
          }
        }
      }

      // Emit the JSON object to the message handler
      if (messageHandler != null) {
        messageHandler.handle(json);
      }

    } catch (Exception e) {
      if (exceptionHandler != null) {
        exceptionHandler.handle(e);
      }
    }
  }

  // ReadStream implementation

  @Override
  public StreamableHttpClientResponse handler(Handler<JsonObject> handler) {
    this.messageHandler = handler;
    return this;
  }

  @Override
  public StreamableHttpClientResponse pause() {
    httpResponse.pause();
    return this;
  }

  @Override
  public StreamableHttpClientResponse resume() {
    httpResponse.resume();
    return this;
  }

  @Override
  public StreamableHttpClientResponse fetch(long amount) {
    httpResponse.fetch(amount);
    return this;
  }

  @Override
  public StreamableHttpClientResponse endHandler(Handler<Void> handler) {
    this.endHandler = handler;
    return this;
  }

  @Override
  public StreamableHttpClientResponse exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;
    return this;
  }

  @Override
  public Future<Void> pipeTo(WriteStream<JsonObject> dst) {
    return ClientResponse.super.pipeTo(dst);
  }

  @Override
  public Object requestId() {
    return request != null ? request.requestId() : null;
  }

  @Override
  public ClientSession session() {
    return session;
  }

  @Override
  public ContextInternal context() {
    return context;
  }

  @Override
  public JsonResponse getJsonResponse() {
    return jsonResponse;
  }

  @Override
  public ClientRequest request() {
    return request;
  }

  /**
   * Gets the underlying HTTP client response.
   *
   * @return the HTTP client response
   */
  public HttpClientResponse getHttpResponse() {
    return httpResponse;
  }

  /**
   * Checks if this is a streaming (SSE) response.
   *
   * @return true if this is a streaming response
   */
  public boolean isStreaming() {
    return isStreaming;
  }
}


