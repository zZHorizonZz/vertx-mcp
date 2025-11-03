package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class InitializeResult extends Result {

  private String protocolVersion;
  private JsonObject capabilities;
  private JsonObject serverInfo;
  private String instructions;

  public InitializeResult() {
    super(null);
  }

  public InitializeResult(JsonObject json) {
    this();
    InitializeResultConverter.fromJson(json, this);
  }

  public String getProtocolVersion() {
    return protocolVersion;
  }

  public InitializeResult setProtocolVersion(String protocolVersion) {
    this.protocolVersion = protocolVersion;
    return this;
  }

  public JsonObject getCapabilities() {
    return capabilities;
  }

  public InitializeResult setCapabilities(JsonObject capabilities) {
    this.capabilities = capabilities;
    return this;
  }

  public JsonObject getServerInfo() {
    return serverInfo;
  }

  public InitializeResult setServerInfo(JsonObject serverInfo) {
    this.serverInfo = serverInfo;
    return this;
  }

  public String getInstructions() {
    return instructions;
  }

  public InitializeResult setInstructions(String instructions) {
    this.instructions = instructions;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    InitializeResultConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
