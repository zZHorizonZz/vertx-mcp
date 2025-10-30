package io.vertx.mcp.content;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

public interface Content {

  JsonObject toJson();

  interface StructuredJsonContent extends Content {
    static StructuredJsonContent create(JsonObject json) {
      return () -> json;
    }

    @Override
    default String type() {
      return "structured_json";
    }

    @Override
    default JsonObject toJson() {
      return json();
    }

    JsonObject json();
  }

  interface UnstructuredContent extends Content {
    static UnstructuredContent create(List<Content> content) {
      return () -> content;
    }

    static UnstructuredContent create(JsonObject json) {
      return () -> {
        JsonArray contentJson = json.getJsonArray("content");
        if (contentJson == null) {
          return List.of();
        }
        return contentJson.stream().map(o -> {
          if (!(o instanceof JsonObject)) {
            throw new IllegalArgumentException("Content must be a json object");
          }

          JsonObject contentJsonObject = (JsonObject) o;
          String type = contentJsonObject.getString("type");
          if (type == null) {
            type = contentJsonObject.getMap().keySet().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Content must have a type"));
            contentJsonObject = contentJsonObject.getJsonObject(type);
          }
          switch (type) {
            case "text":
              return TextContent.create(contentJsonObject);
            case "image":
              return ImageContent.create(contentJsonObject);
            case "audio":
              return AudioContent.create(contentJsonObject);
            case "resourceLink":
            case "resource_link":
              return ResourceLinkContent.create(contentJsonObject);
            default:
              throw new IllegalArgumentException("Content type not supported: " + type);
          }
        }).collect(Collectors.toUnmodifiableList());
      };
    }

    @Override
    default String type() {
      return "unstructured";
    }

    @Override
    default JsonObject toJson() {
      if (content().isEmpty()) {
        return new JsonObject().put("type", type());
      }
      return new JsonObject().put("type", type()).put("content", toJsonArray());
    }

    default JsonArray toJsonArray() {
      JsonArray contentJson = new JsonArray();
      for (Content content : content()) {
        contentJson.add(content.toJson());
      }
      return contentJson;
    }

    List<Content> content();
  }

  interface TextContent extends Content {
    static TextContent create(String text) {
      return () -> text;
    }

    static TextContent create(JsonObject json) {
      return create(json.getString("text"));
    }

    @Override
    default String type() {
      return "text";
    }

    @Override
    default JsonObject toJson() {
      return new JsonObject().put("type", type()).put("text", text());
    }

    String text();
  }

  interface ImageContent extends Content {
    static ImageContent create(String data, String mimeType) {
      return new ImageContent() {
        @Override
        public String data() {
          return data;
        }

        @Override
        public String mimeType() {
          return mimeType;
        }
      };
    }

    static ImageContent create(JsonObject json) {
      return create(json.getString("data"), json.getString("mimeType"));
    }

    @Override
    default String type() {
      return "image";
    }

    @Override
    default JsonObject toJson() {
      return new JsonObject()
        .put("type", type())
        .put("data", data())
        .put("mimeType", mimeType());
    }

    String data();

    String mimeType();
  }

  interface AudioContent extends Content {
    static AudioContent create(String data, String mimeType) {
      return new AudioContent() {
        @Override
        public String data() {
          return data;
        }

        @Override
        public String mimeType() {
          return mimeType;
        }
      };
    }

    static AudioContent create(JsonObject json) {
      return create(json.getString("data"), json.getString("mimeType"));
    }

    @Override
    default String type() {
      return "audio";
    }

    @Override
    default JsonObject toJson() {
      return new JsonObject()
        .put("type", type())
        .put("data", data())
        .put("mimeType", mimeType());
    }

    String data();

    String mimeType();
  }

  interface ResourceLinkContent extends Content {
    static ResourceLinkContent create(URI uri, String name, String description, String mimeType) {
      return new ResourceLinkContent() {
        @Override
        public URI uri() {
          return uri;
        }

        @Override
        public String name() {
          return name;
        }

        @Override
        public String description() {
          return description;
        }

        @Override
        public String mimeType() {
          return mimeType;
        }
      };
    }

    static ResourceLinkContent create(JsonObject json) {
      return create(
        URI.create(json.getString("uri")),
        json.getString("name"),
        json.getString("description"),
        json.getString("mimeType")
      );
    }

    @Override
    default String type() {
      return "resource_link";
    }

    @Override
    default JsonObject toJson() {
      return new JsonObject()
        .put("type", type())
        .put("uri", uri().toString())
        .put("name", name())
        .put("description", description())
        .put("mimeType", mimeType());
    }

    URI uri();

    String name();

    String description();

    String mimeType();
  }

  // TODO: Make this work with Resources
  interface EmbeddedResourceContent extends Content {
    default String type() {
      return "resource";
    }
  }
}
