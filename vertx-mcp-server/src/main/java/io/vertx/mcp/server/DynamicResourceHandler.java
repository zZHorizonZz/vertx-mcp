package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.mcp.common.resources.Resource;
import io.vertx.uritemplate.UriTemplate;

import java.util.function.Supplier;

/**
 * Server feature for individual resource provision. Implements Supplier to provide the resource. Context is obtained from Vert.x context.
 */
public interface DynamicResourceHandler extends Supplier<Future<Resource>> {

  static DynamicResourceHandler create(UriTemplate template, Supplier<Future<Resource>> resourceSupplier) {
    return new DynamicResourceHandler() {
      @Override
      public UriTemplate getResourceTemplate() {
        return template;
      }

      @Override
      public Future<Resource> get() {
        return resourceSupplier.get();
      }
    };
  }

  UriTemplate getResourceTemplate();

}
