package io.vertx.mcp.server.transport.http;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.Session;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpSession implements Session {

  private final String id;
  private final HttpServerResponse httpResponse;
  private final AtomicBoolean active = new AtomicBoolean(true);
  private volatile boolean sseEnabled = false;
  private volatile boolean headersWritten = false;

  public HttpSession(HttpServerResponse httpResponse) {
    this.id = UUID.randomUUID().toString();
    this.httpResponse = httpResponse;
  }

  public HttpSession(String id, HttpServerResponse httpResponse) {
    this.id = id;
    this.httpResponse = httpResponse;
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public boolean isSse() {
    return sseEnabled;
  }

  @Override
  public void enableSse() {
    if (!headersWritten) {
      sseEnabled = true;
      httpResponse.setChunked(true);
      httpResponse.putHeader("Content-Type", "text/event-stream");
      httpResponse.putHeader("Cache-Control", "no-cache");
      httpResponse.putHeader("Connection", "keep-alive");
      headersWritten = true;
    }
  }

  @Override
  public boolean isActive() {
    return active.get();
  }

  @Override
  public Future<Void> send(JsonResponse response) {
    if (!active.get()) {
      return Future.failedFuture("Session is not active");
    }

    if (sseEnabled) {
      // Send as SSE event
      String data = "data: " + response.toJson().encode() + "\n\n";
      return httpResponse.write(data);
    } else {
      // Send as regular HTTP response and close
      active.set(false);
      return httpResponse.end(response.toJson().toBuffer());
    }
  }

  @Override
  public Future<Void> close() {
    if (active.compareAndSet(true, false)) {
      if (sseEnabled) {
        return httpResponse.end();
      }
    }
    return Future.succeededFuture();
  }
}
