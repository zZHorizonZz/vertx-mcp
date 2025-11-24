package io.vertx.mcp.common.reference;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

/**
 * A reference to a resource or resource template definition.
 */
@DataObject
@JsonGen(publicConverter = false)
public class ResourceTemplateReference {

  private String type = "ref/resource";
  private String uri;

  public ResourceTemplateReference() {
  }

  public ResourceTemplateReference(JsonObject json) {
    ResourceTemplateReferenceConverter.fromJson(json, this);
  }

  /**
   * Gets the type of reference.
   *
   * @return "ref/resource"
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type of reference.
   *
   * @param type "ref/resource"
   * @return this instance for method chaining
   */
  public ResourceTemplateReference setType(String type) {
    this.type = type;
    return this;
  }

  /**
   * Gets the URI or URI template of the resource.
   *
   * @return URI or URI template
   */
  public String getUri() {
    return uri;
  }

  /**
   * Sets the URI or URI template of the resource.
   *
   * @param uri URI or URI template
   * @return this instance for method chaining
   */
  public ResourceTemplateReference setUri(String uri) {
    this.uri = uri;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ResourceTemplateReferenceConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
