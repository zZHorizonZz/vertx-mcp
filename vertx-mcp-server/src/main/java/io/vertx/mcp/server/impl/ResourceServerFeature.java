package io.vertx.mcp.server.impl;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.mcp.common.resources.Resource;
import io.vertx.mcp.server.DynamicResourceHandler;
import io.vertx.mcp.server.ServerFeature;
import io.vertx.mcp.server.ServerRequest;
import io.vertx.mcp.server.StaticResourceHandler;
import io.vertx.uritemplate.UriTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ResourceServerFeature implements ServerFeature, Handler<ServerRequest> {

  private final List<StaticResourceHandler> staticHandlers = new ArrayList<>();
  private final List<DynamicResourceHandler> dynamicHandlers = new ArrayList<>();

  @Override
  public void handle(ServerRequest serverRequest) {

  }

  public void addStaticResource(String name, Supplier<Future<Resource>> resourceSupplier) {
    staticHandlers.add(StaticResourceHandler.create(name, resourceSupplier));
  }

  public void addStaticResource(StaticResourceHandler handler) {
    staticHandlers.add(handler);
  }

  public void addDynamicResource(UriTemplate template, Supplier<Future<Resource>> handler) {
    dynamicHandlers.add(DynamicResourceHandler.create(template, handler));
  }

  public void addDynamicResource(DynamicResourceHandler handler) {
    dynamicHandlers.add(handler);
  }
}
