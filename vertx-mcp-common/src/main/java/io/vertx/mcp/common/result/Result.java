package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.GenIgnore;

import java.util.Map;

public abstract class Result {

  private Map<String, Object> _meta;

  public Result(Map<String, Object> _meta) {
    this._meta = _meta;
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
