package io.vertx.mcp.server.transport.http;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.http.HttpServerRequestInternal;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.Session;
import io.vertx.mcp.server.SessionManager;
import io.vertx.mcp.server.impl.ModelContextProtocolServerImpl;
import io.vertx.mcp.server.impl.SessionManagerImpl;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class HttpServerTransport implements Handler<HttpServerRequest> {

  public static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";
  public static final String MCP_PROTOCOL_VERSION_HEADER = "Mcp-Protocol-Version";

  public static final Set<String> ACCEPTED_CONTENT_TYPES = Set.of("application/json", "text/event-stream");
  public static final Set<CharSequence> ACCEPTED_HEADERS = Set.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, MCP_SESSION_ID_HEADER, MCP_PROTOCOL_VERSION_HEADER);

  private final ModelContextProtocolServer server;
  private final ServerOptions options;
  private final SessionManager sessionManager;

  public HttpServerTransport(Vertx vertx, ModelContextProtocolServer server) {
    this.server = server;
    // Get options if available
    if (server instanceof ModelContextProtocolServerImpl) {
      this.options = ((ModelContextProtocolServerImpl) server).getOptions();
    } else {
      this.options = new ServerOptions();
    }
    this.sessionManager = new SessionManagerImpl(vertx, options);
  }

  @Override
  public void handle(HttpServerRequest httpRequest) {
    if (!httpRequest.path().startsWith("/mcp")) {
      return;
    }

    if (httpRequest.method().equals(HttpMethod.OPTIONS)) {
      httpRequest.response()
        .setStatusCode(200)
        .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
        .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, DELETE, OPTIONS")
        .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, String.join(",", ACCEPTED_HEADERS))
        .end();
      return;
    }

    if (!httpRequest.method().equals(HttpMethod.POST) && !httpRequest.method().equals(HttpMethod.GET)) {
      httpRequest.response().setStatusCode(405).end("Method not allowed");
      return;
    }

    String accept = httpRequest.getHeader(HttpHeaders.ACCEPT);
    if (accept == null) {
      httpRequest.response().setStatusCode(400).end("Missing Accept header");
      return;
    }

    Set<String> acceptTypes = Arrays.stream(accept.split(",")).map(String::trim).collect(Collectors.toUnmodifiableSet());

    if (!acceptTypes.containsAll(ACCEPTED_CONTENT_TYPES)) {
      httpRequest.response().setStatusCode(400).end("Invalid Accept header - must accept application/json or text/event-stream");
      return;
    }

    ContextInternal context = ((HttpServerRequestInternal) httpRequest).context();
    String sessionId = httpRequest.getHeader(MCP_SESSION_ID_HEADER);

    HttpServerRequestImpl serverRequest = new HttpServerRequestImpl(context, httpRequest, sessionManager, options);
    HttpServerResponseImpl serverResponse = new HttpServerResponseImpl(context, httpRequest.response());

    Session session = null;

    // If there's a session ID and sessions are enabled, retrieve existing session
    if (sessionId != null && options.getSessionsEnabled()) {
      session = sessionManager.getSession(sessionId);
      if (session == null) {
        httpRequest.response().setStatusCode(400).end("Invalid session ID");
        return;
      }
    }

    serverRequest.handler(v -> context.dispatch(serverRequest, server));
    serverRequest.exceptionHandler(t -> httpRequest.response()
      .setStatusCode(400)
      .putHeader("Content-Type", "application/json")
      .end(JsonResponse.error(null, JsonError.invalidRequest()).toString())
    );

    serverRequest.init(session, serverResponse);
  }

  public SessionManager getSessionManager() {
    return sessionManager;
  }
}
