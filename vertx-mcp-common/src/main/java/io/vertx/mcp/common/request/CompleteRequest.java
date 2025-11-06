package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class CompleteRequest extends Request {

  private static final String METHOD = "completion/complete";

  private JsonObject ref;
  private JsonObject argument;
  private JsonObject context;

  public CompleteRequest() {
    super(METHOD, null);
  }

  public CompleteRequest(JsonObject json) {
    this();
    CompleteRequestConverter.fromJson(json, this);
  }

  public JsonObject getRef() {
    return ref;
  }

  public CompleteRequest setRef(JsonObject ref) {
    this.ref = ref;
    return this;
  }

  public JsonObject getArgument() {
    return argument;
  }

  public CompleteRequest setArgument(JsonObject argument) {
    this.argument = argument;
    return this;
  }

  public JsonObject getContext() {
    return context;
  }

  public CompleteRequest setContext(JsonObject context) {
    this.context = context;
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    CompleteRequestConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
