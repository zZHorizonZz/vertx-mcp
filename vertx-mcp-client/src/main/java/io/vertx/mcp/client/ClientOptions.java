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
package io.vertx.mcp.client;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

/**
 * Configuration for a Model Context Protocol client.
 */
@DataObject
@JsonGen(publicConverter = false)
public class ClientOptions {

  /**
   * The default client name = {@code "vertx-mcp-client"}
   */
  public static final String DEFAULT_CLIENT_NAME = "vertx-mcp-client";

  /**
   * The default client version = {@code "1.0.0"}
   */
  public static final String DEFAULT_CLIENT_VERSION = "1.0.0";

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
   * The default request timeout in milliseconds = {@code 30 seconds}
   */
  public static final long DEFAULT_REQUEST_TIMEOUT_MS = 30 * 1000L;

  /**
   * The default connect timeout in milliseconds = {@code 10 seconds}
   */
  public static final long DEFAULT_CONNECT_TIMEOUT_MS = 10 * 1000L;

  private String clientName;
  private String clientVersion;
  private boolean sessionsEnabled;
  private boolean streamingEnabled;
  private boolean notificationsEnabled;
  private boolean loggingEnabled;
  private long requestTimeoutMs;
  private long connectTimeoutMs;

  public ClientOptions() {
    clientName = DEFAULT_CLIENT_NAME;
    clientVersion = DEFAULT_CLIENT_VERSION;
    sessionsEnabled = DEFAULT_SESSIONS_ENABLED;
    streamingEnabled = DEFAULT_STREAMING_ENABLED;
    notificationsEnabled = DEFAULT_NOTIFICATIONS_ENABLED;
    loggingEnabled = DEFAULT_LOGGING_ENABLED;
    requestTimeoutMs = DEFAULT_REQUEST_TIMEOUT_MS;
    connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
  }

  public ClientOptions(ClientOptions other) {
    clientName = other.clientName;
    clientVersion = other.clientVersion;
    sessionsEnabled = other.sessionsEnabled;
    streamingEnabled = other.streamingEnabled;
    notificationsEnabled = other.notificationsEnabled;
    loggingEnabled = other.loggingEnabled;
    requestTimeoutMs = other.requestTimeoutMs;
    connectTimeoutMs = other.connectTimeoutMs;
  }

  public ClientOptions(JsonObject json) {
    this();
    ClientOptionsConverter.fromJson(json, this);
  }

  /**
   * Gets the client name used in the initialize request.
   *
   * @return the client name
   */
  public String getClientName() {
    return clientName;
  }

  /**
   * Sets the client name used in the initialize request.
   *
   * @param clientName the client name
   * @return a reference to this, so the API can be used fluently
   */
  public ClientOptions setClientName(String clientName) {
    if (clientName == null || clientName.trim().isEmpty()) {
      throw new IllegalArgumentException("Client name cannot be null or empty");
    }
    this.clientName = clientName;
    return this;
  }

  /**
   * Gets the client version used in the initialize request.
   *
   * @return the client version
   */
  public String getClientVersion() {
    return clientVersion;
  }

  /**
   * Sets the client version used in the initialize request.
   *
   * @param clientVersion the client version
   * @return a reference to this, so the API can be used fluently
   */
  public ClientOptions setClientVersion(String clientVersion) {
    if (clientVersion == null || clientVersion.trim().isEmpty()) {
      throw new IllegalArgumentException("Client version cannot be null or empty");
    }
    this.clientVersion = clientVersion;
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
   *   <li>The client will maintain a session ID with the server</li>
   *   <li>Client can subscribe to resources using {@code resources/subscribe}</li>
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
  public ClientOptions setSessionsEnabled(boolean sessionsEnabled) {
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
   * Streaming allows the client to receive multiple responses for a single request.
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
   *   <li>The client can receive multiple responses using SSE</li>
   *   <li>Client can receive real-time updates</li>
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
  public ClientOptions setStreamingEnabled(boolean streamingEnabled) {
    if (streamingEnabled && !sessionsEnabled) {
      throw new IllegalArgumentException("Cannot enable streaming without sessions. Enable sessions first.");
    }
    this.streamingEnabled = streamingEnabled;
    return this;
  }

  /**
   * Gets whether server notifications are enabled.
   * <p>
   * Notifications are one-way messages from the server that don't expect a response.
   *
   * @return true if notifications are enabled
   */
  public boolean getNotificationsEnabled() {
    return notificationsEnabled;
  }

  /**
   * Sets whether server notifications are enabled.
   * <p>
   * When enabled, the client will accept and process notification messages from the server. When disabled, notification messages will be ignored.
   *
   * @param notificationsEnabled true to enable notifications
   * @return a reference to this, so the API can be used fluently
   */
  public ClientOptions setNotificationsEnabled(boolean notificationsEnabled) {
    this.notificationsEnabled = notificationsEnabled;
    return this;
  }

  /**
   * Gets whether logging is enabled.
   * <p>
   * Logging allows the client to set its desired log level and receive log messages as notifications from the server.
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
   *   <li>The client can set its desired log level using {@code logging/setLevel}</li>
   *   <li>Client can receive log messages as notifications from the server</li>
   * </ul>
   * <p>
   * <strong>Note:</strong> Logging requires sessions to be enabled. Attempting to enable logging
   * without sessions will throw an {@link IllegalArgumentException}.
   *
   * @param loggingEnabled true to enable logging
   * @return a reference to this, so the API can be used fluently
   * @throws IllegalArgumentException if sessions are not enabled
   */
  public ClientOptions setLoggingEnabled(boolean loggingEnabled) {
    if (loggingEnabled && !sessionsEnabled) {
      throw new IllegalArgumentException("Cannot enable logging without sessions. Enable sessions first.");
    }
    this.loggingEnabled = loggingEnabled;
    return this;
  }

  /**
   * Gets the request timeout in milliseconds.
   * <p>
   * After this period without a response, requests will fail with a timeout error.
   *
   * @return the request timeout in milliseconds
   */
  public long getRequestTimeoutMs() {
    return requestTimeoutMs;
  }

  /**
   * Sets the request timeout in milliseconds.
   * <p>
   * Requests that don't receive a response within this duration will fail with a timeout error.
   *
   * @param requestTimeoutMs the timeout in milliseconds, must be positive
   * @return a reference to this, so the API can be used fluently
   * @throws IllegalArgumentException if the timeout is not positive
   */
  public ClientOptions setRequestTimeoutMs(long requestTimeoutMs) {
    if (requestTimeoutMs <= 0) {
      throw new IllegalArgumentException("Request timeout must be positive");
    }
    this.requestTimeoutMs = requestTimeoutMs;
    return this;
  }

  /**
   * Gets the connection timeout in milliseconds.
   *
   * @return the connection timeout in milliseconds
   */
  public long getConnectTimeoutMs() {
    return connectTimeoutMs;
  }

  /**
   * Sets the connection timeout in milliseconds.
   * <p>
   * When connecting to a server, if the connection is not established within this duration, the connection attempt will fail with a timeout error.
   *
   * @param connectTimeoutMs the timeout in milliseconds, must be positive
   * @return a reference to this, so the API can be used fluently
   * @throws IllegalArgumentException if the value is not positive
   */
  public ClientOptions setConnectTimeoutMs(long connectTimeoutMs) {
    if (connectTimeoutMs <= 0) {
      throw new IllegalArgumentException("Connect timeout must be positive");
    }
    this.connectTimeoutMs = connectTimeoutMs;
    return this;
  }

  /**
   * @return a JSON representation of options
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ClientOptionsConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
