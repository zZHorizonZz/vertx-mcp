package io.vertx.mcp.common.content;

import io.vertx.core.json.JsonObject;

public class TextContent implements Content {

  private final String text;

  protected TextContent(String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("type", "text");
    json.put("text", text);
    return json;
  }
}
