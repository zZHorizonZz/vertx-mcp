package io.vertx.mcp.client.handler;

import io.vertx.core.Handler;
import io.vertx.mcp.client.ClientNotificationHandler;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.notification.ProgressNotification;

/**
 * Handler for progress notifications from the server.
 * Called when the server reports progress on a long-running operation.
 */
public class ProgressNotificationHandler implements ClientNotificationHandler {

  private final Handler<ProgressNotification> progressHandler;

  public ProgressNotificationHandler(Handler<ProgressNotification> progressHandler) {
    this.progressHandler = progressHandler;
  }

  @Override
  public void handle(Notification notification) {
    if (notification instanceof ProgressNotification) {
      progressHandler.handle((ProgressNotification) notification);
    }
  }

  /**
   * Creates a default handler that prints progress to System.out.
   *
   * @return a default progress notification handler
   */
  public static ProgressNotificationHandler defaultHandler() {
    return new ProgressNotificationHandler(progress -> {
      String progressToken = progress.getProgressToken() != null ?
        progress.getProgressToken().toString() : "unknown";
      Double progressValue = progress.getProgress();
      Double total = progress.getTotal();

      if (progressValue != null && total != null) {
        double percentage = (progressValue / total) * 100.0;
        System.out.printf("Progress [%s]: %.1f%% (%s/%s)%n",
          progressToken, percentage, progressValue, total);
      } else if (progressValue != null) {
        System.out.printf("Progress [%s]: %s%n", progressToken, progressValue);
      } else {
        System.out.printf("Progress [%s]: in progress%n", progressToken);
      }
    });
  }
}
