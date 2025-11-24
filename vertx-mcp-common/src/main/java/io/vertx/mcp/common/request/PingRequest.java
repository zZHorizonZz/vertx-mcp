package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;

@DataObject
@JsonGen(publicConverter = false)
public class PingRequest extends Request {

  private static final String METHOD = "ping";

  public PingRequest() {
    super(METHOD, null);
  }

  public PingRequest(JsonObject json) {
    super(METHOD, json.getJsonObject(Meta.META_KEY, new JsonObject()));
    PingRequestConverter.fromJson(json, this);
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    PingRequestConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
