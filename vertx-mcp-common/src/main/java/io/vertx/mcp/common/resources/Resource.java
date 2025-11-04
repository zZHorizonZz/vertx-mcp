package io.vertx.mcp.common.resources;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;

import java.util.Map;

/**
 * Base class for resource content.
 */
public abstract class Resource implements Meta {

  private String uri;
  private String name;
  private String title;
  private String description;
  private String mimeType;
  private ResourceAnnotations annotations;
  private Map<String, Object> _meta;

  public Resource() {
    this.annotations = new ResourceAnnotations();
  }

  public Resource(String uri, String name, String title, String description, String mimeType) {
    this(uri, name, title, description, mimeType, new ResourceAnnotations());
  }

  public Resource(String uri, String name, String title, String description, String mimeType, ResourceAnnotations annotations) {
    this.uri = uri;
    this.name = name;
    this.title = title;
    this.description = description;
    this.mimeType = mimeType;
    this.annotations = annotations != null ? annotations : new ResourceAnnotations();
  }

  public String getUri() {
    return uri;
  }

  public Resource setUri(String uri) {
    this.uri = uri;
    return this;
  }

  public String getName() {
    return name;
  }

  public Resource setName(String name) {
    this.name = name;
    return this;
  }

  public String getTitle() {
    return title;
  }

  public Resource setTitle(String title) {
    this.title = title;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Resource setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getMimeType() {
    return mimeType;
  }

  public Resource setMimeType(String mimeType) {
    this.mimeType = mimeType;
    return this;
  }

  public ResourceAnnotations getAnnotations() {
    return annotations;
  }

  public Resource setAnnotations(ResourceAnnotations annotations) {
    this.annotations = annotations;
    return this;
  }

  @Override
  public Map<String, Object> getMeta() {
    return _meta;
  }

  @Override
  public void setMeta(Map<String, Object> meta) {
    this._meta = meta;
  }

  public abstract Buffer getContent();

  public long getSize() {
    Buffer content = getContent();
    if (content == null) {
      return 0;
    }
    return content.length();
  }

  public abstract JsonObject toJson();
}
