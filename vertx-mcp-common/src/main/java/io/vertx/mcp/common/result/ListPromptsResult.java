package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class ListPromptsResult extends Result {

  private JsonArray prompts;
  private String nextCursor;

  public ListPromptsResult() {
    super(null);
  }

  public ListPromptsResult(JsonObject json) {
    this();
    ListPromptsResultConverter.fromJson(json, this);
  }

  public JsonArray getPrompts() {
    return prompts;
  }

  public ListPromptsResult setPrompts(JsonArray prompts) {
    this.prompts = prompts;
    return this;
  }

  public String getNextCursor() {
    return nextCursor;
  }

  public ListPromptsResult setNextCursor(String nextCursor) {
    this.nextCursor = nextCursor;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ListPromptsResultConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
