package io.vertx.mcp.client;

import io.vertx.core.buffer.Buffer;

/**
 * State machine that handles slicing the input to a message.
 */
public interface MessageDeframer {

  void maxMessageSize(long maxMessageSize);

  void update(Buffer chunk);

  void end();

  Object next();

}
