package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.mcp.common.resources.Resource;

import java.util.function.Supplier;

/**
 * Server feature for individual resource provision. Implements Supplier to provide the resource. Context is obtained from Vert.x context.
 */
public interface DynamicResourceHandler extends Supplier<Future<Resource>> {

  static DynamicResourceHandler create(String template, Supplier<Future<Resource>> resourceSupplier) {
    return new DynamicResourceHandler() {
      @Override
      public String getResourceTemplate() {
        return template;
      }

      @Override
      public Future<Resource> get() {
        return resourceSupplier.get();
      }
    };
  }

  String getResourceTemplate();

}
