package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class CallToolResult extends Result {

  private JsonArray content;
  private JsonObject structuredContent;
  private Boolean isError;

  public CallToolResult() {
    super(null);
  }

  public CallToolResult(JsonObject json) {
    this();
    CallToolResultConverter.fromJson(json, this);
  }

  public JsonArray getContent() {
    return content;
  }

  public CallToolResult setContent(JsonArray content) {
    this.content = content;
    return this;
  }

  public JsonObject getStructuredContent() {
    return structuredContent;
  }

  public CallToolResult setStructuredContent(JsonObject structuredContent) {
    this.structuredContent = structuredContent;
    return this;
  }

  public Boolean getIsError() {
    return isError;
  }

  public CallToolResult setIsError(Boolean isError) {
    this.isError = isError;
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    CallToolResultConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
