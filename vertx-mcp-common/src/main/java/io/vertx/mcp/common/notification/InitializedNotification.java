package io.vertx.mcp.common.notification;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class InitializedNotification extends Notification {

  public static final String METHOD = "notifications/initialized";

  public InitializedNotification() {
    super(METHOD, null);
  }

  public InitializedNotification(JsonObject json) {
    this();
    InitializedNotificationConverter.fromJson(json, this);
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    InitializedNotificationConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
