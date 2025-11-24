package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;

@DataObject
@JsonGen(publicConverter = false)
public class ListRootsRequest extends Request {

  private static final String METHOD = "roots/list";

  public ListRootsRequest() {
    super(METHOD, null);
  }

  public ListRootsRequest(JsonObject json) {
    super(METHOD, json.getJsonObject(Meta.META_KEY, new JsonObject()));
    ListRootsRequestConverter.fromJson(json, this);
  }

  @Override
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
