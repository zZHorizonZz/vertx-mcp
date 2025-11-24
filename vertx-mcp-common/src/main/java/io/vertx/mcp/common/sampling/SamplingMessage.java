package io.vertx.mcp.common.sampling;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

/**
 * Describes a message issued to or received from an LLM API.
 */
@DataObject
@JsonGen(publicConverter = false)
public class SamplingMessage {

  private String role;
  private JsonObject content;

  public SamplingMessage() {
  }

  public SamplingMessage(JsonObject json) {
    SamplingMessageConverter.fromJson(json, this);
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
  public SamplingMessage setRole(String role) {
    this.role = role;
    return this;
  }

  /**
   * Gets the getContent of the message (TextContent, ImageContent, or AudioContent).
   *
   * @return message getContent
   */
  public JsonObject getContent() {
    return content;
  }

  /**
   * Sets the getContent of the message (TextContent, ImageContent, or AudioContent).
   *
   * @param content message getContent
   * @return this instance for method chaining
   */
  public SamplingMessage setContent(JsonObject content) {
    this.content = content;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    SamplingMessageConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
