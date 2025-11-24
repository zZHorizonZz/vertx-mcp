package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.mcp.common.resources.Resource;
import io.vertx.mcp.common.resources.TextResourceContent;

import java.util.function.Supplier;

/**
 * Represents a handler responsible for providing static resources. A static resource handler provides a resource at a fixed URI that doesn't require any parameters.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/resources#resource">Server Features - Resources - Resource</a>
 */
public interface StaticResourceHandler extends ServerFeatureHandler<Void, Future<Resource>, TextResourceContent> {

  /**
   * Creates a new instance of a {@code StaticResourceHandler} with the specified parameters.
   *
   * @param uri the fixed URI associated with the static resource
   * @param name the name of the static resource handler
   * @param title the title of the static resource handler
   * @param description the description of the static resource handler
   * @param resourceSupplier a supplier that dynamically provides the asynchronous resource content
   * @return a new {@code StaticResourceHandler} instance initialized with the provided parameters
   */
  static StaticResourceHandler create(String uri, String name, String title, String description, Supplier<Future<Resource>> resourceSupplier) {
    return new StaticResourceHandler() {
      @Override
      public String uri() {
        return uri;
      }

      @Override
      public String name() {
        return name;
      }

      @Override
      public String title() {
        return title;
      }

      @Override
      public String description() {
        return description;
      }

      @Override
      public Future<Resource> apply(Void unused) {
        return resourceSupplier.get();
      }
    };
  }

  @Override
  default TextResourceContent toFeature() {
    TextResourceContent resource = new TextResourceContent()
      .setUri(uri())
      .setName(name() != null ? name() : uri());

    if (title() != null) {
      resource.setTitle(title());
    }
    if (description() != null) {
      resource.setDescription(description());
    }

    return resource;
  }

  /**
   * Retrieves the fixed URI associated with the static resource.
   *
   * @return the fixed URI where the static resource is accessible
   */
  String uri();
}
