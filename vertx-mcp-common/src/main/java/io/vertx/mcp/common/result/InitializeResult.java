package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Implementation;
import io.vertx.mcp.common.capabilities.ServerCapabilities;

@DataObject
@JsonGen(publicConverter = false)
public class InitializeResult extends Result {

  public static final String DEFAULT_PROTOCOL_VERSION = "2025-06-18";

  private String protocolVersion = DEFAULT_PROTOCOL_VERSION;

  private ServerCapabilities capabilities;
  private Implementation serverInfo;
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

  public ServerCapabilities getCapabilities() {
    return capabilities;
  }

  public InitializeResult setCapabilities(ServerCapabilities capabilities) {
    this.capabilities = capabilities;
    return this;
  }

  public Implementation getServerInfo() {
    return serverInfo;
  }

  public InitializeResult setServerInfo(Implementation serverInfo) {
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

  @Override
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
