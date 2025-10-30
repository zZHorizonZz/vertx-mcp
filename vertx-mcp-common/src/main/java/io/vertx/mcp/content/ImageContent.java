package io.vertx.mcp.content;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class ImageContent extends BinaryContent {

    protected ImageContent(String mimeType, Buffer data) {
        super(mimeType, data);
    }

    @Override
    public JsonObject toJson() {
        return null;
    }
}
