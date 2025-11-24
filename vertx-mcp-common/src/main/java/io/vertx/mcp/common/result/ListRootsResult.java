package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.root.Root;

import java.util.List;

@DataObject
@JsonGen(publicConverter = false)
public class ListRootsResult extends Result {

  private List<Root> roots;

  public ListRootsResult() {
    super(null);
  }

  public ListRootsResult(JsonObject json) {
    this();
    ListRootsResultConverter.fromJson(json, this);
  }

  public List<Root> getRoots() {
    return roots;
  }

  public ListRootsResult setRoots(List<Root> roots) {
    this.roots = roots;
    return this;
  }

  @Override
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
