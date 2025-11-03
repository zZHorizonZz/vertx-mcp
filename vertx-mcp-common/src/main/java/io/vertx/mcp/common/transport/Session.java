package io.vertx.mcp.common.transport;

import io.vertx.core.streams.WriteStream;
import io.vertx.mcp.common.request.Request;

public interface Session extends WriteStream<Request> {
}
