package io.vertx.mcp.server;

import io.vertx.core.Future;
import io.vertx.mcp.common.completion.Completion;
import io.vertx.mcp.common.completion.CompletionArgument;
import io.vertx.mcp.common.completion.CompletionContext;
import io.vertx.mcp.common.resources.Resource;
import io.vertx.mcp.common.resources.ResourceTemplate;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a handler responsible for providing dynamic resources. A dynamic resource handler provides resources based on URI uri patterns, where the handler receives a map of
 * extracted URI uri variables.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/resources#resourcetemplate">Server Features - Resources - Resource Template</a>
 */
public interface DynamicResourceHandler extends ServerFeatureHandler<Map<String, String>, Future<Resource>, ResourceTemplate> {

  static DynamicResourceHandler create(String uri, String name, String title, String description, Function<Map<String, String>, Future<Resource>> resourceFunction,
    BiFunction<CompletionArgument, CompletionContext, Future<Completion>> completionFunction) {
    return new DynamicResourceHandler() {
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
      public Future<Resource> apply(Map<String, String> params) {
        return resourceFunction.apply(params);
      }

      @Override
      public Future<Completion> completion(CompletionArgument argument, CompletionContext context) {
        return completionFunction.apply(argument, context);
      }
    };
  }

  @Override
  default ResourceTemplate toFeature() {
    ResourceTemplate template = new ResourceTemplate().setUriTemplate(uri()).setName(name());

    if (title() != null) {
      template.setTitle(title());
    }
    if (description() != null) {
      template.setDescription(description());
    }

    return template;
  }

  String uri();

  Future<Completion> completion(CompletionArgument argument, CompletionContext context);

}
