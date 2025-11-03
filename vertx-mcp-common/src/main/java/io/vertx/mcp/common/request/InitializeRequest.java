package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class InitializeRequest extends Request {

  private static final String METHOD = "initialize";

  private String protocolVersion;
  private JsonObject capabilities;
  private JsonObject clientInfo;

  public InitializeRequest() {
    super(METHOD, null);
  }

  public InitializeRequest(JsonObject json) {
    this();
    InitializeRequestConverter.fromJson(json, this);
  }

  public String getProtocolVersion() {
    return protocolVersion;
  }

  public InitializeRequest setProtocolVersion(String protocolVersion) {
    this.protocolVersion = protocolVersion;
    return this;
  }

  public JsonObject getCapabilities() {
    return capabilities;
  }

  public InitializeRequest setCapabilities(JsonObject capabilities) {
    this.capabilities = capabilities;
    return this;
  }

  public JsonObject getClientInfo() {
    return clientInfo;
  }

  public InitializeRequest setClientInfo(JsonObject clientInfo) {
    this.clientInfo = clientInfo;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    InitializeRequestConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
