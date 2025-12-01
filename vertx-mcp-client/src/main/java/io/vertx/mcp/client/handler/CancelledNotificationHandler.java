package io.vertx.mcp.client.handler;

import io.vertx.core.Handler;
import io.vertx.mcp.client.ClientNotificationHandler;
import io.vertx.mcp.common.notification.CancelledNotification;
import io.vertx.mcp.common.notification.Notification;

/**
 * Handler for cancelled notifications from the server.
 * Called when a previously requested operation has been cancelled.
 */
public class CancelledNotificationHandler implements ClientNotificationHandler {

  private final Handler<CancelledNotification> cancelledHandler;

  public CancelledNotificationHandler(Handler<CancelledNotification> cancelledHandler) {
    this.cancelledHandler = cancelledHandler;
  }

  @Override
  public void handle(Notification notification) {
    if (notification instanceof CancelledNotification) {
      cancelledHandler.handle((CancelledNotification) notification);
    }
  }

  /**
   * Creates a default handler that prints to System.out.
   *
   * @return a default cancelled notification handler
   */
  public static CancelledNotificationHandler defaultHandler() {
    return new CancelledNotificationHandler(notif -> {
      String requestId = notif.getRequestId() != null ? notif.getRequestId().toString() : "unknown";
      String reason = notif.getReason() != null ? notif.getReason() : "No reason provided";
      System.out.printf("Request %s was cancelled: %s%n", requestId, reason);
    });
  }
}
