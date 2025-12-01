package io.vertx.mcp.server.transport.http;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
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
import io.vertx.mcp.server.ServerSession;
import io.vertx.mcp.server.SessionManager;
import io.vertx.mcp.server.impl.ModelContextProtocolServerImpl;
import io.vertx.mcp.server.impl.SessionManagerImpl;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The StreamableHttpServerTransport class is responsible for handling HTTP server requests and integrating them with the ModelContextProtocolServer. It defines routing for
 * specific endpoints and manages request validation, session handling, and response generation.
 *
 * This class primarily manages requests to paths starting with "/mcp" and provides support for various HTTP methods, including GET, POST, OPTIONS, and DELETE. It verifies headers,
 * validates sessions, and delegates request handling to the appropriate server and session management components.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#streamable-http">Server Transports - Streamable HTTP</a>
 */
public class StreamableHttpServerTransport implements Handler<HttpServerRequest> {

  public static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";
  public static final String MCP_PROTOCOL_VERSION_HEADER = "Mcp-Protocol-Version";

  public static final Set<String> ACCEPTED_CONTENT_TYPES = Set.of("application/json", "text/event-stream");
  public static final Set<CharSequence> ACCEPTED_HEADERS = Set.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, MCP_SESSION_ID_HEADER, MCP_PROTOCOL_VERSION_HEADER);

  private final ModelContextProtocolServer server;
  private final ServerOptions options;
  private final SessionManager sessionManager;

  public StreamableHttpServerTransport(Vertx vertx, ModelContextProtocolServer server) {
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

    if (httpRequest.method().equals(HttpMethod.DELETE)) {
      handleDelete(httpRequest);
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

    if (acceptTypes.isEmpty() || !acceptTypes.contains("text/event-stream")) {
      httpRequest.response().setStatusCode(400).end("Invalid Accept header - must accept text/event-stream");
      return;
    }

    ContextInternal context = ((HttpServerRequestInternal) httpRequest).context();
    String sessionId = httpRequest.getHeader(MCP_SESSION_ID_HEADER);

    StreamableHttpServerRequest serverRequest = new StreamableHttpServerRequest(context, httpRequest, sessionManager, options);
    StreamableHttpServerResponse serverResponse = new StreamableHttpServerResponse(context, httpRequest.response());

    ServerSession session = null;

    // If there's a session ID and sessions are enabled, retrieve existing session
    if (sessionId != null && options.getSessionsEnabled()) {
      session = sessionManager.getSession(sessionId);
      if (session == null) {
        httpRequest.response().setStatusCode(404).end("Session not found");
        return;
      }
    }

    if (session != null) {
      context.put(ServerSession.MCP_SESSION_CONTEXT_KEY, session);
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

  private void handleDelete(HttpServerRequest httpRequest) {
    String sessionId = httpRequest.getHeader(MCP_SESSION_ID_HEADER);

    if (sessionId == null) {
      httpRequest.response().setStatusCode(400).end("Missing session ID");
      return;
    }

    if (!options.getSessionsEnabled()) {
      httpRequest.response().setStatusCode(400).end("Sessions are not enabled");
      return;
    }

    ServerSession session = sessionManager.getSession(sessionId);
    if (session == null) {
      httpRequest.response().setStatusCode(404).end("Session not found");
      return;
    }

    sessionManager.removeSession(sessionId);

    Promise<Void> promise = Promise.promise();

    session.close(promise);

    promise.future().onComplete(ar -> httpRequest.response()
      .setStatusCode(204)
      .end()
    );
  }
}
