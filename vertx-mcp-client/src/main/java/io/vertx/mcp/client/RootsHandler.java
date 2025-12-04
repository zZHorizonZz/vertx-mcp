package io.vertx.mcp.client;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.mcp.common.result.ListRootsResult;

import java.util.function.Supplier;

/**
 * Represents a handler responsible for providing roots information.
 * A roots handler provides a function to supply a future result of the roots list.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/roots">Client Features - Roots</a>
 */
@VertxGen
public interface RootsHandler extends ClientFeatureHandler<Void, Future<ListRootsResult>> {

  /**
   * Creates a new instance of a {@code RootsHandler} with the specified supplier function.
   *
   * @param name the name of the roots handler
   * @param supplier the supplier function that provides the list of roots
   * @return a new {@code RootsHandler} instance initialized with the provided parameters
   */
  @GenIgnore
  static RootsHandler create(String name, Supplier<Future<ListRootsResult>> supplier) {
    return new RootsHandler() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public Future<ListRootsResult> apply(Void unused) {
        return supplier.get();
      }
    };
  }
}
