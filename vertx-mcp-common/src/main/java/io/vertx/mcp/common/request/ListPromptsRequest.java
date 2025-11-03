package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class ListPromptsRequest extends Request {

  private static final String METHOD = "prompts/list";

  private String cursor;

  public ListPromptsRequest() {
    super(METHOD, null);
  }

  public ListPromptsRequest(JsonObject json) {
    this();
    ListPromptsRequestConverter.fromJson(json, this);
  }

  public String getCursor() {
    return cursor;
  }

  public ListPromptsRequest setCursor(String cursor) {
    this.cursor = cursor;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ListPromptsRequestConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
