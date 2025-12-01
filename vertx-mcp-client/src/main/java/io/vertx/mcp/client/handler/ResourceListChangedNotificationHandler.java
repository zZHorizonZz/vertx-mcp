package io.vertx.mcp.client.handler;

import io.vertx.core.Handler;
import io.vertx.mcp.client.ClientNotificationHandler;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.notification.ResourceListChangedNotification;

/**
 * Handler for resource list changed notifications from the server.
 * Called when the server's available resources have changed.
 */
public class ResourceListChangedNotificationHandler implements ClientNotificationHandler {

  private final Handler<ResourceListChangedNotification> resourceListChangedHandler;

  public ResourceListChangedNotificationHandler(Handler<ResourceListChangedNotification> resourceListChangedHandler) {
    this.resourceListChangedHandler = resourceListChangedHandler;
  }

  @Override
  public void handle(Notification notification) {
    if (notification instanceof ResourceListChangedNotification) {
      resourceListChangedHandler.handle((ResourceListChangedNotification) notification);
    }
  }

  /**
   * Creates a default handler that prints to System.out.
   *
   * @return a default resource list changed notification handler
   */
  public static ResourceListChangedNotificationHandler defaultHandler() {
    return new ResourceListChangedNotificationHandler(notif -> {
      System.out.println("Server resource list has changed - refresh resources list");
    });
  }
}
