package io.vertx.mcp.content;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class BinaryContent implements Content {

    private final String mimeType;
    private final Buffer data;

    protected BinaryContent(String mimeType, Buffer data) {
        this.mimeType = mimeType;
        this.data = data;
    }

    public String mimeType() {
        return mimeType;
    }

    public Buffer data() {
        return data;
    }

    @Override
    public JsonObject toJson() {
        return null;
    }
}
