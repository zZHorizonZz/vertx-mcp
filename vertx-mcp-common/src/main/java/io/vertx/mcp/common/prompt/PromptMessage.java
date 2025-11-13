package io.vertx.mcp.common.prompt;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.content.Content;

/**
 * Describes a message returned as part of a prompt. This is similar to SamplingMessage, but also supports the embedding of resources from the MCP server.
 */
@DataObject
@JsonGen(publicConverter = false)
public class PromptMessage {

  private String role;
  private JsonObject content;

  public PromptMessage() {
  }

  public PromptMessage(JsonObject json) {
    PromptMessageConverter.fromJson(json, this);
  }

  /**
   * Gets the role of the sender or recipient ("user" or "assistant").
   *
   * @return message role
   */
  public String getRole() {
    return role;
  }

  /**
   * Sets the role of the sender or recipient ("user" or "assistant").
   *
   * @param role message role
   * @return this instance for method chaining
   */
  public PromptMessage setRole(String role) {
    this.role = role;
    return this;
  }

  /**
   * Gets the getContent of the message as a ContentBlock array.
   *
   * @return message getContent
   */
  public JsonObject getContent() {
    return content;
  }

  /**
   * Sets the getContent of the message as a ContentBlock array.
   *
   * @param content message getContent
   * @return this instance for method chaining
   */
  public PromptMessage setContent(JsonObject content) {
    this.content = content;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    PromptMessageConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
