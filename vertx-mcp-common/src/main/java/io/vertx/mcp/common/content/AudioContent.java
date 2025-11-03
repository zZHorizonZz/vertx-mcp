package io.vertx.mcp.common.content;

import io.vertx.core.buffer.Buffer;

public class AudioContent extends BinaryContent {

  public static final String TYPE = "audio";

  public AudioContent(String mimeType, Buffer data) {
    super(TYPE, mimeType, data);
  }
}
