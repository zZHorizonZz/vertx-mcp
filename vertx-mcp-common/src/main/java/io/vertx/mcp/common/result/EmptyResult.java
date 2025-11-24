package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class EmptyResult extends Result {

  public EmptyResult() {
    super(null);
  }

  public EmptyResult(JsonObject json) {
    this();
    EmptyResultConverter.fromJson(json, this);
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    EmptyResultConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
