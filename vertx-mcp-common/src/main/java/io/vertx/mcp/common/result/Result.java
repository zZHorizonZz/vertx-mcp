package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;

import java.util.Map;

public abstract class Result implements Meta {

  private Map<String, Object> _meta;

  public Result(Map<String, Object> _meta) {
    this._meta = _meta;
  }

  public abstract JsonObject toJson();

  public JsonResponse toRequest(JsonRequest request) {
    return JsonResponse.success(request, toJson());
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
