package io.vertx.mcp.client.transport.http;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.concurrent.InboundMessageQueue;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import io.vertx.mcp.client.ClientRequest;
import io.vertx.mcp.client.ClientResponse;
import io.vertx.mcp.client.ClientSession;
import io.vertx.mcp.client.impl.ClientSessionImpl;
import io.vertx.mcp.common.rpc.JsonResponse;

public class StreamableHttpClientResponse implements ClientResponse, Handler<Buffer> {

  private final ContextInternal context;
  private final io.vertx.core.http.HttpClientResponse httpResponse;
  private final InboundMessageQueue<JsonObject> queue;
  private final StringBuilder sseBuffer = new StringBuilder();

  private ClientSession session;
  private ClientRequest request;
  private Handler<JsonObject> messageHandler;
  private Handler<Throwable> exceptionHandler;
  private Handler<Void> endHandler;
  private boolean isStreaming;
  private JsonObject last;
  private JsonResponse jsonResponse;
  private final Promise<Void> end;

  public StreamableHttpClientResponse(
    ContextInternal context,
    HttpClientResponse httpResponse,
    ClientSession session,
    ClientRequest request
  ) {
    this.context = context;
    this.httpResponse = httpResponse;
    this.session = session;
    this.request = request;
    this.queue = new InboundMessageQueue<>(context.executor(), context.executor(), 8, 16) {
      @Override
      protected void handleResume() {
        httpResponse.resume();
      }

      @Override
      protected void handlePause() {
        httpResponse.pause();
      }

      @Override
      protected void handleMessage(JsonObject msg) {
        if (!StreamableHttpClientResponse.this.isStreaming) {
          handleEnd();
        } else {
          StreamableHttpClientResponse.this.handleMessage(msg);
        }
      }
    };

    // Determine if this is a streaming response
    String contentType = httpResponse.getHeader(HttpHeaders.CONTENT_TYPE);
    this.isStreaming = contentType != null && contentType.contains("text/event-stream");
    this.end = context.promise();
  }

  @Override
  public void init(ClientSession session, ClientRequest request) {
    this.session = session;
    this.request = request;

    httpResponse.handler(this);

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

  @Override
  public void handle(Buffer event) {
    if (isStreaming) {
      handleServerSentEvent(event);
    } else {
      try {
        String data = event.toString();
        JsonObject json = new JsonObject(data);
        jsonResponse = JsonResponse.fromJson(json);
        if (jsonResponse.getError() != null) {
          if (exceptionHandler != null) {
            exceptionHandler.handle(new RuntimeException(
              "RPC Error " + jsonResponse.getError().getCode() + ": " + jsonResponse.getError().getMessage()
            ));
          }
        } else {
          queue.write(json);
        }
      } catch (Exception e) {
        if (exceptionHandler != null) {
          exceptionHandler.handle(e);
        }
      }
    }
  }

  private void handleServerSentEvent(Buffer buffer) {
    String data = buffer.toString();
    sseBuffer.append(data);

    String buffered = sseBuffer.toString();
    String[] lines = buffered.split("\n");

    StringBuilder currentMessage = new StringBuilder();
    int lastProcessedIndex = -1;

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];

      if (line.isEmpty() || line.equals("\r")) {
        if (currentMessage.length() > 0) {
          try {
            JsonObject json = new JsonObject(currentMessage.toString());
            queue.write(json);
          } catch (Exception e) {
            if (exceptionHandler != null) {
              exceptionHandler.handle(e);
            }
          }
          currentMessage.setLength(0);
          lastProcessedIndex = i;
        }
      } else if (line.startsWith("data:")) {
        String messageData = line.substring(5).trim();
        currentMessage.append(messageData);
      }
    }

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

  protected void handleEnd() {
    end.tryComplete();
    Handler<Void> handler = endHandler;
    if (handler != null) {
      context.dispatch(handler);
    }
  }

  private void handleMessage(JsonObject msg) {
    last = msg;

    // Handle request completion for JSON-RPC responses
    if (msg.containsKey("id") || msg.containsKey("result") || msg.containsKey("error")) {
      JsonResponse response = JsonResponse.fromJson(msg);

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

    Handler<JsonObject> handler = messageHandler;
    if (handler != null) {
      context.dispatch(msg, messageHandler);
    }
  }

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

  public HttpClientResponse getHttpResponse() {
    return httpResponse;
  }

  public boolean isStreaming() {
    return isStreaming;
  }
}


