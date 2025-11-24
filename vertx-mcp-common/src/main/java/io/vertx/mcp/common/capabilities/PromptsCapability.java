package io.vertx.mcp.common.capabilities;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

/**
 * Capability for prompts support.
 */
@DataObject
@JsonGen(publicConverter = false)
public class PromptsCapability {
  private Boolean listChanged;

  public PromptsCapability() {
  }

  public PromptsCapability(JsonObject json) {
    PromptsCapabilityConverter.fromJson(json, this);
  }

  /**
   * Whether this server supports notifications for changes to the prompt list.
   *
   * @return true if list changed notifications are supported
   */
  public Boolean getListChanged() {
    return listChanged;
  }

  /**
   * Sets whether this server supports notifications for changes to the prompt list.
   *
   * @param listChanged true if list changed notifications are supported
   * @return this instance for method chaining
   */
  public PromptsCapability setListChanged(Boolean listChanged) {
    this.listChanged = listChanged;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    PromptsCapabilityConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
