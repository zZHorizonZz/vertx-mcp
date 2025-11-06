package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;
import io.vertx.mcp.common.rpc.JsonRequest;

import java.util.Map;

public abstract class Request implements Meta {

  private final String method;

  private Map<String, Object> _meta;

  public Request(String method, Map<String, Object> _meta) {
    this.method = method;
    this._meta = _meta;
  }

  public abstract JsonObject toJson();

  public JsonRequest toRequest(Integer id) {
    return JsonRequest.createRequest(method, toJson(), id);
  }

  @GenIgnore
  public String getMethod() {
    return method;
  }

  @Override
  public Map<String, Object> getMeta() {
    return _meta;
  }

  @Override
  public void setMeta(Map<String, Object> meta) {
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
