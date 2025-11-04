package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.common.capabilities.ServerCapabilities;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.request.Request;
import io.vertx.mcp.common.result.Result;
import io.vertx.mcp.common.transport.Session;

/**
 * Represents a server-side MCP session.
 * Extends the base Session interface with server-specific functionality for handling
 * client requests and sending notifications.
 */
public interface ServerSession extends Session {

  /**
   * Gets the client capabilities negotiated during initialization.
   *
   * @return client capabilities
   */
  ClientCapabilities clientCapabilities();

  /**
   * Gets the server capabilities advertised during initialization.
   *
   * @return server capabilities
   */
  ServerCapabilities serverCapabilities();

  /**
   * Sends a request to the client.
   *
   * @param request the request to send
   * @return a future that completes with the result
   */
  Future<Result> sendRequest(Request request);

  /**
   * Sends a notification to the client.
   *
   * @param notification the notification to send
   * @return a future that completes when the notification is sent
   */
  Future<Void> sendNotification(Notification notification);

  /**
   * Gets the transport used by this session.
   *
   * @return server transport
   */
  ServerTransport transport();
}
