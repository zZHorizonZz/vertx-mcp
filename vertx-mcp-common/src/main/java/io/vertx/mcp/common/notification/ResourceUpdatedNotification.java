package io.vertx.mcp.common.notification;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class ResourceUpdatedNotification extends Notification {

  public static final String METHOD = "notifications/resources/updated";

  private String uri;

  public ResourceUpdatedNotification() {
    super(METHOD, null);
  }

  public ResourceUpdatedNotification(JsonObject json) {
    this();
    ResourceUpdatedNotificationConverter.fromJson(json, this);
  }

  public String getUri() {
    return uri;
  }

  public ResourceUpdatedNotification setUri(String uri) {
    this.uri = uri;
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ResourceUpdatedNotificationConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
