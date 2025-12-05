package io.vertx.mcp.client;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.mcp.common.result.ListRootsResult;

import java.util.function.Supplier;

/**
 * Represents a handler responsible for providing roots information. A roots handler provides a function to supply a future result of the roots list.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/roots">Client Features - Roots</a>
 */
@VertxGen
public interface RootsHandler extends Supplier<Future<ListRootsResult>> {

}
