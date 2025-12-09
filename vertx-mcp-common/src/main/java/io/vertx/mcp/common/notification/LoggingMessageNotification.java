package io.vertx.mcp.common.notification;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.LoggingLevel;

@DataObject
@JsonGen(publicConverter = false)
public class LoggingMessageNotification extends Notification {

  public static final String METHOD = "notifications/message";

  private LoggingLevel level;
  private String logger;
  private Object data;

  public LoggingMessageNotification() {
    super(METHOD, null);
  }

  public LoggingMessageNotification(JsonObject json) {
    this();
    LoggingMessageNotificationConverter.fromJson(json, this);

    if (json.containsKey("level")) {
      level = LoggingLevel.valueOf(json.getString("level").toUpperCase());
    }
  }

  @GenIgnore
  public LoggingLevel getLevel() {
    return level;
  }

  @GenIgnore
  public LoggingMessageNotification setLevel(LoggingLevel level) {
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

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    LoggingMessageNotificationConverter.toJson(this, json);

    // Vertx JsonGen is using name() which is not usable in this case so we overwrite.
    if (level != null) {
      json.put("level", level.getValue());
    }

    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
