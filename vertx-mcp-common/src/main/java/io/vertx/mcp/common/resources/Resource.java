package io.vertx.mcp.common.resources;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.net.URI;

public abstract class Resource {

  private final URI uri;
  private final String name;
  private final String title;
  private final String description;
  private final String mimeType;
  private final ResourceAnnotations annotations;

  public Resource(URI uri, String name, String title, String description, String mimeType) {
    this(uri, name, title, description, mimeType, new ResourceAnnotations());
  }

  public Resource(URI uri, String name, String title, String description, String mimeType, ResourceAnnotations annotations) {
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

  public abstract Buffer content();

  public long size() {
    Buffer content = content();
    if (content == null) {
      return 0;
    }
    return content().length();
  }

  public ResourceAnnotations annotations() {
    return annotations;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();

    json.put("uri", this.uri.toString());
    json.put("name", this.name);
    json.put("title", this.title);
    json.put("description", this.description);
    json.put("mimeType", this.mimeType);

    if (content() != null) {
      json.put("size", size());
    }

    if (annotations != null && !annotations.isEmpty()) {
      json.put("annotations", this.annotations);
    }
    return json;
  }
}
