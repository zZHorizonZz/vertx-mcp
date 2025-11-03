package io.vertx.mcp.common.capabilities;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

/**
 * Capability for roots support.
 */
@DataObject
@JsonGen(publicConverter = false)
public class RootsCapability {
  private Boolean listChanged;

  public RootsCapability() {
  }

  public RootsCapability(JsonObject json) {
    RootsCapabilityConverter.fromJson(json, this);
  }

  /**
   * Whether the client supports notifications for changes to the roots list.
   *
   * @return true if list changed notifications are supported
   */
  public Boolean getListChanged() {
    return listChanged;
  }

  /**
   * Sets whether the client supports notifications for changes to the roots list.
   *
   * @param listChanged true if list changed notifications are supported
   * @return this instance for method chaining
   */
  public RootsCapability setListChanged(Boolean listChanged) {
    this.listChanged = listChanged;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    RootsCapabilityConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
