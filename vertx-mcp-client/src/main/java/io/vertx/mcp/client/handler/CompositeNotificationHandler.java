package io.vertx.mcp.client.handler;

import io.vertx.mcp.client.ClientNotificationHandler;
import io.vertx.mcp.common.notification.Notification;

import java.util.ArrayList;
import java.util.List;

/**
 * A composite notification handler that dispatches notifications to multiple registered handlers.
 * Allows registering multiple notification handlers and dispatches each notification to all of them.
 */
public class CompositeNotificationHandler implements ClientNotificationHandler {

  private final List<ClientNotificationHandler> handlers = new ArrayList<>();

  /**
   * Adds a notification handler to this composite handler.
   *
   * @param handler the handler to add
   * @return this composite handler for method chaining
   */
  public CompositeNotificationHandler addHandler(ClientNotificationHandler handler) {
    this.handlers.add(handler);
    return this;
  }

  /**
   * Removes a notification handler from this composite handler.
   *
   * @param handler the handler to remove
   * @return this composite handler for method chaining
   */
  public CompositeNotificationHandler removeHandler(ClientNotificationHandler handler) {
    this.handlers.remove(handler);
    return this;
  }

  @Override
  public void handle(Notification notification) {
    // Dispatch to all registered handlers
    for (ClientNotificationHandler handler : handlers) {
      try {
        handler.handle(notification);
      } catch (Exception e) {
        // Log error but continue processing other handlers
        System.err.println("Error in notification handler: " + e.getMessage());
      }
    }
  }

  /**
   * Creates a composite handler with all default notification handlers registered.
   *
   * @return a composite handler with default handlers
   */
  public static CompositeNotificationHandler withDefaults() {
    return new CompositeNotificationHandler()
      .addHandler(LoggingNotificationHandler.defaultHandler())
      .addHandler(ProgressNotificationHandler.defaultHandler())
      .addHandler(ToolListChangedNotificationHandler.defaultHandler())
      .addHandler(ResourceListChangedNotificationHandler.defaultHandler())
      .addHandler(ResourceUpdatedNotificationHandler.defaultHandler())
      .addHandler(PromptListChangedNotificationHandler.defaultHandler())
      .addHandler(RootsListChangedNotificationHandler.defaultHandler())
      .addHandler(CancelledNotificationHandler.defaultHandler())
      .addHandler(InitializedNotificationHandler.defaultHandler());
  }
}
