package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.mcp.common.resources.Resource;

import java.util.Map;
import java.util.function.Function;

/**
 * Represents a handler responsible for providing dynamic resources. A dynamic resource handler provides resources based on URI template patterns,
 * where the handler receives a map of extracted URI template variables.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/resources#resourcetemplate">Server Features - Resources - Resource Template</a>
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
