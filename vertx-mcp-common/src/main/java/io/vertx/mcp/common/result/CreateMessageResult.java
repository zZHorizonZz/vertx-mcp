package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class CreateMessageResult extends Result {

  private String role;
  private JsonObject content;
  private String model;
  private String stopReason;

  public CreateMessageResult() {
    super(null);
  }

  public CreateMessageResult(JsonObject json) {
    this();
    CreateMessageResultConverter.fromJson(json, this);
  }

  public String getRole() {
    return role;
  }

  public CreateMessageResult setRole(String role) {
    this.role = role;
    return this;
  }

  public JsonObject getContent() {
    return content;
  }

  public CreateMessageResult setContent(JsonObject content) {
    this.content = content;
    return this;
  }

  public String getModel() {
    return model;
  }

  public CreateMessageResult setModel(String model) {
    this.model = model;
    return this;
  }

  public String getStopReason() {
    return stopReason;
  }

  public CreateMessageResult setStopReason(String stopReason) {
    this.stopReason = stopReason;
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    CreateMessageResultConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
