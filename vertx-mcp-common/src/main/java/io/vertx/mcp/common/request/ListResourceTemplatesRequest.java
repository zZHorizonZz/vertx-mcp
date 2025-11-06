package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class ListResourceTemplatesRequest extends PaginatedRequest {

  private static final String METHOD = "resources/templates/list";

  public ListResourceTemplatesRequest() {
    super(METHOD, null);
  }

  public ListResourceTemplatesRequest(JsonObject json) {
    this();
    ListResourceTemplatesRequestConverter.fromJson(json, this);
  }

  @Override
  public ListResourceTemplatesRequest setCursor(String cursor) {
    super.setCursor(cursor);
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ListResourceTemplatesRequestConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
