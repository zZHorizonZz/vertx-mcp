/*
 * Copyright (c) 2011-2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.mcp.server;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

/**
 * Configuration for a Model Context Protocol server.
 */
@DataObject
@JsonGen(publicConverter = false)
public class ServerOptions {

  /**
   * The default server name = {@code "vertx-mcp-server"}
   */
  public static final String DEFAULT_SERVER_NAME = "vertx-mcp-server";

  /**
   * The default server version = {@code "1.0.0"}
   */
  public static final String DEFAULT_SERVER_VERSION = "1.0.0";

  /**
   * Whether sessions are enabled by default = {@code true}
   */
  public static final boolean DEFAULT_SESSIONS_ENABLED = true;

  /**
   * Whether streaming (SSE) is enabled by default = {@code true}
   */
  public static final boolean DEFAULT_STREAMING_ENABLED = true;

  /**
   * Whether notifications are enabled by default = {@code true}
   */
  public static final boolean DEFAULT_NOTIFICATIONS_ENABLED = true;

  /**
   * Whether logging is enabled by default = {@code true}
   */
  public static final boolean DEFAULT_LOGGING_ENABLED = true;

  /**
   * The default session timeout in milliseconds = {@code 30 minutes}
   */
  public static final long DEFAULT_SESSION_TIMEOUT_MS = 30 * 60 * 1000L;

  /**
   * The default maximum number of concurrent sessions = {@code 1000}
   */
  public static final int DEFAULT_MAX_SESSIONS = 1000;

  private String serverName;
  private String serverVersion;
  private boolean sessionsEnabled;
  private boolean streamingEnabled;
  private boolean notificationsEnabled;
  private boolean loggingEnabled;
  private long sessionTimeoutMs;
  private int maxSessions;

  public ServerOptions() {
    serverName = DEFAULT_SERVER_NAME;
    serverVersion = DEFAULT_SERVER_VERSION;
    sessionsEnabled = DEFAULT_SESSIONS_ENABLED;
    streamingEnabled = DEFAULT_STREAMING_ENABLED;
    notificationsEnabled = DEFAULT_NOTIFICATIONS_ENABLED;
    loggingEnabled = DEFAULT_LOGGING_ENABLED;
    sessionTimeoutMs = DEFAULT_SESSION_TIMEOUT_MS;
    maxSessions = DEFAULT_MAX_SESSIONS;
  }

  public ServerOptions(ServerOptions other) {
    serverName = other.serverName;
    serverVersion = other.serverVersion;
    sessionsEnabled = other.sessionsEnabled;
    streamingEnabled = other.streamingEnabled;
    notificationsEnabled = other.notificationsEnabled;
    loggingEnabled = other.loggingEnabled;
    sessionTimeoutMs = other.sessionTimeoutMs;
    maxSessions = other.maxSessions;
  }

  public ServerOptions(JsonObject json) {
    this();
    ServerOptionsConverter.fromJson(json, this);
  }

  /**
   * Gets the server name used in the initialize response.
   *
   * @return the server name
   */
  public String getServerName() {
    return serverName;
  }

  /**
   * Sets the server name used in the initialize response.
   *
   * @param serverName the server name
   * @return a reference to this, so the API can be used fluently
   */
  public ServerOptions setServerName(String serverName) {
    if (serverName == null || serverName.trim().isEmpty()) {
      throw new IllegalArgumentException("Server name cannot be null or empty");
    }
    this.serverName = serverName;
    return this;
  }

  /**
   * Gets the server version used in the initialize response.
   *
   * @return the server version
   */
  public String getServerVersion() {
    return serverVersion;
  }

  /**
   * Sets the server version used in the initialize response.
   *
   * @param serverVersion the server version
   * @return a reference to this, so the API can be used fluently
   */
  public ServerOptions setServerVersion(String serverVersion) {
    if (serverVersion == null || serverVersion.trim().isEmpty()) {
      throw new IllegalArgumentException("Server version cannot be null or empty");
    }
    this.serverVersion = serverVersion;
    return this;
  }

  /**
   * Gets whether session management is enabled.
   * <p>
   * Sessions are required for resource subscriptions and streaming responses.
   *
   * @return true if sessions are enabled
   */
  public boolean getSessionsEnabled() {
    return sessionsEnabled;
  }

  /**
   * Sets whether session management is enabled.
   * <p>
   * When sessions are enabled:
   * <ul>
   *   <li>The server will create and track session IDs for clients</li>
   *   <li>Clients can subscribe to resources using {@code resources/subscribe}</li>
   *   <li>Streaming responses (SSE) are available if also enabled</li>
   * </ul>
   * <p>
   * When sessions are disabled:
   * <ul>
   *   <li>Subscribe/unsubscribe operations will fail</li>
   *   <li>Streaming cannot be enabled</li>
   *   <li>Each request is handled independently</li>
   * </ul>
   *
   * @param sessionsEnabled true to enable sessions
   * @return a reference to this, so the API can be used fluently
   */
  public ServerOptions setSessionsEnabled(boolean sessionsEnabled) {
    this.sessionsEnabled = sessionsEnabled;

    if (!sessionsEnabled) {
      setStreamingEnabled(false);
      setLoggingEnabled(false);
    }

    return this;
  }

  /**
   * Gets whether streaming (Server-Sent Events) is enabled.
   * <p>
   * Streaming allows the server to send multiple responses for a single request.
   *
   * @return true if streaming is enabled
   */
  public boolean getStreamingEnabled() {
    return streamingEnabled;
  }

  /**
   * Sets whether streaming (Server-Sent Events) is enabled.
   * <p>
   * When streaming is enabled:
   * <ul>
   *   <li>The server can send multiple responses using SSE</li>
   *   <li>Clients can receive real-time updates</li>
   *   <li>Long-running operations can stream progress</li>
   * </ul>
   * <p>
   * <strong>Note:</strong> Streaming requires sessions to be enabled. Attempting to enable streaming
   * without sessions will throw an {@link IllegalArgumentException}.
   *
   * @param streamingEnabled true to enable streaming
   * @return a reference to this, so the API can be used fluently
   * @throws IllegalArgumentException if sessions are not enabled
   */
  public ServerOptions setStreamingEnabled(boolean streamingEnabled) {
    if (streamingEnabled && !sessionsEnabled) {
      throw new IllegalArgumentException("Cannot enable streaming without sessions. Enable sessions first.");
    }
    this.streamingEnabled = streamingEnabled;
    return this;
  }

  /**
   * Gets whether client notifications are enabled.
   * <p>
   * Notifications are one-way messages from the client that don't expect a response.
   *
   * @return true if notifications are enabled
   */
  public boolean getNotificationsEnabled() {
    return notificationsEnabled;
  }

  /**
   * Sets whether client notifications are enabled.
   * <p>
   * When enabled, the server will accept and process notification messages from clients. When disabled, notification messages will be ignored.
   *
   * @param notificationsEnabled true to enable notifications
   * @return a reference to this, so the API can be used fluently
   */
  public ServerOptions setNotificationsEnabled(boolean notificationsEnabled) {
    this.notificationsEnabled = notificationsEnabled;
    return this;
  }

  /**
   * Gets whether logging is enabled.
   * <p>
   * Logging allows clients to set their desired log level and receive log messages as notifications.
   *
   * @return true if logging is enabled
   */
  public boolean getLoggingEnabled() {
    return loggingEnabled;
  }

  /**
   * Sets whether logging is enabled.
   * <p>
   * When logging is enabled:
   * <ul>
   *   <li>The server will automatically add the logging feature</li>
   *   <li>Clients can set their desired log level using {@code logging/setLevel}</li>
   *   <li>Servers can send log messages as notifications</li>
   * </ul>
   * <p>
   * <strong>Note:</strong> Logging requires sessions to be enabled. Attempting to enable logging
   * without sessions will throw an {@link IllegalArgumentException}.
   *
   * @param loggingEnabled true to enable logging
   * @return a reference to this, so the API can be used fluently
   * @throws IllegalArgumentException if sessions are not enabled
   */
  public ServerOptions setLoggingEnabled(boolean loggingEnabled) {
    if (loggingEnabled && !sessionsEnabled) {
      throw new IllegalArgumentException("Cannot enable logging without sessions. Enable sessions first.");
    }
    this.loggingEnabled = loggingEnabled;
    return this;
  }

  /**
   * Gets the session timeout in milliseconds.
   * <p>
   * After this period of inactivity, sessions will be automatically closed.
   *
   * @return the session timeout in milliseconds
   */
  public long getSessionTimeoutMs() {
    return sessionTimeoutMs;
  }

  /**
   * Sets the session timeout in milliseconds.
   * <p>
   * Sessions that remain inactive for this duration will be automatically closed to free up resources.
   *
   * @param sessionTimeoutMs the timeout in milliseconds, must be positive
   * @return a reference to this, so the API can be used fluently
   * @throws IllegalArgumentException if the timeout is not positive
   */
  public ServerOptions setSessionTimeoutMs(long sessionTimeoutMs) {
    if (sessionTimeoutMs <= 0) {
      throw new IllegalArgumentException("ServerSession timeout must be positive");
    }
    this.sessionTimeoutMs = sessionTimeoutMs;
    return this;
  }

  /**
   * Gets the maximum number of concurrent sessions allowed.
   *
   * @return the maximum number of sessions
   */
  public int getMaxSessions() {
    return maxSessions;
  }

  /**
   * Sets the maximum number of concurrent sessions allowed.
   * <p>
   * When this limit is reached, new session requests will be rejected until existing sessions are closed.
   *
   * @param maxSessions the maximum number of sessions, must be positive
   * @return a reference to this, so the API can be used fluently
   * @throws IllegalArgumentException if the value is not positive
   */
  public ServerOptions setMaxSessions(int maxSessions) {
    if (maxSessions <= 0) {
      throw new IllegalArgumentException("Max sessions must be positive");
    }
    this.maxSessions = maxSessions;
    return this;
  }

  /**
   * @return a JSON representation of options
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ServerOptionsConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
