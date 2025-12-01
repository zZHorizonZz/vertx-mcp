package io.vertx.mcp.client.handler;

import io.vertx.core.Handler;
import io.vertx.mcp.client.ClientNotificationHandler;
import io.vertx.mcp.common.notification.LoggingMessageNotification;
import io.vertx.mcp.common.notification.Notification;

/**
 * Handler for logging notifications from the server.
 * Processes server log messages at various logging levels.
 */
public class LoggingNotificationHandler implements ClientNotificationHandler {

  private final Handler<LoggingMessageNotification> loggingHandler;

  public LoggingNotificationHandler(Handler<LoggingMessageNotification> loggingHandler) {
    this.loggingHandler = loggingHandler;
  }

  @Override
  public void handle(Notification notification) {
    if (notification instanceof LoggingMessageNotification) {
      loggingHandler.handle((LoggingMessageNotification) notification);
    }
  }

  /**
   * Creates a default logging notification handler that prints to System.err.
   *
   * @return a default logging notification handler
   */
  public static LoggingNotificationHandler defaultHandler() {
    return new LoggingNotificationHandler(logMsg -> {
      String level = logMsg.getLevel() != null ? logMsg.getLevel().name() : "INFO";
      String logger = logMsg.getLogger() != null ? logMsg.getLogger() : "server";
      System.err.printf("[%s] %s: %s%n", level, logger, logMsg.getData());
    });
  }
}
