package io.vertx.mcp.client;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;

@VertxGen
public interface MessageReadStream extends ReadStream<JsonObject> {

  /**
   * @return the {@link MultiMap} to read metadata headers
   */
  MultiMap headers();

  /**
   * Set a handler to be notified with incoming encoded messages. The {@code handler} is responsible for fully decoding incoming messages, including compression.
   *
   * @param handler the message handler
   * @return a reference to this, so the API can be used fluently
   */
  @Fluent
  MessageReadStream messageHandler(@Nullable Handler<JsonObject> handler);

  @Override
  MessageReadStream exceptionHandler(@Nullable Handler<Throwable> handler);

  @Override
  MessageReadStream handler(@Nullable Handler<JsonObject> handler);

  @Override
  MessageReadStream pause();

  @Override
  MessageReadStream resume();

  @Override
  MessageReadStream fetch(long l);

  @Override
  MessageReadStream endHandler(@Nullable Handler<Void> handler);

  /**
   * @return the last element of the stream
   */
  Future<JsonObject> last();

  /**
   * @return a future signaling when the response has been fully received successfully or failed
   */
  Future<Void> end();

}

