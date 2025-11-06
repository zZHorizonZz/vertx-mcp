package io.vertx.mcp.server;

import io.vertx.core.Handler;
import io.vertx.mcp.server.impl.ModelContextProtocolServerImpl;
import io.vertx.mcp.server.impl.ProtocolServerFeature;
import io.vertx.mcp.server.impl.SessionServerFeature;

import java.util.List;

/**
 * Main interface for the Model Context Protocol server. Supports registering individual items (tools, resources, prompts, roots) and handlers.
 */
public interface ModelContextProtocolServer extends Handler<ServerRequest> {

  /**
   * Creates a new MCP server with default options and protocol features pre-registered.
   *
   * @return a new server instance with protocol and session features registered
   */
  static ModelContextProtocolServer create() {
    return create(new ServerOptions());
  }

  /**
   * Creates a new MCP server with default protocol features pre-registered.
   *
   * @param serverName the server name
   * @param serverVersion the server version
   * @return a new server instance with protocol and session features registered
   */
  static ModelContextProtocolServer create(String serverName, String serverVersion) {
    ServerOptions options = new ServerOptions()
      .setServerName(serverName)
      .setServerVersion(serverVersion);
    return create(options);
  }

  /**
   * Creates a new MCP server with the specified options and protocol features pre-registered.
   *
   * @param options the server options
   * @return a new server instance with protocol and session features registered
   */
  static ModelContextProtocolServer create(ServerOptions options) {
    ModelContextProtocolServer server = new ModelContextProtocolServerImpl(options);

    // Register protocol feature (handles initialize, ping)
    ProtocolServerFeature protocolFeature = new ProtocolServerFeature(server, options);
    server.serverFeatures(protocolFeature);

    // Register session feature if sessions are enabled (handles subscribe, unsubscribe)
    if (options.getSessionsEnabled()) {
      SessionServerFeature sessionFeature = new SessionServerFeature(options);
      server.serverFeatures(sessionFeature);
    }

    return server;
  }

  ModelContextProtocolServer serverFeatures(ServerFeature feature);

  List<ServerFeature> features();
}
