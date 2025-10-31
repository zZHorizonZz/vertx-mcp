package io.vertx.mcp.server;

import java.util.function.Function;

public interface ServerFeature<T> extends Function<ServerFeatureContext, T> {
}
