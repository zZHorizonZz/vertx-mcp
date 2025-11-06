package io.vertx.mcp.common.notification;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class ResourceListChangedNotification extends Notification {

  private static final String METHOD = "notifications/resources/list_changed";

  public ResourceListChangedNotification() {
    super(METHOD, null);
  }

  public ResourceListChangedNotification(JsonObject json) {
    this();
    ResourceListChangedNotificationConverter.fromJson(json, this);
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ResourceListChangedNotificationConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
