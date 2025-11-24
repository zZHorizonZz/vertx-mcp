package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class ElicitResult extends Result {

  private String action;
  private JsonObject content;

  public ElicitResult() {
    super(null);
  }

  public ElicitResult(JsonObject json) {
    this();
    ElicitResultConverter.fromJson(json, this);
  }

  public String getAction() {
    return action;
  }

  public ElicitResult setAction(String action) {
    this.action = action;
    return this;
  }

  public JsonObject getContent() {
    return content;
  }

  public ElicitResult setContent(JsonObject content) {
    this.content = content;
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ElicitResultConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
