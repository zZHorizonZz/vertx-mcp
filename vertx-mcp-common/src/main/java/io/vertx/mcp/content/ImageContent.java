package io.vertx.mcp.content;

import io.vertx.core.buffer.Buffer;

public class ImageContent extends BinaryContent {

  public static final String TYPE = "image";

  public ImageContent(String mimeType, Buffer data) {
    super(TYPE, mimeType, data);
  }
}
