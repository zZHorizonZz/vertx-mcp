package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.prompt.PromptMessage;

import java.util.List;

@DataObject
@JsonGen(publicConverter = false)
public class GetPromptResult extends Result {

  private String description;
  private List<PromptMessage> messages;

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

  public List<PromptMessage> getMessages() {
    return messages;
  }

  public GetPromptResult setMessages(List<PromptMessage> messages) {
    this.messages = messages;
    return this;
  }

  @Override
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
