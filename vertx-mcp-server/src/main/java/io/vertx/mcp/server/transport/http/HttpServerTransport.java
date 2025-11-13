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
import io.vertx.mcp.server.impl.ModelContextProtocolServerImpl;
import io.vertx.mcp.server.impl.SessionManagerImpl;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class HttpServerTransport implements Handler<HttpServerRequest> {

  public static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";
  public static final Set<String> ACCEPTED_CONTENT_TYPES = Set.of("application/json", "text/event-stream");

  private final ModelContextProtocolServer server;
  private final ServerOptions options;
  private final SessionManagerImpl sessionManager;

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
    // Validate if request is MCP request
    if (!httpRequest.path().startsWith("/mcp")) {
      return;
    }

    // If request is OPTIONS, return 200 OK with CORS headers
    if (httpRequest.method().equals(HttpMethod.OPTIONS)) {
      httpRequest.response()
        .setStatusCode(200)
        .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
        .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS")
        .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Accept, Mcp-Session-Id, Mcp-Protocol-Version")
        .end();
      return;
    }

    // Validate if request is POST or GET
    if (!httpRequest.method().equals(HttpMethod.POST) && !httpRequest.method().equals(HttpMethod.GET)) {
      httpRequest.response().setStatusCode(405).end("Method not allowed");
      return;
    }

    // Validate if request contains required headers
    String accept = httpRequest.getHeader(HttpHeaders.ACCEPT);
    if (accept == null) {
      httpRequest.response().setStatusCode(400).end("Missing Accept header");
      return;
    }

    // Parse and trim accept header values
    Set<String> acceptTypes = Arrays.stream(accept.split(",")).map(String::trim).collect(Collectors.toUnmodifiableSet());

    if (!acceptTypes.containsAll(ACCEPTED_CONTENT_TYPES)) {
      httpRequest.response().setStatusCode(400).end("Invalid Accept header - must accept application/json or text/event-stream");
      return;
    }

    // Get the current Vert.x context
    ContextInternal context = ((HttpServerRequestInternal) httpRequest).context();

    // Check for existing session ID in header
    String sessionId = httpRequest.getHeader(MCP_SESSION_ID_HEADER);

    // Create the server request and response wrappers
    HttpServerRequestImpl serverRequest = new HttpServerRequestImpl(context, httpRequest, sessionManager, options);
    HttpServerResponseImpl serverResponse = new HttpServerResponseImpl(context, httpRequest.response());

    // If there's a session ID and sessions are enabled, retrieve existing session
    if (sessionId != null && options.getSessionsEnabled()) {
      Session session = sessionManager.getSession(sessionId);
      if (session != null) {
        serverRequest.setSession(session);
        serverResponse.setSession(session);
      } else {
        // Invalid session ID - reject request
        httpRequest.response().setStatusCode(400).end("Invalid session ID");
        return;
      }
    }

    // Set handler to dispatch to server when request is fully parsed
    serverRequest.handler(v -> context.dispatch(serverRequest, server));

    // Set exception handler to handle errors
    serverRequest.exceptionHandler(t -> {
      // Handle parsing errors by returning invalid request error
      httpRequest.response()
        .setStatusCode(400)
        .putHeader("Content-Type", "application/json")
        .end(JsonResponse.error(null, JsonError.invalidRequest()).toString());
    });

    // Initialize the request with the response (starts reading body)
    serverRequest.init(serverResponse);
  }

  public SessionManagerImpl getSessionManager() {
    return sessionManager;
  }
}
