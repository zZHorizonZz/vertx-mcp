package io.vertx.mcp.common.completion;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

/**
 * A reference to either a prompt or a resource for completion requests. The type field indicates whether this is a "ref/prompt" or "ref/resource".
 */
@DataObject
@JsonGen(publicConverter = false)
public class CompletionReference {

  private String type;
  private String name;  // Used for ref/prompt
  private String uri;   // Used for ref/resource

  public CompletionReference() {
  }

  public CompletionReference(JsonObject json) {
    CompletionReferenceConverter.fromJson(json, this);
  }

  /**
   * Creates a prompt reference.
   *
   * @param name the prompt name
   * @return a new CompletionReference for a prompt
   */
  public static CompletionReference promptRef(String name) {
    return new CompletionReference()
      .setType("ref/prompt")
      .setName(name);
  }

  /**
   * Creates a resource reference.
   *
   * @param uri the resource URI
   * @return a new CompletionReference for a resource
   */
  public static CompletionReference resourceRef(String uri) {
    return new CompletionReference()
      .setType("ref/resource")
      .setUri(uri);
  }

  /**
   * Gets the reference type ("ref/prompt" or "ref/resource").
   *
   * @return the reference type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the reference type.
   *
   * @param type the reference type
   * @return this instance for method chaining
   */
  public CompletionReference setType(String type) {
    this.type = type;
    return this;
  }

  /**
   * Gets the prompt name (for ref/prompt type).
   *
   * @return the prompt name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the prompt name.
   *
   * @param name the prompt name
   * @return this instance for method chaining
   */
  public CompletionReference setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Gets the resource URI (for ref/resource type).
   *
   * @return the resource URI
   */
  public String getUri() {
    return uri;
  }

  /**
   * Sets the resource URI.
   *
   * @param uri the resource URI
   * @return this instance for method chaining
   */
  public CompletionReference setUri(String uri) {
    this.uri = uri;
    return this;
  }

  /**
   * Checks if this is a prompt reference.
   *
   * @return true if this is a ref/prompt type
   */
  public boolean isPromptRef() {
    return "ref/prompt".equals(type);
  }

  /**
   * Checks if this is a resource reference.
   *
   * @return true if this is a ref/resource type
   */
  public boolean isResourceRef() {
    return "ref/resource".equals(type);
  }

  /**
   * Gets the identifier for this reference (name for prompts, uri for resources).
   *
   * @return the reference identifier
   */
  public String getIdentifier() {
    return isPromptRef() ? name : uri;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    CompletionReferenceConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
