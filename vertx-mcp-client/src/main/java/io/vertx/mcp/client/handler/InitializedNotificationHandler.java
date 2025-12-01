package io.vertx.mcp.client.handler;

import io.vertx.core.Handler;
import io.vertx.mcp.client.ClientNotificationHandler;
import io.vertx.mcp.common.notification.InitializedNotification;
import io.vertx.mcp.common.notification.Notification;

/**
 * Handler for initialized notifications from the server.
 * Called after the server has completed initialization following the initialize request.
 */
public class InitializedNotificationHandler implements ClientNotificationHandler {

  private final Handler<InitializedNotification> initializedHandler;

  public InitializedNotificationHandler(Handler<InitializedNotification> initializedHandler) {
    this.initializedHandler = initializedHandler;
  }

  @Override
  public void handle(Notification notification) {
    if (notification instanceof InitializedNotification) {
      initializedHandler.handle((InitializedNotification) notification);
    }
  }

  /**
   * Creates a default handler that prints to System.out.
   *
   * @return a default initialized notification handler
   */
  public static InitializedNotificationHandler defaultHandler() {
    return new InitializedNotificationHandler(notif -> {
      System.out.println("Server has completed initialization");
    });
  }
}
