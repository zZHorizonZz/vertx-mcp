package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class ReadResourceResult extends Result {

  private JsonArray contents;

  public ReadResourceResult() {
    super(null);
  }

  public ReadResourceResult(JsonObject json) {
    this();
    ReadResourceResultConverter.fromJson(json, this);
  }

  public JsonArray getContents() {
    return contents;
  }

  public ReadResourceResult setContents(JsonArray contents) {
    this.contents = contents;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ReadResourceResultConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
