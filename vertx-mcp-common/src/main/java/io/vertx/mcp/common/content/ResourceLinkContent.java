package io.vertx.mcp.common.content;

import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.resources.Resource;
import io.vertx.mcp.common.resources.ResourceAnnotations;

public class ResourceLinkContent implements Content {

  private final String uri;
  private final String name;
  private final String title;
  private final String description;
  private final String mimeType;
  private final ResourceAnnotations annotations;

  public ResourceLinkContent(Resource resource) {
    this(resource.getUri(), resource.getName(), resource.getTitle(), resource.getDescription(), resource.getMimeType(), resource.getAnnotations());
  }

  public ResourceLinkContent(String uri, String name, String title, String description, String mimeType) {
    this(uri, name, title, description, mimeType, null);
  }

  public ResourceLinkContent(String uri, String name, String title, String description, String mimeType, ResourceAnnotations annotations) {
    this.uri = uri;
    this.name = name;
    this.title = title;
    this.description = description;
    this.mimeType = mimeType;
    this.annotations = annotations;
  }

  public String uri() {
    return uri;
  }

  public String name() {
    return name;
  }

  public String title() {
    return title;
  }

  public String description() {
    return description;
  }

  public String mimeType() {
    return mimeType;
  }

  public ResourceAnnotations annotations() {
    return annotations;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("type", "resource_link");
    json.put("uri", uri);
    json.put("name", name);
    json.put("title", title);
    json.put("description", description);
    json.put("mimeType", mimeType);
    if (annotations != null && !annotations.isEmpty()) {
      json.put("annotations", annotations);
    }
    return json;
  }
}
