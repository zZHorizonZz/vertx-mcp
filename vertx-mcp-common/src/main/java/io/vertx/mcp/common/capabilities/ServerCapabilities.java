package io.vertx.mcp.common.capabilities;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

/**
 * Capabilities that a server may support. Known capabilities are defined here, in this schema, but this is not a closed set: any server can define its own, additional
 * capabilities.
 */
@DataObject
@JsonGen(publicConverter = false)
public class ServerCapabilities {

  private JsonObject experimental;
  private JsonObject logging;
  private JsonObject completions;
  private PromptsCapability prompts;
  private ResourcesCapability resources;
  private ToolsCapability tools;

  public ServerCapabilities() {
  }

  public ServerCapabilities(JsonObject json) {
    ServerCapabilitiesConverter.fromJson(json, this);
  }

  /**
   * Gets the experimental, non-standard capabilities that the server supports.
   *
   * @return experimental capabilities
   */
  public JsonObject getExperimental() {
    return experimental;
  }

  /**
   * Sets the experimental, non-standard capabilities that the server supports.
   *
   * @param experimental experimental capabilities
   * @return this instance for method chaining
   */
  public ServerCapabilities setExperimental(JsonObject experimental) {
    this.experimental = experimental;
    return this;
  }

  /**
   * Gets the logging capability if the server supports sending log messages to the client.
   *
   * @return logging capability
   */
  public JsonObject getLogging() {
    return logging;
  }

  /**
   * Sets the logging capability if the server supports sending log messages to the client.
   *
   * @param logging logging capability
   * @return this instance for method chaining
   */
  public ServerCapabilities setLogging(JsonObject logging) {
    this.logging = logging;
    return this;
  }

  /**
   * Gets the completions capability if the server supports argument autocompletion suggestions.
   *
   * @return completions capability
   */
  public JsonObject getCompletions() {
    return completions;
  }

  /**
   * Sets the completions capability if the server supports argument autocompletion suggestions.
   *
   * @param completions completions capability
   * @return this instance for method chaining
   */
  public ServerCapabilities setCompletions(JsonObject completions) {
    this.completions = completions;
    return this;
  }

  /**
   * Gets the prompts capability if the server offers any prompt templates.
   *
   * @return prompts capability
   */
  public PromptsCapability getPrompts() {
    return prompts;
  }

  /**
   * Sets the prompts capability if the server offers any prompt templates.
   *
   * @param prompts prompts capability
   * @return this instance for method chaining
   */
  public ServerCapabilities setPrompts(PromptsCapability prompts) {
    this.prompts = prompts;
    return this;
  }

  /**
   * Gets the resources capability if the server offers any resources to read.
   *
   * @return resources capability
   */
  public ResourcesCapability getResources() {
    return resources;
  }

  /**
   * Sets the resources capability if the server offers any resources to read.
   *
   * @param resources resources capability
   * @return this instance for method chaining
   */
  public ServerCapabilities setResources(ResourcesCapability resources) {
    this.resources = resources;
    return this;
  }

  /**
   * Gets the tools capability if the server offers any tools to call.
   *
   * @return tools capability
   */
  public ToolsCapability getTools() {
    return tools;
  }

  /**
   * Sets the tools capability if the server offers any tools to call.
   *
   * @param tools tools capability
   * @return this instance for method chaining
   */
  public ServerCapabilities setTools(ToolsCapability tools) {
    this.tools = tools;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ServerCapabilitiesConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
