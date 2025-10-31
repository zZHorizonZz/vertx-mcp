package io.vertx.mcp.content;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public abstract class BinaryContent implements Content {

  private final String type;
  private final String mimeType;
  private final Buffer data;

  public BinaryContent(String type, String mimeType, Buffer data) {
    this.type = type;
    this.mimeType = mimeType;
    this.data = data;
  }

  public String mimeType() {
    return mimeType;
  }

  public Buffer data() {
    return data;
  }

  @Override
  public JsonObject toJson() {
    return new JsonObject()
      .put("type", this.type)
      .put("data", data())
      .put("mimeType", mimeType());
  }
}
