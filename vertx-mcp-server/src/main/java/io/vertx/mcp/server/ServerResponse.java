package io.vertx.mcp.server;

import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import io.vertx.mcp.common.rpc.JsonBatch;
import io.vertx.mcp.common.rpc.JsonResponse;

public interface ServerResponse extends WriteStream<JsonObject> {
  void end(JsonResponse response);

  void end(JsonBatch batch);
}
