package io.vertx.mcp.client;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public interface MessageDecoder {

  JsonObject decode(Buffer buffer);

}
