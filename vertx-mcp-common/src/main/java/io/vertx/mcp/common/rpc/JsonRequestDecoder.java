package io.vertx.mcp.common.rpc;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public final class JsonRequestDecoder {

  private JsonRequestDecoder() {
  }

  /**
   * Creates a JSON-RPC request from a JsonObject.
   *
   * @param json the JsonObject representing the request
   * @return a new JsonRequest
   * @throws IllegalArgumentException if the JsonObject is not a valid JSON-RPC request
   */
  public static JsonRequest fromJson(JsonObject json) {
    String version = json.getString(JsonProtocol.JSONRPC_FIELD);
    if (version == null || !version.equals(JsonProtocol.JSONRPC_VERSION)) {
      throw new IllegalArgumentException("Invalid JSON-RPC version: " + version);
    }

    String method = json.getString(JsonProtocol.METHOD_FIELD);
    if (method == null) {
      throw new IllegalArgumentException("Method is required");
    }

    Object params = null;
    if (json.containsKey(JsonProtocol.PARAMS_FIELD)) {
      params = json.getValue(JsonProtocol.PARAMS_FIELD);
      if (!(params instanceof JsonObject) && !(params instanceof JsonArray)) {
        throw new IllegalArgumentException("Params must be an object or array");
      }
    }

    Integer id = null;
    if (json.containsKey(JsonProtocol.ID_FIELD)) {
      id = json.getInteger(JsonProtocol.ID_FIELD);
    }

    if (id == null) {
      return JsonNotification.createNotification(method, params);
    }

    return JsonRequest.createRequest(method, params, id);
  }
}
