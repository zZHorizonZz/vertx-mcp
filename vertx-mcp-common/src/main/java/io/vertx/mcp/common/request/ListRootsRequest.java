package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class ListRootsRequest extends Request {

  private static final String METHOD = "roots/list";

  public ListRootsRequest() {
    super(METHOD, null);
  }

  public ListRootsRequest(JsonObject json) {
    this();
    ListRootsRequestConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ListRootsRequestConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
