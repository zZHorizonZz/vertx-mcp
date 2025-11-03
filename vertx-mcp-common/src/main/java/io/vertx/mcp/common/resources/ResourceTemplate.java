package io.vertx.mcp.common.resources;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.Map;

/**
 * A template description for resources available on the server.
 */
@DataObject
@JsonGen(publicConverter = false)
public class ResourceTemplate {

  private String name;
  private String title;
  private String uriTemplate;
  private String description;
  private String mimeType;
  private ResourceAnnotations annotations;
  private Map<String, Object> _meta;

  public ResourceTemplate() {
  }

  public ResourceTemplate(JsonObject json) {
    ResourceTemplateConverter.fromJson(json, this);
  }

  /**
   * Gets the name of the template. Intended for programmatic or logical use.
   *
   * @return template name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the template.
   *
   * @param name template name
   * @return this instance for method chaining
   */
  public ResourceTemplate setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Gets the title of the template. Intended for UI and end-user contexts.
   *
   * @return template title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title of the template.
   *
   * @param title template title
   * @return this instance for method chaining
   */
  public ResourceTemplate setTitle(String title) {
    this.title = title;
    return this;
  }

  /**
   * Gets a URI template (according to RFC 6570) that can be used to construct resource URIs.
   *
   * @return URI template
   */
  public String getUriTemplate() {
    return uriTemplate;
  }

  /**
   * Sets a URI template (according to RFC 6570) that can be used to construct resource URIs.
   *
   * @param uriTemplate URI template
   * @return this instance for method chaining
   */
  public ResourceTemplate setUriTemplate(String uriTemplate) {
    this.uriTemplate = uriTemplate;
    return this;
  }

  /**
   * Gets a description of what this template is for.
   * This can be used by clients to improve the LLM's understanding of available resources.
   *
   * @return template description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets a description of what this template is for.
   *
   * @param description template description
   * @return this instance for method chaining
   */
  public ResourceTemplate setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * Gets the MIME type for all resources that match this template.
   * This should only be included if all resources matching this template have the same type.
   *
   * @return MIME type
   */
  public String getMimeType() {
    return mimeType;
  }

  /**
   * Sets the MIME type for all resources that match this template.
   *
   * @param mimeType MIME type
   * @return this instance for method chaining
   */
  public ResourceTemplate setMimeType(String mimeType) {
    this.mimeType = mimeType;
    return this;
  }

  /**
   * Gets optional annotations for the client.
   *
   * @return annotations
   */
  public ResourceAnnotations getAnnotations() {
    return annotations;
  }

  /**
   * Sets optional annotations for the client.
   *
   * @param annotations annotations
   * @return this instance for method chaining
   */
  public ResourceTemplate setAnnotations(ResourceAnnotations annotations) {
    this.annotations = annotations;
    return this;
  }

  /**
   * Gets the metadata for the template.
   *
   * @return metadata map
   */
  public Map<String, Object> get_meta() {
    return _meta;
  }

  /**
   * Sets the metadata for the template.
   *
   * @param _meta metadata map
   * @return this instance for method chaining
   */
  public ResourceTemplate set_meta(Map<String, Object> _meta) {
    this._meta = _meta;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ResourceTemplateConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
