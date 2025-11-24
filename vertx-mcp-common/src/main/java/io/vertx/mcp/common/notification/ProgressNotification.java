package io.vertx.mcp.common.notification;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class ProgressNotification extends Notification {

  private static final String METHOD = "notifications/progress";

  private String progressToken;
  private Double progress;
  private Double total;
  private String message;

  public ProgressNotification() {
    super(METHOD, null);
  }

  public ProgressNotification(JsonObject json) {
    this();
    ProgressNotificationConverter.fromJson(json, this);
  }

  public String getProgressToken() {
    return progressToken;
  }

  public ProgressNotification setProgressToken(String progressToken) {
    this.progressToken = progressToken;
    return this;
  }

  public Double getProgress() {
    return progress;
  }

  public ProgressNotification setProgress(Double progress) {
    this.progress = progress;
    return this;
  }

  public Double getTotal() {
    return total;
  }

  public ProgressNotification setTotal(Double total) {
    this.total = total;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public ProgressNotification setMessage(String message) {
    this.message = message;
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ProgressNotificationConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
