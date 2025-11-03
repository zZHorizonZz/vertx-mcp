package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class ListResourcesResult extends Result {

  private JsonArray resources;
  private String nextCursor;

  public ListResourcesResult() {
    super(null);
  }

  public ListResourcesResult(JsonObject json) {
    this();
    ListResourcesResultConverter.fromJson(json, this);
  }

  public JsonArray getResources() {
    return resources;
  }

  public ListResourcesResult setResources(JsonArray resources) {
    this.resources = resources;
    return this;
  }

  public String getNextCursor() {
    return nextCursor;
  }

  public ListResourcesResult setNextCursor(String nextCursor) {
    this.nextCursor = nextCursor;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ListResourcesResultConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
