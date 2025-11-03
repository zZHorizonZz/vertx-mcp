package io.vertx.mcp.common.notification;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class ToolListChangedNotification extends Notification {

  private static final String METHOD = "notifications/tools/list_changed";

  public ToolListChangedNotification() {
    super(METHOD, null);
  }

  public ToolListChangedNotification(JsonObject json) {
    this();
    ToolListChangedNotificationConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ToolListChangedNotificationConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
