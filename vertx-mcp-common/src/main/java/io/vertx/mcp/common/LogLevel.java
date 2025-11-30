package io.vertx.mcp.common;

import java.util.logging.Level;

/**
 * Log levels ordered by severity (lowest to highest).
 * Based on syslog severity levels from RFC-5424.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/utilities/logging">Server Utilities - Logging</a>
 */
public enum LogLevel {
  DEBUG("debug", Level.FINE),
  INFO("info", Level.INFO),
  NOTICE("notice", Level.INFO),
  WARNING("warning", Level.WARNING),
  ERROR("error", Level.SEVERE),
  CRITICAL("critical", Level.SEVERE),
  ALERT("alert", Level.SEVERE),
  EMERGENCY("emergency", Level.SEVERE);

  private final String value;
  private final Level javaLevel;

  LogLevel(String value, Level javaLevel) {
    this.value = value;
    this.javaLevel = javaLevel;
  }

  /**
   * Gets the string value used in the MCP protocol.
   *
   * @return the string value
   */
  public String getValue() {
    return value;
  }

  /**
   * Gets the corresponding Java logging Level.
   *
   * @return the Java logging Level
   */
  public Level getJavaLevel() {
    return javaLevel;
  }

  /**
   * Gets the severity index (0 = lowest, 7 = highest).
   *
   * @return the severity index
   */
  public int getSeverity() {
    return ordinal();
  }

  /**
   * Checks if this log level should be logged based on a minimum level.
   *
   * @param minLevel the minimum level threshold
   * @return true if this level meets or exceeds the minimum level
   */
  public boolean shouldLog(LogLevel minLevel) {
    return this.ordinal() >= minLevel.ordinal();
  }

  /**
   * Parses a string value into a LogLevel.
   *
   * @param value the string value (case-insensitive)
   * @return the LogLevel, or null if invalid
   */
  public static LogLevel fromValue(String value) {
    if (value == null) {
      return null;
    }
    String lowerValue = value.toLowerCase();
    for (LogLevel level : values()) {
      if (level.value.equals(lowerValue)) {
        return level;
      }
    }
    return null;
  }

  /**
   * Checks if a string is a valid log level.
   *
   * @param value the string value to check
   * @return true if valid
   */
  public static boolean isValid(String value) {
    return fromValue(value) != null;
  }

  @Override
  public String toString() {
    return value;
  }
}
