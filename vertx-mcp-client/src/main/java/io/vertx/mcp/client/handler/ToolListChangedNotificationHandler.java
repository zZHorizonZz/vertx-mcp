package io.vertx.mcp.client.handler;

import io.vertx.core.Handler;
import io.vertx.mcp.client.ClientNotificationHandler;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.notification.ToolListChangedNotification;

/**
 * Handler for tool list changed notifications from the server.
 * Called when the server's available tools have changed.
 */
public class ToolListChangedNotificationHandler implements ClientNotificationHandler {

  private final Handler<ToolListChangedNotification> toolListChangedHandler;

  public ToolListChangedNotificationHandler(Handler<ToolListChangedNotification> toolListChangedHandler) {
    this.toolListChangedHandler = toolListChangedHandler;
  }

  @Override
  public void handle(Notification notification) {
    if (notification instanceof ToolListChangedNotification) {
      toolListChangedHandler.handle((ToolListChangedNotification) notification);
    }
  }

  /**
   * Creates a default handler that prints to System.out.
   *
   * @return a default tool list changed notification handler
   */
  public static ToolListChangedNotificationHandler defaultHandler() {
    return new ToolListChangedNotificationHandler(notif -> {
      System.out.println("Server tool list has changed - refresh tools list");
    });
  }
}
