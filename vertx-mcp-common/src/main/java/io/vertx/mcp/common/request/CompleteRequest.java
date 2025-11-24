package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;
import io.vertx.mcp.common.completion.CompletionArgument;
import io.vertx.mcp.common.completion.CompletionContext;
import io.vertx.mcp.common.completion.CompletionReference;

@DataObject
@JsonGen(publicConverter = false)
public class CompleteRequest extends Request {

  private static final String METHOD = "completion/complete";

  private CompletionReference ref;
  private CompletionArgument argument;
  private CompletionContext context;

  public CompleteRequest() {
    super(METHOD, null);
  }

  public CompleteRequest(JsonObject json) {
    super(METHOD, json.getJsonObject(Meta.META_KEY, new JsonObject()));
    CompleteRequestConverter.fromJson(json, this);
  }

  public CompletionReference getRef() {
    return ref;
  }

  public CompleteRequest setRef(CompletionReference ref) {
    this.ref = ref;
    return this;
  }

  public CompletionArgument getArgument() {
    return argument;
  }

  public CompleteRequest setArgument(CompletionArgument argument) {
    this.argument = argument;
    return this;
  }

  public CompletionContext getContext() {
    return context;
  }

  public CompleteRequest setContext(CompletionContext context) {
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
