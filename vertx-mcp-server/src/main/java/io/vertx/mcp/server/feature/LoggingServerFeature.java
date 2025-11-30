package io.vertx.mcp.server.feature;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.LogLevel;
import io.vertx.mcp.common.notification.LoggingMessageNotification;
import io.vertx.mcp.common.request.SetLevelRequest;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.ServerRequest;
import io.vertx.mcp.server.impl.ServerFeatureBase;
import io.vertx.mcp.server.impl.SessionManagerImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The LoggingServerFeature class implements support for MCP logging capabilities. It allows clients to set their desired log level and servers to send log messages as
 * notifications.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/utilities/logging">Server Utilities - Logging</a>
 */
public class LoggingServerFeature extends ServerFeatureBase {

  private final Map<String, LogLevel> sessionLogLevels = new ConcurrentHashMap<>();
  private final Logger logger;

  private Vertx vertx;
  private LogLevel defaultLogLevel = LogLevel.INFO;

  public LoggingServerFeature() {
    this(null);
  }

  public LoggingServerFeature(Logger logger) {
    this.logger = logger;
  }

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
  public LoggingServerFeature setDefaultLogLevel(LogLevel level) {
    if (level != null) {
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
  public LogLevel getLogLevel(String sessionId) {
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
  public void log(LogLevel level, String logger, Object data) {
    if (vertx == null || level == null) {
      return;
    }

    // Log to custom Java Logger if configured
    if (this.logger != null && level.shouldLog(defaultLogLevel)) {
      String message = data != null ? data.toString() : "";
      if (logger != null && !logger.isEmpty()) {
        this.logger.log(level.getJavaLevel(), "[" + logger + "] " + message);
      } else {
        this.logger.log(level.getJavaLevel(), message);
      }
    }

    // Check against default level for broadcast
    if (!level.shouldLog(defaultLogLevel)) {
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
    for (Map.Entry<String, LogLevel> entry : sessionLogLevels.entrySet()) {
      String sessionId = entry.getKey();
      LogLevel sessionLevel = entry.getValue();

      if (level.shouldLog(sessionLevel)) {
        DeliveryOptions options = new DeliveryOptions().addHeader("Mcp-Session-Id", sessionId);
        vertx.eventBus().send(SessionManagerImpl.NOTIFICATION_ADDRESS, notification.toNotification().toJson(), options);
      }
    }
  }

  /**
   * Sends a log message notification to all sessions that have a log level allowing messages of the given severity. Uses the default log level for filtering when no per-session
   * levels are configured.
   *
   * @param level the severity level of the log message as a string
   * @param logger optional logger name
   * @param data the log data (message or structured data)
   */
  public void log(String level, String logger, Object data) {
    LogLevel logLevel = LogLevel.fromValue(level);
    if (logLevel != null) {
      log(logLevel, logger, data);
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
    log(LogLevel.DEBUG, logger, data);
  }

  /**
   * Convenience method for info level logging.
   */
  public void info(String logger, Object data) {
    log(LogLevel.INFO, logger, data);
  }

  /**
   * Convenience method for warning level logging.
   */
  public void warning(String logger, Object data) {
    log(LogLevel.WARNING, logger, data);
  }

  /**
   * Convenience method for error level logging.
   */
  public void error(String logger, Object data) {
    log(LogLevel.ERROR, logger, data);
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

    if (!LogLevel.isValid(level)) {
      String validLevels = Stream.of(LogLevel.values())
        .map(LogLevel::getValue)
        .collect(Collectors.joining(", "));
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Invalid log level: " + level + ". Valid levels are: " + validLevels))
      );
    }

    // Require a session for logging
    if (serverRequest.session() == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidRequest("Logging requires a session-scoped request"))
      );
    }

    // Store the log level on the session
    LogLevel logLevel = LogLevel.fromValue(level);
    serverRequest.session().setMinLogLevel(level);
    // Also track in our map for log filtering
    sessionLogLevels.put(serverRequest.session().id(), logLevel);

    // Return empty result on success
    return Future.succeededFuture(JsonResponse.success(request, new JsonObject()));
  }
}
