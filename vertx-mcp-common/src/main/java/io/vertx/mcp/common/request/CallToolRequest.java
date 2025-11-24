package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;

@DataObject
@JsonGen(publicConverter = false, inheritConverter = true)
public class CallToolRequest extends Request {

  private static final String METHOD = "tools/call";

  private String name;
  private JsonObject arguments;

  public CallToolRequest() {
    super(METHOD, null);
  }

  public CallToolRequest(JsonObject json) {
    super(METHOD, json.getJsonObject(Meta.META_KEY, new JsonObject()));
    CallToolRequestConverter.fromJson(json, this);
  }

  public String getName() {
    return name;
  }

  public CallToolRequest setName(String name) {
    this.name = name;
    return this;
  }

  public JsonObject getArguments() {
    return arguments;
  }

  public CallToolRequest setArguments(JsonObject arguments) {
    this.arguments = arguments;
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    CallToolRequestConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
