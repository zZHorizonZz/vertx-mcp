package io.vertx.mcp.common.notification;

import io.vertx.codegen.annotations.GenIgnore;

import java.util.Map;

public abstract class Notification {

  private final String method;

  private Map<String, Object> _meta;

  public Notification(String method, Map<String, Object> _meta) {
    this.method = method;
    this._meta = _meta;
  }

  @GenIgnore
  public String getMethod() {
    return method;
  }

  public Map<String, Object> getMeta() {
    return _meta;
  }

  @GenIgnore
  public void addMeta(String key, Object value) {
    _meta.put(key, value);
  }

  @GenIgnore
  public void removeMeta(String key) {
    _meta.remove(key);
  }

  public void setMeta(Map<String, Object> meta) {
    _meta = meta;
  }
}
