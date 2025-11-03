package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class ListRootsResult extends Result {

  private JsonArray roots;

  public ListRootsResult() {
    super(null);
  }

  public ListRootsResult(JsonObject json) {
    this();
    ListRootsResultConverter.fromJson(json, this);
  }

  public JsonArray getRoots() {
    return roots;
  }

  public ListRootsResult setRoots(JsonArray roots) {
    this.roots = roots;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ListRootsResultConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
