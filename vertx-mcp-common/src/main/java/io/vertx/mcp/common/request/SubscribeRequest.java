package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;

@DataObject
@JsonGen(publicConverter = false)
public class SubscribeRequest extends Request {

  private static final String METHOD = "resources/subscribe";

  private String uri;

  public SubscribeRequest() {
    super(METHOD, null);
  }

  public SubscribeRequest(JsonObject json) {
    super(METHOD, json.getJsonObject(Meta.META_KEY, new JsonObject()));
    SubscribeRequestConverter.fromJson(json, this);
  }

  public String getUri() {
    return uri;
  }

  public SubscribeRequest setUri(String uri) {
    this.uri = uri;
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    SubscribeRequestConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
