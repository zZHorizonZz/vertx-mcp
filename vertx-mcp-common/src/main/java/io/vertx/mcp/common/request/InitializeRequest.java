package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Implementation;
import io.vertx.mcp.common.capabilities.ClientCapabilities;

@DataObject
@JsonGen(publicConverter = false)
public class InitializeRequest extends Request {

  private static final String METHOD = "initialize";

  private String protocolVersion;
  private ClientCapabilities capabilities;
  private Implementation clientInfo;

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

  public ClientCapabilities getCapabilities() {
    return capabilities;
  }

  public InitializeRequest setCapabilities(ClientCapabilities capabilities) {
    this.capabilities = capabilities;
    return this;
  }

  public Implementation getClientInfo() {
    return clientInfo;
  }

  public InitializeRequest setClientInfo(Implementation clientInfo) {
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
