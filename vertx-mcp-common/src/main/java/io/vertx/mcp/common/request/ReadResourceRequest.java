package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;

@DataObject
@JsonGen(publicConverter = false)
public class ReadResourceRequest extends Request {

  public static final String METHOD = "resources/read";

  private String uri;

  public ReadResourceRequest() {
    super(METHOD, null);
  }

  public ReadResourceRequest(JsonObject json) {
    super(METHOD, json.getJsonObject(Meta.META_KEY, new JsonObject()));
    ReadResourceRequestConverter.fromJson(json, this);
  }

  public String getUri() {
    return uri;
  }

  public ReadResourceRequest setUri(String uri) {
    this.uri = uri;
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ReadResourceRequestConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
