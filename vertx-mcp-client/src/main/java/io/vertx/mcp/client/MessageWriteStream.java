package io.vertx.mcp.client;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;

@VertxGen
public interface MessageWriteStream extends WriteStream<JsonObject> {

  /**
   * @return the {@link MultiMap} to reader metadata headers
   */
  MultiMap headers();

  @Override
  MessageWriteStream exceptionHandler(@Nullable Handler<Throwable> handler);

  @Override
  MessageWriteStream setWriteQueueMaxSize(int i);

  @Override
  MessageWriteStream drainHandler(@Nullable Handler<Void> handler);

  /**
   * Write an encoded gRPC message.
   *
   * @param message the message
   * @return a future completed with the result
   */
  Future<Void> writeMessage(JsonObject message);

  /**
   * End the stream with an encoded gRPC message.
   *
   * @param message the message
   * @return a future completed with the result
   */
  Future<Void> endMessage(JsonObject message);

  /**
   * Cancel the stream.
   */
  void cancel();

  /**
   * @return whether the stream is cancelled
   */
  boolean isCancelled();

}

