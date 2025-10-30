package io.vertx.mcp.resources;

import io.vertx.core.buffer.Buffer;

import java.net.URI;

public class BinaryResourceContent extends Resource {

    private final Buffer blob;

    public BinaryResourceContent(URI uri, String name, String title, String description, String mimeType, Buffer blob) {
        super(uri, name, title, description, mimeType);
        this.blob = blob;
    }

    public Buffer blob() {
        return blob;
    }

    @Override
    public Buffer content() {
        return blob.copy();
    }
}
