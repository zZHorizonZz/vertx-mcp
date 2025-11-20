package io.vertx.mcp.server;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.mcp.server.feature.ProtocolServerFeature;
import io.vertx.mcp.server.feature.SessionServerFeature;
import io.vertx.mcp.server.impl.ModelContextProtocolServerImpl;

import java.util.List;

/**
 * The {@code ModelContextProtocolServer} interface defines a protocol server for managing Model Context Protocol operations. It extends the {@code Handler<ServerRequest>}
 * interface to handle server requests within the JSON-RPC framework. This server provides functionalities such as protocol and session handling, feature registration, and
 * interaction with server requests.
 */
@VertxGen
public interface ModelContextProtocolServer extends Handler<ServerRequest> {

  /**
   * Creates a new instance of the ModelContextProtocolServer with default server options. This method initializes a protocol server that supports the core features of the Model
   * Context Protocol.
   *
   * @return a new instance of ModelContextProtocolServer with default configuration.
   */
  static ModelContextProtocolServer create(Vertx vertx) {
    return create(vertx, new ServerOptions());
  }

  /**
   * Creates a new instance of the ModelContextProtocolServer using the specified server name and server version.
   *
   * @param serverName the name of the server
   * @param serverVersion the version of the server
   * @return a new instance of ModelContextProtocolServer configured with the specified server name and version
   */
  static ModelContextProtocolServer create(Vertx vertx, String serverName, String serverVersion) {
    return create(vertx, new ServerOptions().setServerName(serverName).setServerVersion(serverVersion));
  }

  /**
   * Creates and returns a new instance of {@code ModelContextProtocolServer} with the specified server options. Registers necessary features including protocol and session
   * handling (if sessions are enabled).
   *
   * @param options the {@code ServerOptions} object containing configuration details for the server
   * @return a new instance of {@code ModelContextProtocolServer} initialized with the provided options
   */
  static ModelContextProtocolServer create(Vertx vertx, ServerOptions options) {
    ModelContextProtocolServer server = new ModelContextProtocolServerImpl(vertx, options);

    // Register protocol feature (handles initialize, ping)
    ProtocolServerFeature protocolFeature = new ProtocolServerFeature(server, options);
    server.addServerFeature(protocolFeature);

    // Register session feature if sessions are enabled (handles subscribe, unsubscribe)
    if (options.getSessionsEnabled()) {
      SessionServerFeature sessionFeature = new SessionServerFeature(options, server);
      server.addServerFeature(sessionFeature);
    }

    return server;
  }

  /**
   * Adds a server feature to the Model Context Protocol server. Server features define specific capabilities for the server, such as handling tools, resources, or prompts.
   *
   * @param feature the server feature to add
   * @return the current instance of {@code ModelContextProtocolServer}, allowing for method chaining
   */
  @Fluent
  ModelContextProtocolServer addServerFeature(ServerFeature feature);

  /**
   * Retrieves the list of server features registered with the server. Each server feature represents a specific capability supported by the server, such as tools, resources, or
   * prompts.
   *
   * @return a list of registered server features
   */
  List<ServerFeature> features();
}
