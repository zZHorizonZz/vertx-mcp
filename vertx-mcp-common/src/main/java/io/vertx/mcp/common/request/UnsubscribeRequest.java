package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class UnsubscribeRequest extends Request {

  private static final String METHOD = "resources/unsubscribe";

  private String uri;

  public UnsubscribeRequest() {
    super(METHOD, null);
  }

  public UnsubscribeRequest(JsonObject json) {
    this();
    UnsubscribeRequestConverter.fromJson(json, this);
  }

  public String getUri() {
    return uri;
  }

  public UnsubscribeRequest setUri(String uri) {
    this.uri = uri;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    UnsubscribeRequestConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
