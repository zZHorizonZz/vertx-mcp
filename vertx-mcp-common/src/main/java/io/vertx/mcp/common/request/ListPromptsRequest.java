package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;

@DataObject
@JsonGen(publicConverter = false)
public class ListPromptsRequest extends PaginatedRequest {

  public static final String METHOD = "prompts/list";

  public ListPromptsRequest() {
    super(METHOD, null);
  }

  public ListPromptsRequest(JsonObject json) {
    super(METHOD, json.getJsonObject(Meta.META_KEY, new JsonObject()));
    ListPromptsRequestConverter.fromJson(json, this);
  }

  @Override
  public ListPromptsRequest setCursor(String cursor) {
    super.setCursor(cursor);
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ListPromptsRequestConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
