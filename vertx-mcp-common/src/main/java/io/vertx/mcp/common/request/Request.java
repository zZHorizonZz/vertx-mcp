package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;
import io.vertx.mcp.common.rpc.JsonRequest;

@DataObject
public abstract class Request implements Meta {

  private final String method;

  private JsonObject _meta;

  public Request(String method, JsonObject _meta) {
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
