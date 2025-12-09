package io.vertx.mcp.common.notification;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class RootsListChangedNotification extends Notification {

  public static final String METHOD = "notifications/roots/list_changed";

  public RootsListChangedNotification() {
    super(METHOD, null);
  }

  public RootsListChangedNotification(JsonObject json) {
    this();
    RootsListChangedNotificationConverter.fromJson(json, this);
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    RootsListChangedNotificationConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
