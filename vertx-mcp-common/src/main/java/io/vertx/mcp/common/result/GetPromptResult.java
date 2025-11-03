package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class GetPromptResult extends Result {

  private String description;
  private JsonArray messages;

  public GetPromptResult() {
    super(null);
  }

  public GetPromptResult(JsonObject json) {
    this();
    GetPromptResultConverter.fromJson(json, this);
  }

  public String getDescription() {
    return description;
  }

  public GetPromptResult setDescription(String description) {
    this.description = description;
    return this;
  }

  public JsonArray getMessages() {
    return messages;
  }

  public GetPromptResult setMessages(JsonArray messages) {
    this.messages = messages;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    GetPromptResultConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
