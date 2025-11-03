package io.vertx.mcp.common.notification;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class PromptListChangedNotification extends Notification {

  private static final String METHOD = "notifications/prompts/list_changed";

  public PromptListChangedNotification() {
    super(METHOD, null);
  }

  public PromptListChangedNotification(JsonObject json) {
    this();
    PromptListChangedNotificationConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    PromptListChangedNotificationConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
