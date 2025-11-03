package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class ListResourceTemplatesResult extends Result {

  private JsonArray resourceTemplates;
  private String nextCursor;

  public ListResourceTemplatesResult() {
    super(null);
  }

  public ListResourceTemplatesResult(JsonObject json) {
    this();
    ListResourceTemplatesResultConverter.fromJson(json, this);
  }

  public JsonArray getResourceTemplates() {
    return resourceTemplates;
  }

  public ListResourceTemplatesResult setResourceTemplates(JsonArray resourceTemplates) {
    this.resourceTemplates = resourceTemplates;
    return this;
  }

  public String getNextCursor() {
    return nextCursor;
  }

  public ListResourceTemplatesResult setNextCursor(String nextCursor) {
    this.nextCursor = nextCursor;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ListResourceTemplatesResultConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
