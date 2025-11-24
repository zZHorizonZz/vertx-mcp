package io.vertx.mcp.common.capabilities;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

/**
 * Capability for resources support.
 */
@DataObject
@JsonGen(publicConverter = false)
public class ResourcesCapability {
  private Boolean subscribe;
  private Boolean listChanged;

  public ResourcesCapability() {
  }

  public ResourcesCapability(JsonObject json) {
    ResourcesCapabilityConverter.fromJson(json, this);
  }

  /**
   * Whether this server supports subscribing to resource updates.
   *
   * @return true if subscriptions are supported
   */
  public Boolean getSubscribe() {
    return subscribe;
  }

  /**
   * Sets whether this server supports subscribing to resource updates.
   *
   * @param subscribe true if subscriptions are supported
   * @return this instance for method chaining
   */
  public ResourcesCapability setSubscribe(Boolean subscribe) {
    this.subscribe = subscribe;
    return this;
  }

  /**
   * Whether this server supports notifications for changes to the resource list.
   *
   * @return true if list changed notifications are supported
   */
  public Boolean getListChanged() {
    return listChanged;
  }

  /**
   * Sets whether this server supports notifications for changes to the resource list.
   *
   * @param listChanged true if list changed notifications are supported
   * @return this instance for method chaining
   */
  public ResourcesCapability setListChanged(Boolean listChanged) {
    this.listChanged = listChanged;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ResourcesCapabilityConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
