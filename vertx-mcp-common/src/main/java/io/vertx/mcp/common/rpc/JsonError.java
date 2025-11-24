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
package io.vertx.mcp.common.rpc;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Represents a JSON-RPC 2.0 error object.
 * <p>
 * A JSON-RPC error contains:
 * <ul>
 *   <li>code - A Number that indicates the error type that occurred.</li>
 *   <li>message - A String providing a short description of the error.</li>
 *   <li>data - A Primitive or Structured value that contains additional information about the error (optional).</li>
 * </ul>
 */
@DataObject
public class JsonError {

  // Standard error codes
  public static final int PARSE_ERROR = -32700;
  public static final int INVALID_REQUEST = -32600;
  public static final int METHOD_NOT_FOUND = -32601;
  public static final int METHOD_NOT_ALLOWED = -32000;
  public static final int INVALID_PARAMS = -32602;
  public static final int INTERNAL_ERROR = -32603;

  // Server error codes range from -32000 to -32099
  public static final int SERVER_ERROR_MIN = -32099;
  public static final int SERVER_ERROR_MAX = -32000;
  private static final String CODE_FIELD = "code";
  private static final String MESSAGE_FIELD = "message";
  private static final String DATA_FIELD = "data";
  private final int code;
  private final String message;
  private final Object data;

  /**
   * Creates a new JSON-RPC error.
   *
   * @param code the error code
   * @param message the error message
   * @param data additional error data (optional)
   */
  public JsonError(int code, String message, Object data) {
    this.code = code;
    this.message = message;
    this.data = data;
  }

  /**
   * Creates a new JSON-RPC error without additional data.
   *
   * @param code the error code
   * @param message the error message
   */
  public JsonError(int code, String message) {
    this(code, message, null);
  }

  /**
   * Creates a parse error (-32700).
   *
   * @return a new parse error
   */
  public static JsonError parseError() {
    return new JsonError(PARSE_ERROR, "Parse error");
  }

  /**
   * Creates a parse error (-32700) with additional details.
   *
   * @param details additional error details
   * @return a new parse error
   */
  public static JsonError parseError(String details) {
    return new JsonError(PARSE_ERROR, details);
  }

  /**
   * Creates an invalid request error (-32600).
   *
   * @return a new invalid request error
   */
  public static JsonError invalidRequest() {
    return new JsonError(INVALID_REQUEST, "Invalid Request");
  }

  /**
   * Creates an invalid request error (-32600) with additional details.
   *
   * @param details additional error details
   * @return a new invalid request error
   */
  public static JsonError invalidRequest(String details) {
    return new JsonError(INVALID_REQUEST, details);
  }

  /**
   * Creates a method not found error (-32601).
   *
   * @return a new method not found error
   */
  public static JsonError methodNotFound() {
    return new JsonError(METHOD_NOT_FOUND, "Method not found");
  }

  /**
   * Creates a method not allowed error (-32000).
   *
   * @return a new method not allowed error
   */
  public static JsonError methodNotAllowed() {
    return new JsonError(METHOD_NOT_ALLOWED, "Method not allowed");
  }

  /**
   * Creates a method not found error (-32601) with additional details.
   *
   * @param method the method name that was not found
   * @return a new method not found error
   */
  public static JsonError methodNotFound(String method) {
    return new JsonError(METHOD_NOT_FOUND, "Method not found: " + method, method);
  }

  /**
   * Creates an invalid params error (-32602).
   *
   * @return a new invalid params error
   */
  public static JsonError invalidParams() {
    return new JsonError(INVALID_PARAMS, "Invalid params");
  }

  /**
   * Creates an invalid params error (-32602) with additional details.
   *
   * @param details additional error details
   * @return a new invalid params error
   */
  public static JsonError invalidParams(String details) {
    return new JsonError(INVALID_PARAMS, details);
  }

  /**
   * Creates an internal error (-32603).
   *
   * @return a new internal error
   */
  public static JsonError internalError() {
    return new JsonError(INTERNAL_ERROR, "Internal error");
  }

  /**
   * Creates an internal error (-32603) with additional details.
   *
   * @param details additional error details
   * @return a new internal error
   */
  public static JsonError internalError(String details) {
    return new JsonError(INTERNAL_ERROR, details);
  }

  /**
   * Creates a server error (between -32000 and -32099).
   *
   * @param code the error code (must be between -32000 and -32099)
   * @param message the error message
   * @return a new server error
   * @throws IllegalArgumentException if the code is not in the server error range
   */
  public static JsonError serverError(int code, String message) {
    if (code < SERVER_ERROR_MIN || code > SERVER_ERROR_MAX) {
      throw new IllegalArgumentException("Server error code must be between " + SERVER_ERROR_MIN + " and " + SERVER_ERROR_MAX);
    }
    return new JsonError(code, message);
  }

  /**
   * Creates a server error (between -32000 and -32099) with additional details.
   *
   * @param code the error code (must be between -32000 and -32099)
   * @param message the error message
   * @param data additional error data
   * @return a new server error
   * @throws IllegalArgumentException if the code is not in the server error range
   */
  public static JsonError serverError(int code, String message, Object data) {
    if (code < SERVER_ERROR_MIN || code > SERVER_ERROR_MAX) {
      throw new IllegalArgumentException("Server error code must be between " + SERVER_ERROR_MIN + " and " + SERVER_ERROR_MAX);
    }
    return new JsonError(code, message, data);
  }

  /**
   * Creates a JSON-RPC error from a JsonObject.
   *
   * @param json the JsonObject representing the error
   * @return a new JsonError
   * @throws IllegalArgumentException if the JsonObject is not a valid JSON-RPC error
   */
  public static JsonError fromJson(JsonObject json) {
    Integer code = json.getInteger(CODE_FIELD);
    if (code == null) {
      throw new IllegalArgumentException("Error code is required");
    }

    String message = json.getString(MESSAGE_FIELD);
    if (message == null) {
      throw new IllegalArgumentException("Error message is required");
    }

    Object data = null;
    if (json.containsKey(DATA_FIELD)) {
      data = json.getValue(DATA_FIELD);
    }

    return new JsonError(code, message, data);
  }

  /**
   * @return the error code
   */
  public int getCode() {
    return code;
  }

  /**
   * @return the error message
   */
  public String getMessage() {
    return message;
  }

  /**
   * @return the additional error data (may be null)
   */
  public Object getData() {
    return data;
  }

  /**
   * Converts this error to a JsonObject.
   *
   * @return the JsonObject representation of this error
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject()
      .put(CODE_FIELD, code)
      .put(MESSAGE_FIELD, message);

    if (data != null) {
      json.put(DATA_FIELD, data);
    }

    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
