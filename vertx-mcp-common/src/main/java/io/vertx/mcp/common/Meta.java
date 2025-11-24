package io.vertx.mcp.common;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Context;
import io.vertx.core.json.JsonObject;

/**
 * Interface for objects that support metadata. Provides consistent metadata handling across all MCP protocol objects.
 */
@VertxGen
public interface Meta {

  String META_KEY = "_meta";

  String MCP_META_CONTEXT_KEY = "mcp.request.meta";

  /**
   * Retrieve the current meta from the Vert.x context.
   *
   * @param context the Vert.x context
   * @return the meta, or null if no session is stored in the context
   */
  static JsonObject fromContext(Context context) {
    return context.get(MCP_META_CONTEXT_KEY);
  }

  /**
   * Get the metadata map.
   *
   * @return the metadata map, may be null
   */
  @GenIgnore
  JsonObject getMeta();

  /**
   * Set the metadata map.
   *
   * @param meta the metadata map to set
   */
  @GenIgnore
  void setMeta(JsonObject meta);

  /**
   * Add a metadata entry.
   *
   * @param key the metadata key
   * @param value the metadata value
   */
  @GenIgnore
  default void addMeta(String key, Object value) {
    if (getMeta() == null) {
      setMeta(new JsonObject());
    }
    getMeta().put(key, value);
  }

  /**
   * Remove a metadata entry.
   *
   * @param key the metadata key to remove
   */
  @GenIgnore
  default void removeMeta(String key) {
    if (getMeta() != null) {
      getMeta().remove(key);
    }
  }
}
