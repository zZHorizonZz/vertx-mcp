package io.vertx.mcp.common.notification;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;
import io.vertx.mcp.common.rpc.JsonNotification;

@DataObject
public abstract class Notification implements Meta {

  private final String method;

  private JsonObject _meta;

  public Notification(String method, JsonObject _meta) {
    this.method = method;
    this._meta = _meta;
  }

  public abstract JsonObject toJson();

  public JsonNotification toNotification() {
    return JsonNotification.createNotification(method, toJson());
  }

  @GenIgnore
  public String getMethod() {
    return method;
  }

  @Override
  @GenIgnore
  public JsonObject getMeta() {
    return _meta;
  }

  @Override
  @GenIgnore
  public void setMeta(JsonObject meta) {
    _meta = meta;
  }

  @Override
  @GenIgnore
  public void addMeta(String key, Object value) {
    _meta.put(key, value);
  }

  @Override
  @GenIgnore
  public void removeMeta(String key) {
    _meta.remove(key);
  }
}
