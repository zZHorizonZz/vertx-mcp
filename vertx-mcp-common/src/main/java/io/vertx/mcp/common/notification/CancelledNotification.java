package io.vertx.mcp.common.notification;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class CancelledNotification extends Notification {

  public static final String METHOD = "notifications/cancelled";

  private String requestId;
  private String reason;

  public CancelledNotification() {
    super(METHOD, null);
  }

  public CancelledNotification(JsonObject json) {
    this();
    CancelledNotificationConverter.fromJson(json, this);
  }

  public String getRequestId() {
    return requestId;
  }

  public CancelledNotification setRequestId(String requestId) {
    this.requestId = requestId;
    return this;
  }

  public String getReason() {
    return reason;
  }

  public CancelledNotification setReason(String reason) {
    this.reason = reason;
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    CancelledNotificationConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
