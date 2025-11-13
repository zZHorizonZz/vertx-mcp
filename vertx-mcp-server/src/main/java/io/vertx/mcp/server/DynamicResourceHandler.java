package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.mcp.common.resources.Resource;

import java.util.Map;
import java.util.function.Function;

/**
 * Server feature for individual dynamic resource provision.
 * The handler receives a map of extracted URI template variables.
 */
public interface DynamicResourceHandler extends Function<Map<String, String>, Future<Resource>> {

  static DynamicResourceHandler create(String template, Function<Map<String, String>, Future<Resource>> resourceFunction) {
    return new DynamicResourceHandler() {
      @Override
      public String getResourceTemplate() {
        return template;
      }

      @Override
      public Future<Resource> apply(Map<String, String> params) {
        return resourceFunction.apply(params);
      }
    };
  }

  String getResourceTemplate();

}
