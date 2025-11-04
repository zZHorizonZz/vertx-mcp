package io.vertx.mcp.common;

import io.vertx.codegen.annotations.GenIgnore;

import java.util.HashMap;
import java.util.Map;

/**
 * Interface for objects that support metadata. Provides consistent metadata handling across all MCP protocol objects.
 */
public interface Meta {

  /**
   * Get the metadata map.
   *
   * @return the metadata map, may be null
   */
  Map<String, Object> getMeta();

  /**
   * Set the metadata map.
   *
   * @param meta the metadata map to set
   */
  void setMeta(Map<String, Object> meta);

  /**
   * Add a metadata entry.
   *
   * @param key the metadata key
   * @param value the metadata value
   */
  @GenIgnore
  default void addMeta(String key, Object value) {
    if (getMeta() == null) {
      setMeta(new HashMap<>());
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
