package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.mcp.common.resources.Resource;

import java.util.function.Supplier;

/**
 * Server feature for individual resource provision.
 * Implements Supplier to provide the resource.
 * Context is obtained from Vert.x context.
 */
public interface ResourceServerFeature extends ServerFeature, Supplier<Future<Resource>> {
}
