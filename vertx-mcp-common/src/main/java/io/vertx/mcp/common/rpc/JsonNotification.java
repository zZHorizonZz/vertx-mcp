package io.vertx.mcp.common.rpc;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Represents a JSON-RPC 2.0 notification. A notification is a special type of request that does not require a response from the server and thus does not include an identifier.
 */
@DataObject
public class JsonNotification extends JsonRequest {

  public JsonNotification(JsonObject json) {
    super(json);
  }

  public JsonNotification(String method, JsonArray unamedParams) {
    super(method, unamedParams, null);
  }

  public JsonNotification(String method, JsonObject namedParams) {
    super(method, namedParams, null);
  }

  /**
   * Creates a new JSON-RPC notification (request without an id).
   *
   * @param method the method name
   * @param params the parameters (can be JsonObject for named parameters or JsonArray for positional parameters)
   * @return a new JSON-RPC notification
   */
  public static JsonNotification createNotification(String method, Object params) {
    if (params == null) {
      return new JsonNotification(method, new JsonArray());
    }

    if (params instanceof JsonObject) {
      return new JsonNotification(method, (JsonObject) params);
    } else if (params instanceof JsonArray) {
      return new JsonNotification(method, (JsonArray) params);
    } else {
      throw new IllegalArgumentException("Params must be an object or array");
    }
  }
}
