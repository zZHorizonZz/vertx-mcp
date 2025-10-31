package io.vertx.mcp.transport;

import io.vertx.core.streams.WriteStream;
import io.vertx.mcp.request.Request;

public interface Session extends WriteStream<Request> {
}
