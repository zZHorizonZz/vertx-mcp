package io.vertx.mcp.common.transport;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;

/**
 * Represents a bidirectional session that supports reading and writing JSON-RPC 2.0 messages. It extends both `ReadStream` for handling incoming `JsonResponse` messages and
 * `WriteStream` for sending outgoing `JsonRequest` messages. A `Session` instance enables asynchronous streaming of JSON-RPC requests and responses.
 */
@VertxGen
public interface Session extends ReadStream<JsonResponse>, WriteStream<JsonRequest> {
  @Override
  Session exceptionHandler(Handler<Throwable> handler);

  @Override
  Session pause();

  @Override
  Session resume();

  @Override
  Session fetch(long amount);

  @Override
  Session endHandler(Handler<Void> endHandler);

  @Override
  Session setWriteQueueMaxSize(int maxSize);

  @Override
  Session drainHandler(Handler<Void> handler);
}
