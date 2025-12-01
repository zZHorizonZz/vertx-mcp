package io.vertx.mcp.server;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.mcp.common.LoggingLevel;

/**
 * Interface for managing and emitting log messages with different severity levels. Provides methods for configuring the default log level, emitting logs at various levels, and
 * convenience methods for specific logging operations.
 */
@VertxGen
public interface Logging {

  /**
   * Sets the default logging level for the application. This determines the minimum severity of log messages that will be handled.
   *
   * @param level the logging level to set as the default. It must be one of the predefined levels in {@link LoggingLevel}, such as DEBUG, INFO, WARNING, or ERROR.
   */
  void setLoggingLevel(LoggingLevel level);

  /**
   * Retrieves the current log level configuration.
   *
   * @return the current log level
   */
  LoggingLevel getLoggingLevel();

  /**
   * Logs a message with a specific logging level, logger identifier, and additional data.
   *
   * @param level the logging level indicating the severity of the log message
   * @param logger the unique identifier of the logger instance, can be null
   * @param data the additional data or message to log, represented as an object
   */
  void log(LoggingLevel level, String logger, Object data);

  /**
   * Logs a message at the specified log level using the provided logger name and data. Converts the string representation of the log level to the corresponding
   * {@link LoggingLevel}. If the level is invalid, no logging is performed.
   *
   * @param level the log level as a string (case-insensitive, e.g., "debug", "info")
   * @param logger the name of the logger (can be null for default logger)
   * @param data the data or message to log (e.g., String, JSON, etc.)
   */
  default void log(String level, String logger, Object data) {
    LoggingLevel loggingLevel = LoggingLevel.fromValue(level);
    if (loggingLevel != null) {
      log(loggingLevel, logger, data);
    }
  }

  /**
   * Logs a message with a specified log level and optional logger.
   *
   * @param level the log level represented as a string
   * @param data the data or message to be logged
   */
  default void log(String level, Object data) {
    log(level, null, data);
  }

  /**
   * Convenience method for debug level logging.
   */
  default void debug(String logger, Object data) {
    log(LoggingLevel.DEBUG, logger, data);
  }

  /**
   * Convenience method for info level logging.
   */
  default void info(String logger, Object data) {
    log(LoggingLevel.INFO, logger, data);
  }

  /**
   * Convenience method for warning level logging.
   */
  default void warning(String logger, Object data) {
    log(LoggingLevel.WARNING, logger, data);
  }

  /**
   * Convenience method for error level logging.
   */
  default void error(String logger, Object data) {
    log(LoggingLevel.ERROR, logger, data);
  }
}
