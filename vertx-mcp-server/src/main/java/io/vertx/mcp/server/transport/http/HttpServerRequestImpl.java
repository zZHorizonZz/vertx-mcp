package io.vertx.mcp.server.transport.http;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.server.*;
import io.vertx.mcp.server.impl.SessionImpl;

public class HttpServerRequestImpl implements ServerRequest {

  private final ContextInternal context;
  private final HttpServerRequest httpRequest;
  private final SessionManager sessionManager;
  private final ServerOptions options;

  private ServerResponse response;
  private Handler<Void> requestHandler;
  private Handler<Throwable> exceptionHandler;

  private JsonRequest jsonRequest;
  private Session session;

  public HttpServerRequestImpl(Context context, HttpServerRequest httpRequest, SessionManager sessionManager, ServerOptions options) {
    this.context = (ContextInternal) context;
    this.httpRequest = httpRequest;
    this.sessionManager = sessionManager;
    this.options = options;
  }

  /**
   * Set a handler to be called when the request has been fully parsed and is ready to process.
   *
   * @param handler the handler
   */
  public void handler(Handler<Void> handler) {
    this.requestHandler = handler;
  }

  /**
   * Set an exception handler for errors during request processing.
   *
   * @param handler the exception handler
   */
  public void exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;
  }

  @Override
  public void init(Session session, ServerResponse response) {
    this.session = session;
    this.response = response;

    if (session != null) {
      ((SessionImpl) session).init(this.response);
    }

    response.init(session);

    // Directly read the body and parse as a single JsonRequest
    // MCP always sends a single JSON-RPC request per HTTP request
    httpRequest.bodyHandler(body -> {
      try {
        // Parse the complete body as JSON-RPC request
        String bodyStr = body.toString();
        JsonObject json = new JsonObject(bodyStr);

        // Parse JSON-RPC request
        JsonRequest tempRequest = JsonRequest.fromJson(json);
        this.jsonRequest = tempRequest;

        httpRequest.response().putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        httpRequest.response().putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, DELETE, OPTIONS");
        httpRequest.response().putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, String.join(",", HttpServerTransport.ACCEPTED_HEADERS));

        // If this is an initialize request and sessions are enabled, create a new session
        if (tempRequest.getMethod().equals("initialize") && options.getSessionsEnabled() && session == null) {
          httpRequest.response().putHeader(HttpServerTransport.MCP_SESSION_ID_HEADER, sessionManager.createSession().id());
        }

        if (this.session != null && options.getStreamingEnabled() && !this.jsonRequest.isNotification()) {
          HttpServerResponse httpResponse = httpRequest.response();

          httpResponse.setChunked(true);
          httpResponse.putHeader(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8");
          httpResponse.putHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
          httpResponse.putHeader(HttpHeaders.CONNECTION, "keep-alive");
        }

        // Notify that request is ready
        if (requestHandler != null) {
          requestHandler.handle(null);
        }
      } catch (DecodeException | IllegalArgumentException e) {
        if (exceptionHandler != null) {
          exceptionHandler.handle(e);
        }
      }
    });

    // Set exception handler for body reading errors
    httpRequest.exceptionHandler(t -> {
      if (exceptionHandler != null) {
        exceptionHandler.handle(t);
      }
    });
  }

  @Override
  public String path() {
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
  public JsonRequest getJsonRequest() {
    return jsonRequest;
  }

  @Override
  public Session session() {
    return session;
  }
}
