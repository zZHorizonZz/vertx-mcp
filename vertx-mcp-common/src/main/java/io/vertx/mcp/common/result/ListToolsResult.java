package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class ListToolsResult extends Result {

  private JsonArray tools;
  private String nextCursor;

  public ListToolsResult() {
    super(null);
  }

  public ListToolsResult(JsonObject json) {
    this();
    ListToolsResultConverter.fromJson(json, this);
  }

  public JsonArray getTools() {
    return tools;
  }

  public ListToolsResult setTools(JsonArray tools) {
    this.tools = tools;
    return this;
  }

  public String getNextCursor() {
    return nextCursor;
  }

  public ListToolsResult setNextCursor(String nextCursor) {
    this.nextCursor = nextCursor;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ListToolsResultConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
