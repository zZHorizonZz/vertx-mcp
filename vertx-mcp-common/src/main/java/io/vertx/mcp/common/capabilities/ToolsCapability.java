package io.vertx.mcp.common.capabilities;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

/**
 * Capability for tools support.
 */
@DataObject
@JsonGen(publicConverter = false)
public class ToolsCapability {
  private Boolean listChanged;

  public ToolsCapability() {
  }

  public ToolsCapability(JsonObject json) {
    ToolsCapabilityConverter.fromJson(json, this);
  }

  /**
   * Whether this server supports notifications for changes to the tool list.
   *
   * @return true if list changed notifications are supported
   */
  public Boolean getListChanged() {
    return listChanged;
  }

  /**
   * Sets whether this server supports notifications for changes to the tool list.
   *
   * @param listChanged true if list changed notifications are supported
   * @return this instance for method chaining
   */
  public ToolsCapability setListChanged(Boolean listChanged) {
    this.listChanged = listChanged;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ToolsCapabilityConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
