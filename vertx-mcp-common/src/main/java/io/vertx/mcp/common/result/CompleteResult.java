package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.completion.Completion;

@DataObject
@JsonGen(publicConverter = false)
public class CompleteResult extends Result {

  private Completion completion;

  public CompleteResult() {
    super(null);
  }

  public CompleteResult(JsonObject json) {
    this();
    CompleteResultConverter.fromJson(json, this);
  }

  public Completion getCompletion() {
    return completion;
  }

  public CompleteResult setCompletion(Completion completion) {
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
