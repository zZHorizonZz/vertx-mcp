package io.vertx.mcp.server;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.notification.Notification;
import io.vertx.mcp.common.rpc.JsonNotification;

@DataObject
@JsonGen(publicConverter = false)
public class ServerNotification {

  private JsonObject notification;
  private boolean broadcast;

  public ServerNotification() {

  }

  public ServerNotification(ServerNotification notification) {
    this.notification = notification.notification;
    this.broadcast = notification.broadcast;
  }

  public ServerNotification(JsonObject json) {
    this();
    ServerNotificationConverter.fromJson(json, this);
  }

  public JsonObject getNotification() {
    return notification;
  }

  public ServerNotification setNotification(JsonObject notification) {
    this.notification = notification;
    return this;
  }

  @GenIgnore
  public ServerNotification setNotification(JsonNotification notification) {
    return setNotification(notification.toJson());
  }

  @GenIgnore
  public ServerNotification setNotification(Notification notification) {
    return setNotification(notification.toNotification());
  }

  public boolean isBroadcast() {
    return broadcast;
  }

  public ServerNotification setBroadcast(boolean broadcast) {
    this.broadcast = broadcast;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ServerNotificationConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
