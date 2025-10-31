package io.vertx.mcp.content;

import io.vertx.core.json.JsonObject;
import io.vertx.mcp.resources.Resource;
import io.vertx.mcp.resources.ResourceAnnotations;

import java.net.URI;

public class ResourceLinkContent implements Content {

  private final URI uri;
  private final String name;
  private final String title;
  private final String description;
  private final String mimeType;
  private final ResourceAnnotations annotations;

  public ResourceLinkContent(Resource resource) {
    this(resource.uri(), resource.name(), resource.title(), resource.description(), resource.mimeType(), resource.annotations());
  }

  public ResourceLinkContent(URI uri, String name, String title, String description, String mimeType) {
    this(uri, name, title, description, mimeType, null);
  }

  public ResourceLinkContent(URI uri, String name, String title, String description, String mimeType, ResourceAnnotations annotations) {
    this.uri = uri;
    this.name = name;
    this.title = title;
    this.description = description;
    this.mimeType = mimeType;
    this.annotations = annotations;
  }

  public URI uri() {
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
    json.put("uri", uri.toString());
    json.put("name", name);
    json.put("title", title);
    json.put("description", description);
    json.put("mimeType", mimeType);
    if (annotations != null) {
      json.put("annotations", annotations);
    }
    return json;
  }
}
