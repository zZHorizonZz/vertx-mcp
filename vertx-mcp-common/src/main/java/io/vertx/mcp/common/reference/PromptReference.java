package io.vertx.mcp.common.reference;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

/**
 * Identifies a prompt.
 */
@DataObject
@JsonGen(publicConverter = false)
public class PromptReference {

  private String type = "ref/prompt";
  private String name;
  private String title;

  public PromptReference() {
  }

  public PromptReference(JsonObject json) {
    PromptReferenceConverter.fromJson(json, this);
  }

  /**
   * Gets the type of reference.
   *
   * @return "ref/prompt"
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type of reference.
   *
   * @param type "ref/prompt"
   * @return this instance for method chaining
   */
  public PromptReference setType(String type) {
    this.type = type;
    return this;
  }

  /**
   * Gets the name of the prompt. Intended for programmatic or logical use.
   *
   * @return prompt name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the prompt.
   *
   * @param name prompt name
   * @return this instance for method chaining
   */
  public PromptReference setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Gets the title of the prompt. Intended for UI and end-user contexts.
   *
   * @return prompt title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title of the prompt.
   *
   * @param title prompt title
   * @return this instance for method chaining
   */
  public PromptReference setTitle(String title) {
    this.title = title;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    PromptReferenceConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
