package io.vertx.mcp.server.feature;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.notification.LoggingMessageNotification;
import io.vertx.mcp.common.request.SetLevelRequest;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.ServerRequest;
import io.vertx.mcp.server.impl.ServerFeatureBase;
import io.vertx.mcp.server.impl.SessionManagerImpl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * The LoggingServerFeature class implements support for MCP logging capabilities. It allows clients to set their desired log level and servers to send log messages as
 * notifications.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/utilities/logging">Server Utilities - Logging</a>
 */
public class LoggingServerFeature extends ServerFeatureBase {

  /**
   * Log levels ordered by severity (lowest to highest). Based on syslog severity levels from RFC-5424.
   */
  private static final List<String> LOG_LEVELS = Arrays.asList(
    "debug",
    "info",
    "notice",
    "warning",
    "error",
    "critical",
    "alert",
    "emergency"
  );
  private final Map<String, String> sessionLogLevels = new ConcurrentHashMap<>();
  private Vertx vertx;
  private String defaultLogLevel = "info";

  @Override
  public void init(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public Map<String, BiFunction<ServerRequest, JsonRequest, Future<JsonResponse>>> getHandlers() {
    return Map.of("logging/setLevel", this::handleSetLevel);
  }

  /**
   * Sets the default log level for sessions that haven't explicitly set one.
   *
   * @param level the default log level
   * @return this instance for method chaining
   */
  public LoggingServerFeature setDefaultLogLevel(String level) {
    if (isValidLogLevel(level)) {
      this.defaultLogLevel = level;
    }
    return this;
  }

  /**
   * Gets the current log level for a session.
   *
   * @param sessionId the session ID
   * @return the log level for the session, or the default if not set
   */
  public String getLogLevel(String sessionId) {
    return sessionLogLevels.getOrDefault(sessionId, defaultLogLevel);
  }

  /**
   * Sends a log message notification to all sessions that have a log level allowing messages of the given severity. Uses the default log level for filtering when no per-session
   * levels are configured.
   *
   * @param level the severity level of the log message
   * @param logger optional logger name
   * @param data the log data (message or structured data)
   */
  public void log(String level, String logger, Object data) {
    if (vertx == null || !isValidLogLevel(level)) {
      return;
    }

    // Check against default level for broadcast
    if (!shouldLog(level, defaultLogLevel)) {
      return;
    }

    LoggingMessageNotification notification = new LoggingMessageNotification()
      .setLevel(level)
      .setLogger(logger)
      .setData(data);

    // If no sessions have set specific levels, broadcast to all
    if (sessionLogLevels.isEmpty()) {
      sendNotification(vertx, notification);
      return;
    }

    // Send to sessions that have appropriate log level
    for (Map.Entry<String, String> entry : sessionLogLevels.entrySet()) {
      String sessionId = entry.getKey();
      String sessionLevel = entry.getValue();

      if (shouldLog(level, sessionLevel)) {
        DeliveryOptions options = new DeliveryOptions().addHeader("Mcp-Session-Id", sessionId);
        vertx.eventBus().send(SessionManagerImpl.NOTIFICATION_ADDRESS, notification.toNotification().toJson(), options);
      }
    }
  }

  /**
   * Sends a log message notification to all sessions (broadcast). Uses the session's configured level to filter.
   *
   * @param level the severity level
   * @param data the log data
   */
  public void log(String level, Object data) {
    log(level, null, data);
  }

  /**
   * Convenience method for debug level logging.
   */
  public void debug(String logger, Object data) {
    log("debug", logger, data);
  }

  /**
   * Convenience method for info level logging.
   */
  public void info(String logger, Object data) {
    log("info", logger, data);
  }

  /**
   * Convenience method for warning level logging.
   */
  public void warning(String logger, Object data) {
    log("warning", logger, data);
  }

  /**
   * Convenience method for error level logging.
   */
  public void error(String logger, Object data) {
    log("error", logger, data);
  }

  private Future<JsonResponse> handleSetLevel(ServerRequest serverRequest, JsonRequest request) {
    JsonObject params = request.getNamedParams();
    if (params == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing parameters"))
      );
    }

    SetLevelRequest setLevelRequest = new SetLevelRequest(params);
    String level = setLevelRequest.getLevel();

    if (level == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing 'level' parameter"))
      );
    }

    if (!isValidLogLevel(level)) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Invalid log level: " + level +
          ". Valid levels are: " + String.join(", ", LOG_LEVELS)))
      );
    }

    // Require a session for logging
    if (serverRequest.session() == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidRequest("Logging requires a session-scoped request"))
      );
    }

    // Store the log level on the session
    serverRequest.session().setMinLogLevel(level);
    // Also track in our map for log filtering
    sessionLogLevels.put(serverRequest.session().id(), level);

    // Return empty result on success
    return Future.succeededFuture(JsonResponse.success(request, new JsonObject()));
  }

  /**
   * Checks if a log level is valid.
   */
  private boolean isValidLogLevel(String level) {
    return level != null && LOG_LEVELS.contains(level.toLowerCase());
  }

  /**
   * Determines if a message at the given level should be logged based on the configured minimum level.
   *
   * @param messageLevel the level of the message
   * @param minLevel the minimum level configured
   * @return true if the message should be logged
   */
  private boolean shouldLog(String messageLevel, String minLevel) {
    int messageSeverity = LOG_LEVELS.indexOf(messageLevel.toLowerCase());
    int minSeverity = LOG_LEVELS.indexOf(minLevel.toLowerCase());

    // Higher index = higher severity, so message should be logged if its severity >= min
    return messageSeverity >= 0 && minSeverity >= 0 && messageSeverity >= minSeverity;
  }

  /**
   * Removes the log level setting for a session (e.g., when session closes).
   *
   * @param sessionId the session ID to remove
   */
  public void removeSession(String sessionId) {
    sessionLogLevels.remove(sessionId);
  }

  /**
   * Clears all session log level settings.
   */
  public void clearSessions() {
    sessionLogLevels.clear();
  }
}
