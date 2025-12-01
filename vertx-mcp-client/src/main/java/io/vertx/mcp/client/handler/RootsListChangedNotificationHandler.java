package io.vertx.mcp.client.handler;

import io.vertx.core.Handler;
import io.vertx.mcp.client.ClientNotificationHandler;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.notification.RootsListChangedNotification;

/**
 * Handler for roots list changed notifications from the server.
 * Called when the server's available roots have changed.
 */
public class RootsListChangedNotificationHandler implements ClientNotificationHandler {

  private final Handler<RootsListChangedNotification> rootsListChangedHandler;

  public RootsListChangedNotificationHandler(Handler<RootsListChangedNotification> rootsListChangedHandler) {
    this.rootsListChangedHandler = rootsListChangedHandler;
  }

  @Override
  public void handle(Notification notification) {
    if (notification instanceof RootsListChangedNotification) {
      rootsListChangedHandler.handle((RootsListChangedNotification) notification);
    }
  }

  /**
   * Creates a default handler that prints to System.out.
   *
   * @return a default roots list changed notification handler
   */
  public static RootsListChangedNotificationHandler defaultHandler() {
    return new RootsListChangedNotificationHandler(notif -> {
      System.out.println("Server roots list has changed - refresh roots list");
    });
  }
}
