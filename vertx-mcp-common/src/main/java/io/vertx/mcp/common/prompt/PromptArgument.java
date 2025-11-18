package io.vertx.mcp.common.prompt;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.ArraySchemaBuilder;

import java.util.ArrayList;
import java.util.List;

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
   * Converts an array schema to a list of PromptArgument objects. The schema should be an array schema with object items containing properties.
   *
   * @param builder the schema builder (array schema)
   * @return List of PromptArgument objects
   */
  public static List<PromptArgument> convertSchemaToArguments(ArraySchemaBuilder builder) {
    List<PromptArgument> arguments = new ArrayList<>();

    // Convert schema to JSON
    JsonObject schemaJson = builder.toJson();

    // Get the items field (should be an object schema)
    JsonObject itemsSchema = schemaJson.getJsonObject("items");
    if (itemsSchema == null) {
      return arguments;
    }

    // Get the properties from the object schema
    JsonObject properties = itemsSchema.getJsonObject("properties");
    if (properties == null) {
      return arguments;
    }

    // Get the required fields list
    JsonArray requiredFields = itemsSchema.getJsonArray("required");
    List<String> requiredList = new ArrayList<>();
    if (requiredFields != null) {
      for (int i = 0; i < requiredFields.size(); i++) {
        requiredList.add(requiredFields.getString(i));
      }
    }

    // Create PromptArgument for each property
    for (String propertyName : properties.fieldNames()) {
      JsonObject propertySchema = properties.getJsonObject(propertyName);

      PromptArgument argument = new PromptArgument()
        .setName(propertyName)
        .setRequired(requiredList.contains(propertyName));

      // Extract description if available
      if (propertySchema.containsKey("description")) {
        argument.setDescription(propertySchema.getString("description"));
      }

      // Extract title if available
      if (propertySchema.containsKey("title")) {
        argument.setTitle(propertySchema.getString("title"));
      }

      arguments.add(argument);
    }

    return arguments;
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
