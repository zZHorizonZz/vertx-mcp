package io.vertx.mcp.server.transport.http;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.ServerRequest;
import io.vertx.mcp.server.ServerResponse;
import io.vertx.mcp.server.Session;
import io.vertx.mcp.server.transport.http.SessionManager;

public class HttpServerRequestImpl implements ServerRequest {

  private final ContextInternal context;
  private final HttpServerRequest httpRequest;
  private final SessionManager sessionManager;
  private final ServerOptions options;

  private ServerResponse response;
  private Handler<JsonObject> dataHandler;
  private Handler<Void> endHandler;
  private Handler<Throwable> exceptionHandler;
  private boolean paused = false;
  private JsonRequest jsonRequest;
  private Session session;
  private boolean sseCandidate = false;

  public HttpServerRequestImpl(Context context, HttpServerRequest httpRequest, SessionManager sessionManager, ServerOptions options) {
    this.context = (ContextInternal) context;
    this.httpRequest = httpRequest;
    this.sessionManager = sessionManager;
    this.options = options;
  }

  public void setSseCandidate(boolean sseCandidate) {
    this.sseCandidate = sseCandidate;
  }

  @Override
  public void init(ServerResponse response) {
    this.response = response;
    response.init();

    // Read the HTTP body and parse it as JSON-RPC
    httpRequest.body().onComplete(ar -> {
      if (ar.succeeded()) {
        Buffer body = ar.result();
        try {
          // Try to parse as JSON
          String bodyStr = body.toString();
          if (bodyStr.trim().startsWith("[")) {
            // Batch request
            JsonArray array = new JsonArray(bodyStr);
            // For batch requests, we need to handle them as a single JsonObject
            // wrapping the array for processing
            JsonObject batchWrapper = new JsonObject().put("batch", array);
            if (dataHandler != null && !paused) {
              dataHandler.handle(batchWrapper);
            }
          } else {
            // Single request
            JsonObject json = new JsonObject(bodyStr);

            // Parse JSON-RPC to check method and if it's a notification
            JsonRequest tempRequest = JsonRequest.fromJson(json);

            // If this is an initialize request and sessions are enabled, create a new session
            if ("initialize".equals(tempRequest.getMethod()) && options.getSessionsEnabled() && session == null) {
              String sessionId = sessionManager.generateSessionId();
              HttpSession newSession = sessionManager.createSession(sessionId, httpRequest.response());
              session = newSession;
              response.setSession(newSession);

              // Mark the response to include the session ID header
              if (response instanceof HttpServerResponseImpl) {
                ((HttpServerResponseImpl) response).markNewSession();
              }
            }

            // If this is an SSE candidate (has session ID) and NOT a notification, enable SSE
            if (sseCandidate && !tempRequest.isNotification() && session != null) {
              session.enableSse();
            }

            if (dataHandler != null && !paused) {
              dataHandler.handle(json);
            }
          }
          if (endHandler != null) {
            endHandler.handle(null);
          }
        } catch (DecodeException e) {
          if (exceptionHandler != null) {
            exceptionHandler.handle(e);
          }
        }
      } else {
        if (exceptionHandler != null) {
          exceptionHandler.handle(ar.cause());
        }
      }
    });
  }

  @Override
  public String path() {
    // In MCP, the path is the JSON-RPC method field, not the HTTP path
    return jsonRequest != null ? jsonRequest.getMethod() : null;
  }

  @Override
  public ContextInternal context() {
    return context;
  }

  @Override
  public ServerResponse response() {
    return response;
  }

  @Override
  public ReadStream<JsonObject> pause() {
    paused = true;
    return this;
  }

  @Override
  public ReadStream<JsonObject> resume() {
    paused = false;
    return this;
  }

  @Override
  public ReadStream<JsonObject> fetch(long amount) {
    // HTTP body is read all at once, so fetch is a no-op
    return this;
  }

  @Override
  public ReadStream<JsonObject> endHandler(@Nullable Handler<Void> handler) {
    this.endHandler = handler;
    return this;
  }

  @Override
  public ReadStream<JsonObject> exceptionHandler(@Nullable Handler<Throwable> handler) {
    this.exceptionHandler = handler;
    return this;
  }

  @Override
  public ReadStream<JsonObject> handler(@Nullable Handler<JsonObject> handler) {
    this.dataHandler = handler;
    return this;
  }

  @Override
  public JsonRequest getJsonRequest() {
    return jsonRequest;
  }

  @Override
  public void setJsonRequest(JsonRequest request) {
    this.jsonRequest = request;
  }

  @Override
  public Session session() {
    return session;
  }

  @Override
  public void setSession(Session session) {
    this.session = session;
  }
}
