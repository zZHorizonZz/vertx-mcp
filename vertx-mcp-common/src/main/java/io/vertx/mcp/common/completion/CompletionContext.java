package io.vertx.mcp.common.completion;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.Map;

@DataObject
@JsonGen(publicConverter = false)
public class CompletionContext {

  private Map<String, String> arguments;

  public CompletionContext() {

  }

  public CompletionContext(JsonObject json) {
    CompletionContextConverter.fromJson(json, this);
  }

  public Map<String, String> getArguments() {
    return arguments;
  }

  public CompletionContext setArguments(Map<String, String> arguments) {
    this.arguments = arguments;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    CompletionContextConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
