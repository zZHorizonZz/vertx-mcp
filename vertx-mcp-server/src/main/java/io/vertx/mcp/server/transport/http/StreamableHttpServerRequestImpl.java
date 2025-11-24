package io.vertx.mcp.server.transport.http;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;
import io.vertx.mcp.common.request.InitializeRequest;
import io.vertx.mcp.common.rpc.JsonNotification;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonRequestDecoder;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.*;
import io.vertx.mcp.server.impl.ServerSessionImpl;

public class StreamableHttpServerRequestImpl implements ServerRequest {

  private final ContextInternal context;
  private final HttpServerRequest httpRequest;
  private final SessionManager sessionManager;
  private final ServerOptions options;

  private StreamableHttpServerResponseImpl response;
  private Handler<Void> requestHandler;
  private Handler<Throwable> exceptionHandler;

  private JsonRequest jsonRequest;
  private ServerSession session;

  public StreamableHttpServerRequestImpl(Context context, HttpServerRequest httpRequest, SessionManager sessionManager, ServerOptions options) {
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

    if (!(response instanceof StreamableHttpServerResponseImpl)) {
      throw new IllegalArgumentException("Response must be an instance of StreamableHttpServerResponseImpl");
    }

    this.response = (StreamableHttpServerResponseImpl) response;

    response.init(session);

    if (httpRequest.method().equals(HttpMethod.GET)) {
      if (session == null) {
        httpRequest.response().setStatusCode(404).end("Session not found");
        return;
      }

      if (!this.session.isStreaming()) {
        ((ServerSessionImpl) this.session).init(this.response);
      }

      httpRequest.response().setChunked(true);

      httpRequest.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8");
      httpRequest.response().putHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
      httpRequest.response().putHeader(HttpHeaders.CONNECTION, "keep-alive");

      httpRequest.response().writeHead();
    }

    httpRequest.bodyHandler(body -> {
      try {
        if (body.length() == 0 && httpRequest.method().equals(HttpMethod.GET)) {
          return;
        }

        JsonObject json = new JsonObject(body.toString());

        if (!json.containsKey("method") && (json.containsKey("result") || json.containsKey("error"))) {
          if (session == null) {
            httpRequest.response().setStatusCode(400).end("Session required for responses");
            return;
          }

          JsonResponse jsonResponse = JsonResponse.fromJson(json);
          ServerSessionImpl sessionImpl = (ServerSessionImpl) session;
          Promise<JsonObject> promise = sessionImpl.getPendingRequests().remove(jsonResponse.getId());

          if (promise == null) {
            httpRequest.response().setStatusCode(400).end("Unknown request ID");
            return;
          }

          if (jsonResponse.isSuccess()) {
            Object resultValue = jsonResponse.getResult();
            JsonObject resultJson = resultValue instanceof JsonObject ? (JsonObject) resultValue : new JsonObject();
            promise.complete(resultJson);
          } else {
            promise.fail(new RuntimeException(jsonResponse.getError().getMessage()));
          }

          httpRequest.response().setStatusCode(202);
          httpRequest.response().end();
          return;
        }

        this.jsonRequest = JsonRequestDecoder.fromJson(json);

        if (this.jsonRequest.getMethod().equals("initialize") && options.getSessionsEnabled() && session == null) {
          InitializeRequest initialize = new InitializeRequest(json);
          httpRequest.response().putHeader(StreamableHttpServerTransport.MCP_SESSION_ID_HEADER, sessionManager.createSession(initialize.getCapabilities()).id());
        }

        if (this.session != null && options.getStreamingEnabled() && !(this.jsonRequest instanceof JsonNotification)) {
          if (!this.session.isStreaming()) {
            ((ServerSessionImpl) this.session).init(this.response);
          }

          HttpServerResponse httpResponse = httpRequest.response();

          httpResponse.setChunked(true);
          httpResponse.putHeader(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8");
          httpResponse.putHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
          httpResponse.putHeader(HttpHeaders.CONNECTION, "keep-alive");
        }

        this.response.requestId(this.jsonRequest.getId());

        if (this.jsonRequest.getNamedParams() != null && !this.jsonRequest.getNamedParams().isEmpty()) {
          this.context.put(Meta.MCP_META_CONTEXT_KEY, this.jsonRequest.getNamedParams().getJsonObject(Meta.META_KEY, new JsonObject()));
        }

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
