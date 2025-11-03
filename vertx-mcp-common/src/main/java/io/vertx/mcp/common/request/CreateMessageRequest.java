package io.vertx.mcp.common.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@DataObject
@JsonGen(publicConverter = false)
public class CreateMessageRequest extends Request {

  private static final String METHOD = "sampling/createMessage";

  private JsonArray messages;
  private JsonObject modelPreferences;
  private String systemPrompt;
  private String includeContext;
  private Double temperature;
  private Integer maxTokens;
  private JsonArray stopSequences;
  private JsonObject metadata;

  public CreateMessageRequest() {
    super(METHOD, null);
  }

  public CreateMessageRequest(JsonObject json) {
    this();
    CreateMessageRequestConverter.fromJson(json, this);
  }

  public JsonArray getMessages() {
    return messages;
  }

  public CreateMessageRequest setMessages(JsonArray messages) {
    this.messages = messages;
    return this;
  }

  public JsonObject getModelPreferences() {
    return modelPreferences;
  }

  public CreateMessageRequest setModelPreferences(JsonObject modelPreferences) {
    this.modelPreferences = modelPreferences;
    return this;
  }

  public String getSystemPrompt() {
    return systemPrompt;
  }

  public CreateMessageRequest setSystemPrompt(String systemPrompt) {
    this.systemPrompt = systemPrompt;
    return this;
  }

  public String getIncludeContext() {
    return includeContext;
  }

  public CreateMessageRequest setIncludeContext(String includeContext) {
    this.includeContext = includeContext;
    return this;
  }

  public Double getTemperature() {
    return temperature;
  }

  public CreateMessageRequest setTemperature(Double temperature) {
    this.temperature = temperature;
    return this;
  }

  public Integer getMaxTokens() {
    return maxTokens;
  }

  public CreateMessageRequest setMaxTokens(Integer maxTokens) {
    this.maxTokens = maxTokens;
    return this;
  }

  public JsonArray getStopSequences() {
    return stopSequences;
  }

  public CreateMessageRequest setStopSequences(JsonArray stopSequences) {
    this.stopSequences = stopSequences;
    return this;
  }

  public JsonObject getMetadata() {
    return metadata;
  }

  public CreateMessageRequest setMetadata(JsonObject metadata) {
    this.metadata = metadata;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    CreateMessageRequestConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
