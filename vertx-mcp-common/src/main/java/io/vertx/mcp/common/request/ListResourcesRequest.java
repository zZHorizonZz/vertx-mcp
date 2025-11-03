package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class ListResourcesRequest extends Request {

  private static final String METHOD = "resources/list";

  private String cursor;

  public ListResourcesRequest() {
    super(METHOD, null);
  }

  public ListResourcesRequest(JsonObject json) {
    this();
    ListResourcesRequestConverter.fromJson(json, this);
  }

  public String getCursor() {
    return cursor;
  }

  public ListResourcesRequest setCursor(String cursor) {
    this.cursor = cursor;
    return this;
  }

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
