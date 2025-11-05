package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.mcp.common.resources.Resource;
import io.vertx.mcp.common.resources.ResourceTemplate;

import java.util.List;

/**
 * Handler for resource operations like listing resources and templates.
 */
public interface ResourceServerFeature {

  /**
   * Lists all available resources.
   *
   * @param cursor pagination cursor
   * @return a future that completes with the list of resources
   */
  Future<List<Resource>> listResources(String cursor);

  /**
   * Lists all available resource templates.
   *
   * @param cursor pagination cursor
   * @return a future that completes with the list of resource templates
   */
  Future<List<ResourceTemplate>> listResourceTemplates(String cursor);

  /**
   * Checks if resource subscriptions are supported.
   *
   * @return true if subscriptions are supported
   */
  default boolean supportsSubscribe() {
    return false;
  }
}
