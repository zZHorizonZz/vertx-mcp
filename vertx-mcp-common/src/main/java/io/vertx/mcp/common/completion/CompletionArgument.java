package io.vertx.mcp.common.completion;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class CompletionArgument {

  private String name;
  private String value;

  public CompletionArgument() {

  }

  public CompletionArgument(JsonObject json) {
    CompletionArgumentConverter.fromJson(json, this);
  }

  public String getName() {
    return name;
  }

  public CompletionArgument setName(String name) {
    this.name = name;
    return this;
  }

  public String getValue() {
    return value;
  }

  public CompletionArgument setValue(String value) {
    this.value = value;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    CompletionArgumentConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
