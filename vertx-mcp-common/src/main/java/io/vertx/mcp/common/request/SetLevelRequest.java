package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.LoggingLevel;
import io.vertx.mcp.common.Meta;

@DataObject
@JsonGen(publicConverter = false)
public class SetLevelRequest extends Request {

  public static final String METHOD = "logging/setLevel";

  private LoggingLevel level;

  public SetLevelRequest() {
    super(METHOD, null);
  }

  public SetLevelRequest(JsonObject json) {
    super(METHOD, json.getJsonObject(Meta.META_KEY, new JsonObject()));
    SetLevelRequestConverter.fromJson(json, this);

    if (json.containsKey("level")) {
      level = LoggingLevel.valueOf(json.getString("level").toUpperCase());
    }
  }

  @GenIgnore
  public LoggingLevel getLevel() {
    return level;
  }

  @GenIgnore
  public SetLevelRequest setLevel(LoggingLevel level) {
    this.level = level;
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    SetLevelRequestConverter.toJson(this, json);

    if (this.level != null) {
      json.put("level", this.level.getValue());
    }

    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
