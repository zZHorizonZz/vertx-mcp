package io.vertx.mcp.common.notification;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class LoggingMessageNotification extends Notification {

  private static final String METHOD = "notifications/message";

  private String level;
  private String logger;
  private Object data;

  public LoggingMessageNotification() {
    super(METHOD, null);
  }

  public LoggingMessageNotification(JsonObject json) {
    this();
    LoggingMessageNotificationConverter.fromJson(json, this);
  }

  public String getLevel() {
    return level;
  }

  public LoggingMessageNotification setLevel(String level) {
    this.level = level;
    return this;
  }

  public String getLogger() {
    return logger;
  }

  public LoggingMessageNotification setLogger(String logger) {
    this.logger = logger;
    return this;
  }

  public Object getData() {
    return data;
  }

  public LoggingMessageNotification setData(Object data) {
    this.data = data;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    LoggingMessageNotificationConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
