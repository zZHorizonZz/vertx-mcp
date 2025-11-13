package io.vertx.mcp.common.prompt;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;

import java.util.List;
import java.util.Map;

/**
 * A prompt or prompt template that the server offers.
 */
@DataObject
@JsonGen(publicConverter = false)
public class Prompt implements Meta {

  private String name;
  private String title;
  private String description;
  private List<PromptArgument> arguments;
  private Map<String, Object> _meta;

  public Prompt() {
  }

  public Prompt(JsonObject json) {
    PromptConverter.fromJson(json, this);
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
  public Prompt setName(String name) {
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
  public Prompt setTitle(String title) {
    this.title = title;
    return this;
  }

  /**
   * Gets an optional description of what this prompt provides.
   *
   * @return prompt description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets an optional description of what this prompt provides.
   *
   * @param description prompt description
   * @return this instance for method chaining
   */
  public Prompt setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * Gets a list of arguments to use for templating the prompt.
   *
   * @return prompt arguments
   */
  public List<PromptArgument> getArguments() {
    return arguments;
  }

  /**
   * Sets a list of arguments to use for templating the prompt.
   *
   * @param arguments prompt arguments
   * @return this instance for method chaining
   */
  public Prompt setArguments(List<PromptArgument> arguments) {
    this.arguments = arguments;
    return this;
  }

  /**
   * Gets the metadata for the prompt.
   *
   * @return metadata map
   */
  @Override
  public Map<String, Object> getMeta() {
    return _meta;
  }

  /**
   * Sets the metadata for the prompt.
   *
   * @param meta metadata map
   */
  @Override
  public void setMeta(Map<String, Object> meta) {
    this._meta = meta;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    PromptConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
