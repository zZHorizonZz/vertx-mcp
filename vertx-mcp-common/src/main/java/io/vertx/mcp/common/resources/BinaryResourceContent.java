package io.vertx.mcp.common.resources;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

/**
 * Binary resource content with blob data.
 */
@DataObject
@JsonGen(publicConverter = false)
public class BinaryResourceContent extends Resource<BinaryResourceContent> {

  private Buffer blob;

  public BinaryResourceContent() {
    super();
  }

  public BinaryResourceContent(JsonObject json) {
    super();
    BinaryResourceContentConverter.fromJson(json, this);
  }

  public BinaryResourceContent(String uri, String name, String title, String description, String mimeType, Buffer blob) {
    super(uri, name, title, description, mimeType);
    this.blob = blob;
  }

  /**
   * Gets the binary content as a Buffer.
   *
   * @return binary content buffer
   */
  public Buffer getBlob() {
    return blob;
  }

  /**
   * Sets the binary content.
   *
   * @param blob binary content buffer
   * @return this instance for method chaining
   */
  public BinaryResourceContent setBlob(Buffer blob) {
    this.blob = blob;
    return this;
  }

  @Override
  public Buffer getContent() {
    return blob != null ? blob.copy() : null;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    BinaryResourceContentConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
