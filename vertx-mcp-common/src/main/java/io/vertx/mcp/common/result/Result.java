package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;

@DataObject
public abstract class Result implements Meta {

  private JsonObject _meta;

  public Result(JsonObject _meta) {
    this._meta = _meta;
  }

  public abstract JsonObject toJson();

  public JsonResponse toResponse(JsonRequest request) {
    return JsonResponse.success(request, toJson());
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
