package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.mcp.common.resources.Resource;

import java.util.function.Supplier;

/**
 * Represents a handler responsible for providing static resources. A static resource handler provides a resource at a fixed URI that doesn't require any parameters.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/resources#resource">Server Features - Resources - Resource</a>
 */
public interface StaticResourceHandler extends Supplier<Future<Resource>> {

  static StaticResourceHandler create(String name, Supplier<Future<Resource>> resourceSupplier) {
    return new StaticResourceHandler() {
      @Override
      public String getResourceName() {
        return name;
      }

      @Override
      public Future<Resource> get() {
        return resourceSupplier.get();
      }
    };
  }

  String getResourceName();

}
