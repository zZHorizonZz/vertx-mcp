package io.vertx.mcp.client.transport.http;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.client.MessageDecoder;

public class JsonMessageDecoder implements MessageDecoder {
  @Override
  public JsonObject decode(Buffer buffer) {
    return buffer.toJsonObject();
  }
}
