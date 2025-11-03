package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class SetLevelRequest extends Request {

  private static final String METHOD = "logging/setLevel";

  private String level;

  public SetLevelRequest() {
    super(METHOD, null);
  }

  public SetLevelRequest(JsonObject json) {
    this();
    SetLevelRequestConverter.fromJson(json, this);
  }

  public String getLevel() {
    return level;
  }

  public SetLevelRequest setLevel(String level) {
    this.level = level;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    SetLevelRequestConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
