package io.vertx.mcp.client.handler;

import io.vertx.core.Handler;
import io.vertx.mcp.client.ClientNotificationHandler;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.notification.PromptListChangedNotification;

/**
 * Handler for prompt list changed notifications from the server.
 * Called when the server's available prompts have changed.
 */
public class PromptListChangedNotificationHandler implements ClientNotificationHandler {

  private final Handler<PromptListChangedNotification> promptListChangedHandler;

  public PromptListChangedNotificationHandler(Handler<PromptListChangedNotification> promptListChangedHandler) {
    this.promptListChangedHandler = promptListChangedHandler;
  }

  @Override
  public void handle(Notification notification) {
    if (notification instanceof PromptListChangedNotification) {
      promptListChangedHandler.handle((PromptListChangedNotification) notification);
    }
  }

  /**
   * Creates a default handler that prints to System.out.
   *
   * @return a default prompt list changed notification handler
   */
  public static PromptListChangedNotificationHandler defaultHandler() {
    return new PromptListChangedNotificationHandler(notif -> {
      System.out.println("Server prompt list has changed - refresh prompts list");
    });
  }
}
