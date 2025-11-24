package io.vertx.mcp.common.resources;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

/**
 * Text resource content with UTF-8 text data.
 */
@DataObject
@JsonGen(publicConverter = false)
public class TextResourceContent extends Resource<TextResourceContent> {

  private String text;

  public TextResourceContent() {
    super();
  }

  public TextResourceContent(JsonObject json) {
    super();
    TextResourceContentConverter.fromJson(json, this);
  }

  public TextResourceContent(String uri, String name, String title, String description, String mimeType, String text) {
    super(uri, name, title, description, mimeType);
    this.text = text;
  }

  /**
   * Gets the text content.
   *
   * @return text content
   */
  public String getText() {
    return text;
  }

  /**
   * Sets the text content.
   *
   * @param text text content
   * @return this instance for method chaining
   */
  public TextResourceContent setText(String text) {
    this.text = text;
    return this;
  }

  @Override
  public Buffer getContent() {
    return text != null ? Buffer.buffer(text) : null;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    TextResourceContentConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
