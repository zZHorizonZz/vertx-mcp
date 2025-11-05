package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.mcp.common.resources.Resource;

import java.util.function.Supplier;

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
