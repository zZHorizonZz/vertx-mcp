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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Represents a JSON-RPC 2.0 request object.
 * <p>
 * A JSON-RPC request contains:
 * <ul>
 *   <li>version - A String specifying the version of the JSON-RPC protocol. MUST be exactly "2.0".</li>
 *   <li>method - A String containing the name of the method to be invoked.</li>
 *   <li>params - A Structured value that holds the parameter values (optional).</li>
 *   <li>id - An identifier established by the Client (optional for notifications).</li>
 * </ul>
 */
@DataObject
public class JsonRequest {

  private final Integer id;
  private final String method;
  private final String version;

  private final JsonArray unamedParams;
  private final JsonObject namedParams;

  public JsonRequest(JsonObject json) {
    this.version = json.getString(JsonProtocol.JSONRPC_FIELD);
    this.method = json.getString(JsonProtocol.METHOD_FIELD);

    if (json.getValue(JsonProtocol.PARAMS_FIELD) instanceof JsonObject) {
      this.namedParams = json.getJsonObject(JsonProtocol.PARAMS_FIELD);
      this.unamedParams = null;
    } else if (json.getValue(JsonProtocol.PARAMS_FIELD) instanceof JsonArray) {
      this.unamedParams = json.getJsonArray(JsonProtocol.PARAMS_FIELD);
      this.namedParams = null;
    } else {
      this.unamedParams = null;
      this.namedParams = null;
    }

    this.id = json.getInteger(JsonProtocol.ID_FIELD);
  }

  public JsonRequest(String method, JsonArray unamedParams, Integer id) {
    this.version = JsonProtocol.JSONRPC_VERSION;
    this.method = method;
    this.unamedParams = unamedParams;
    this.namedParams = null;
    this.id = id;
  }

  public JsonRequest(String method, JsonObject namedParams, Integer id) {
    this.version = JsonProtocol.JSONRPC_VERSION;
    this.method = method;
    this.unamedParams = null;
    this.namedParams = namedParams;
    this.id = id;
  }

  /**
   * Creates a new JSON-RPC request with the provided method, parameters, and identifier. If the parameters are null, an empty JsonArray will be used by default. If the parameters
   * are not of type JsonObject or JsonArray, an IllegalArgumentException will be thrown.
   *
   * @param method the name of the method to be invoked in the JSON-RPC request
   * @param params the parameters for the method, which can be a JsonObject or JsonArray
   * @param id the identifier for the JSON-RPC request, used to match responses to requests
   * @return a new instance of JsonRequest representing the JSON-RPC request
   * @throws IllegalArgumentException if the params object is not a JsonObject or JsonArray
   */
  public static JsonRequest createRequest(String method, Object params, Integer id) {
    if (params == null) {
      return new JsonRequest(method, new JsonArray(), id);
    }

    if (params instanceof JsonObject) {
      return new JsonRequest(method, (JsonObject) params, id);
    } else if (params instanceof JsonArray) {
      return new JsonRequest(method, (JsonArray) params, id);
    } else {
      throw new IllegalArgumentException("Params must be an object or array");
    }
  }

  /**
   * @return the request identifier (can be String, Number, or null for notifications)
   */
  public Integer getId() {
    return id;
  }

  /**
   * @return the JSON-RPC version
   */
  public String getVersion() {
    return version;
  }

  /**
   * @return the method name
   */
  public String getMethod() {
    return method;
  }

  /**
   * @return the parameters (can be JsonObject, JsonArray, or null)
   */
  public Object getParams() {
    if (unamedParams != null) {
      return unamedParams;
    } else {
      return namedParams;
    }
  }

  /**
   * Retrieves the unnamed parameters associated with this JSON-RPC request.
   *
   * @return a JsonArray containing the unnamed parameters of the request
   */
  public JsonArray getUnnamedParams() {
    return unamedParams;
  }

  /**
   * Retrieves the named parameters associated with this JSON-RPC request.
   *
   * @return a JsonObject containing the named parameters, or null if no named parameters are set
   */
  public JsonObject getNamedParams() {
    return namedParams;
  }

  /**
   * Converts the parameters of the JSON-RPC request to a Buffer object. If the unnamed parameters are present, their encoded value will be used to create the buffer. If the named
   * parameters are present instead, their encoded value will be used. If neither are present, an empty buffer will be returned.
   *
   * @return a Buffer object containing the encoded parameters or an empty Buffer if no parameters exist
   */
  public Buffer toBuffer() {
    if (unamedParams != null) {
      return Buffer.buffer(unamedParams.encode());
    } else if (namedParams != null) {
      return Buffer.buffer(namedParams.encode());
    } else {
      return Buffer.buffer();
    }
  }

  /**
   * Converts this request to a JsonObject.
   *
   * @return the JsonObject representation of this request
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject()
      .put(JsonProtocol.JSONRPC_FIELD, version)
      .put(JsonProtocol.METHOD_FIELD, method);

    if (unamedParams != null) {
      json.put(JsonProtocol.PARAMS_FIELD, unamedParams);
    }

    if (namedParams != null) {
      json.put(JsonProtocol.PARAMS_FIELD, namedParams);
    }

    if (id != null) {
      json.put(JsonProtocol.ID_FIELD, id);
    }

    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
