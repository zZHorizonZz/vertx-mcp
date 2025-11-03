package io.vertx.mcp.common.prompt;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

/**
 * Describes an argument that a prompt can accept.
 */
@DataObject
@JsonGen(publicConverter = false)
public class PromptArgument {

  private String name;
  private String title;
  private String description;
  private Boolean required;

  public PromptArgument() {
  }

  public PromptArgument(JsonObject json) {
    PromptArgumentConverter.fromJson(json, this);
  }

  /**
   * Gets the name of the argument. Intended for programmatic or logical use.
   *
   * @return argument name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the argument.
   *
   * @param name argument name
   * @return this instance for method chaining
   */
  public PromptArgument setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Gets the title of the argument. Intended for UI and end-user contexts.
   *
   * @return argument title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title of the argument.
   *
   * @param title argument title
   * @return this instance for method chaining
   */
  public PromptArgument setTitle(String title) {
    this.title = title;
    return this;
  }

  /**
   * Gets a human-readable description of the argument.
   *
   * @return argument description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets a human-readable description of the argument.
   *
   * @param description argument description
   * @return this instance for method chaining
   */
  public PromptArgument setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * Gets whether this argument must be provided.
   *
   * @return true if required
   */
  public Boolean getRequired() {
    return required;
  }

  /**
   * Sets whether this argument must be provided.
   *
   * @param required true if required
   * @return this instance for method chaining
   */
  public PromptArgument setRequired(Boolean required) {
    this.required = required;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    PromptArgumentConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
