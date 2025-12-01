package io.vertx.mcp.client.handler;

import io.vertx.core.Handler;
import io.vertx.mcp.client.ClientNotificationHandler;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.notification.ResourceUpdatedNotification;

/**
 * Handler for resource updated notifications from the server.
 * Called when a subscribed resource has been updated.
 */
public class ResourceUpdatedNotificationHandler implements ClientNotificationHandler {

  private final Handler<ResourceUpdatedNotification> resourceUpdatedHandler;

  public ResourceUpdatedNotificationHandler(Handler<ResourceUpdatedNotification> resourceUpdatedHandler) {
    this.resourceUpdatedHandler = resourceUpdatedHandler;
  }

  @Override
  public void handle(Notification notification) {
    if (notification instanceof ResourceUpdatedNotification) {
      resourceUpdatedHandler.handle((ResourceUpdatedNotification) notification);
    }
  }

  /**
   * Creates a default handler that prints to System.out.
   *
   * @return a default resource updated notification handler
   */
  public static ResourceUpdatedNotificationHandler defaultHandler() {
    return new ResourceUpdatedNotificationHandler(notif -> {
      String uri = notif.getUri();
      System.out.printf("Resource updated: %s%n", uri);
    });
  }
}
