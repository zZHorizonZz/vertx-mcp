package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class CompleteResult extends Result {

  private JsonObject completion;

  public CompleteResult() {
    super(null);
  }

  public CompleteResult(JsonObject json) {
    this();
    CompleteResultConverter.fromJson(json, this);
  }

  public JsonObject getCompletion() {
    return completion;
  }

  public CompleteResult setCompletion(JsonObject completion) {
    this.completion = completion;
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    CompleteResultConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
