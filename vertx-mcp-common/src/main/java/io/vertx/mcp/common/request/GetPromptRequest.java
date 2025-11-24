package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;

@DataObject
@JsonGen(publicConverter = false)
public class GetPromptRequest extends Request {

  private static final String METHOD = "prompts/get";

  private String name;
  private JsonObject arguments;

  public GetPromptRequest() {
    super(METHOD, null);
  }

  public GetPromptRequest(JsonObject json) {
    super(METHOD, json.getJsonObject(Meta.META_KEY, new JsonObject()));
    GetPromptRequestConverter.fromJson(json, this);
  }

  public String getName() {
    return name;
  }

  public GetPromptRequest setName(String name) {
    this.name = name;
    return this;
  }

  public JsonObject getArguments() {
    return arguments;
  }

  public GetPromptRequest setArguments(JsonObject arguments) {
    this.arguments = arguments;
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    GetPromptRequestConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
