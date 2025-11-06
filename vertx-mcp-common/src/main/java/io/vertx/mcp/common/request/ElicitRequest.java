package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class ElicitRequest extends Request {

  private static final String METHOD = "elicitation/create";

  private String message;
  private JsonObject requestedSchema;

  public ElicitRequest() {
    super(METHOD, null);
  }

  public ElicitRequest(JsonObject json) {
    this();
    ElicitRequestConverter.fromJson(json, this);
  }

  public String getMessage() {
    return message;
  }

  public ElicitRequest setMessage(String message) {
    this.message = message;
    return this;
  }

  public JsonObject getRequestedSchema() {
    return requestedSchema;
  }

  public ElicitRequest setRequestedSchema(JsonObject requestedSchema) {
    this.requestedSchema = requestedSchema;
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ElicitRequestConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
