package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;

@DataObject
@JsonGen(publicConverter = false)
public class ListResourcesRequest extends PaginatedRequest {

  private static final String METHOD = "resources/list";

  public ListResourcesRequest() {
    super(METHOD, null);
  }

  public ListResourcesRequest(JsonObject json) {
    super(METHOD, json.getJsonObject(Meta.META_KEY, new JsonObject()));
    ListResourcesRequestConverter.fromJson(json, this);
  }

  @Override
  public ListResourcesRequest setCursor(String cursor) {
    super.setCursor(cursor);
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ListResourcesRequestConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
