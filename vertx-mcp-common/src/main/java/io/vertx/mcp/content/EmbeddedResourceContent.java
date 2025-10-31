package io.vertx.mcp.content;

import io.vertx.core.json.JsonObject;
import io.vertx.mcp.resources.Resource;

public class EmbeddedResourceContent implements Content {

  private final Resource resource;

  public EmbeddedResourceContent(Resource resource) {
    this.resource = resource;
  }

  public Resource resource() {
    return resource;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("type", "resource");
    json.put("resource", resource.toJson());
    return json;
  }
}
