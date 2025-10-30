package io.vertx.mcp.resources;

import io.vertx.core.buffer.Buffer;

import java.net.URI;

public class TextResourceContent extends Resource {

    private final String text;

    public TextResourceContent(URI uri, String name, String title, String description, String mimeType, String text) {
        super(uri, name, title, description, mimeType);
        this.text = text;
    }

    public String text() {
        return text;
    }

    @Override
    public Buffer content() {
        return Buffer.buffer(text);
    }
}
