package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;

@DataObject
@JsonGen(publicConverter = false)
public class ListToolsRequest extends PaginatedRequest {

  public static final String METHOD = "tools/list";

  public ListToolsRequest() {
    super(METHOD, null);
  }

  public ListToolsRequest(JsonObject json) {
    super(METHOD, json.getJsonObject(Meta.META_KEY, new JsonObject()));
    ListToolsRequestConverter.fromJson(json, this);
  }

  @Override
  public ListToolsRequest setCursor(String cursor) {
    super.setCursor(cursor);
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ListToolsRequestConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
