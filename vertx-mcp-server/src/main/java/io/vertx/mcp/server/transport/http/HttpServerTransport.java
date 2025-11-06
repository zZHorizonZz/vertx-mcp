package io.vertx.mcp.server.transport.http;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.http.HttpServerRequestInternal;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerOptions;
import io.vertx.mcp.server.ServerRequest;
import io.vertx.mcp.server.ServerResponse;
import io.vertx.mcp.server.impl.ModelContextProtocolServerImpl;

public class HttpServerTransport implements Handler<HttpServerRequest> {

  private static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";

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
    this.sessionManager = new SessionManager(vertx, options);
  }

  @Override
  public void handle(HttpServerRequest httpRequest) {
    // Get the current Vert.x context
    ContextInternal context = ((HttpServerRequestInternal) httpRequest).context();

    // Check for existing session ID in header
    String sessionId = httpRequest.getHeader(MCP_SESSION_ID_HEADER);

    // Create the server request and response wrappers
    HttpServerRequestImpl serverRequest = new HttpServerRequestImpl(context, httpRequest, sessionManager, options);
    HttpServerResponseImpl serverResponse = new HttpServerResponseImpl(context, httpRequest.response());

    // If there's a session ID and sessions are enabled, retrieve existing session
    if (sessionId != null && options.getSessionsEnabled()) {
      HttpSession session = sessionManager.getSession(sessionId);
      if (session != null) {
        serverRequest.setSession(session);
        serverResponse.setSession(session);

        // Enable SSE if this is not a notification (will be determined after parsing)
        serverRequest.setSseCandidate(true);
      } else {
        // Invalid session ID - reject request
        httpRequest.response()
          .setStatusCode(400)
          .end("Invalid session ID");
        return;
      }
    }

    // Initialize the request with the response
    serverRequest.init(serverResponse);

    // Dispatch the request to the MCP server
    context.dispatch(serverRequest, server);
  }

  public SessionManager getSessionManager() {
    return sessionManager;
  }
}
