package io.vertx.mcp.server.transport.http;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.request.InitializeRequest;
import io.vertx.mcp.common.rpc.JsonNotification;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonRequestDecoder;
import io.vertx.mcp.server.*;
import io.vertx.mcp.server.impl.ServerSessionImpl;

public class HttpServerRequestImpl implements ServerRequest {

  private final ContextInternal context;
  private final HttpServerRequest httpRequest;
  private final SessionManager sessionManager;
  private final ServerOptions options;

  private HttpServerResponseImpl response;
  private Handler<Void> requestHandler;
  private Handler<Throwable> exceptionHandler;

  private JsonRequest jsonRequest;
  private ServerSession session;

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
  public void init(ServerSession session, ServerResponse response) {
    this.session = session;

    if (!(response instanceof HttpServerResponseImpl)) {
      throw new IllegalArgumentException("Response must be an instance of HttpServerResponseImpl");
    }

    this.response = (HttpServerResponseImpl) response;

    if (session != null) {
      ((ServerSessionImpl) session).init(this.response);
    }

    response.init(session);

    if (httpRequest.method().equals(HttpMethod.GET)) {
      if (session == null) {
        httpRequest.response().setStatusCode(404).end("Session not found");
        return;
      }

      httpRequest.response().setChunked(true);

      httpRequest.response().putHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
      httpRequest.response().putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
      httpRequest.response().putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, DELETE, OPTIONS");
      httpRequest.response().putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, String.join(",", HttpServerTransport.ACCEPTED_HEADERS));
      httpRequest.response().putHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, String.join(",", HttpServerTransport.ACCEPTED_HEADERS));

      httpRequest.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8");
      httpRequest.response().putHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
      httpRequest.response().putHeader(HttpHeaders.CONNECTION, "keep-alive");

      httpRequest.response().writeHead();
    }

    // Directly read the body and parse as a single JsonRequest
    // MCP always sends a single JSON-RPC request per HTTP request
    httpRequest.bodyHandler(body -> {
      try {
        if (body.length() == 0 && httpRequest.method().equals(HttpMethod.GET)) {
          return;
        }

        this.jsonRequest = JsonRequestDecoder.fromJson(new JsonObject(body.toString()));

        httpRequest.response().putHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
        httpRequest.response().putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        httpRequest.response().putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, DELETE, OPTIONS");
        httpRequest.response().putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, String.join(",", HttpServerTransport.ACCEPTED_HEADERS));
        httpRequest.response().putHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, String.join(",", HttpServerTransport.ACCEPTED_HEADERS));

        // If this is an initialize request and sessions are enabled, create a new session
        if (this.jsonRequest.getMethod().equals("initialize") && options.getSessionsEnabled() && session == null) {
          InitializeRequest initialize = new InitializeRequest(new JsonObject(body.toString()));
          httpRequest.response().putHeader(HttpServerTransport.MCP_SESSION_ID_HEADER, sessionManager.createSession(initialize.getCapabilities()).id());
        }

        if (this.session != null && options.getStreamingEnabled() && !(this.jsonRequest instanceof JsonNotification)) {
          HttpServerResponse httpResponse = httpRequest.response();

          httpResponse.setChunked(true);
          httpResponse.putHeader(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8");
          httpResponse.putHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
          httpResponse.putHeader(HttpHeaders.CONNECTION, "keep-alive");
        }

        this.response.requestId(this.jsonRequest.getId());

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
  public ServerSession session() {
    return session;
  }
}
