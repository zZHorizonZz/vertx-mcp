package io.vertx.mcp.common.tool;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;

/**
 * Definition for a tool the client can call.
 */
@DataObject
@JsonGen(publicConverter = false)
public class Tool implements Meta {

  private String name;
  private String title;
  private String description;
  private JsonObject inputSchema;
  private JsonObject outputSchema;
  private ToolAnnotations annotations;
  private JsonObject _meta;

  public Tool() {
  }

  public Tool(JsonObject json) {
    ToolConverter.fromJson(json, this);
  }

  /**
   * Gets the name of the tool. Intended for programmatic or logical use.
   *
   * @return tool name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the tool.
   *
   * @param name tool name
   * @return this instance for method chaining
   */
  public Tool setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Gets the title of the tool. Intended for UI and end-user contexts.
   *
   * @return tool title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title of the tool.
   *
   * @param title tool title
   * @return this instance for method chaining
   */
  public Tool setTitle(String title) {
    this.title = title;
    return this;
  }

  /**
   * Gets a human-readable description of the tool. This can be used by clients to improve the LLM's understanding of available tools.
   *
   * @return tool description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets a human-readable description of the tool.
   *
   * @param description tool description
   * @return this instance for method chaining
   */
  public Tool setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * Gets a JSON Schema object defining the expected parameters for the tool.
   *
   * @return input schema
   */
  public JsonObject getInputSchema() {
    return inputSchema;
  }

  /**
   * Sets a JSON Schema object defining the expected parameters for the tool.
   *
   * @param inputSchema input schema
   * @return this instance for method chaining
   */
  public Tool setInputSchema(JsonObject inputSchema) {
    this.inputSchema = inputSchema;
    return this;
  }

  /**
   * Gets an optional JSON Schema object defining the structure of the tool's output returned in the structuredContent field of a CallToolResult.
   *
   * @return output schema
   */
  public JsonObject getOutputSchema() {
    return outputSchema;
  }

  /**
   * Sets an optional JSON Schema object defining the structure of the tool's output.
   *
   * @param outputSchema output schema
   * @return this instance for method chaining
   */
  public Tool setOutputSchema(JsonObject outputSchema) {
    this.outputSchema = outputSchema;
    return this;
  }

  /**
   * Gets optional additional tool information. Display name precedence order is: title, annotations.title, then name.
   *
   * @return tool annotations
   */
  public ToolAnnotations getAnnotations() {
    return annotations;
  }

  /**
   * Sets optional additional tool information.
   *
   * @param annotations tool annotations
   * @return this instance for method chaining
   */
  public Tool setAnnotations(ToolAnnotations annotations) {
    this.annotations = annotations;
    return this;
  }

  /**
   * Gets the metadata for the tool.
   *
   * @return metadata map
   */
  @Override
  @GenIgnore
  public JsonObject getMeta() {
    return _meta;
  }

  /**
   * Sets the metadata for the tool.
   *
   * @param meta metadata map
   */
  @Override
  @GenIgnore
  public void setMeta(JsonObject meta) {
    this._meta = meta;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ToolConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
