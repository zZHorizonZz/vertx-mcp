package io.vertx.mcp.common.tool;

import io.vertx.mcp.common.Annotations;

import java.util.Map;

/**
 * Additional properties describing a Tool to clients.
 * <p>
 * NOTE: all properties in ToolAnnotations are <strong>hints</strong>.
 * They are not guaranteed to provide a faithful description of
 * tool behavior (including descriptive properties like title).
 * <p>
 * Clients should never make tool use decisions based on ToolAnnotations
 * received from untrusted servers.
 */
public class ToolAnnotations extends Annotations {

  private static final String TITLE_KEY = "title";
  private static final String READ_ONLY_HINT_KEY = "readOnlyHint";
  private static final String DESTRUCTIVE_HINT_KEY = "destructiveHint";
  private static final String IDEMPOTENT_HINT_KEY = "idempotentHint";
  private static final String OPEN_WORLD_HINT_KEY = "openWorldHint";

  /**
   * Creates a new empty ToolAnnotations instance.
   */
  public ToolAnnotations() {
    super();
  }

  /**
   * Creates a new ToolAnnotations instance with the given initial annotations.
   *
   * @param annotations initial annotations
   */
  public ToolAnnotations(Map<String, Object> annotations) {
    super(annotations);
  }

  /**
   * Sets a human-readable title for the tool.
   *
   * @param title tool title
   * @return this instance for method chaining
   */
  public ToolAnnotations title(String title) {
    if (title != null && !title.isEmpty()) {
      delegate.put(TITLE_KEY, title);
    } else {
      delegate.remove(TITLE_KEY);
    }
    return this;
  }

  /**
   * Gets a human-readable title for the tool.
   *
   * @return tool title
   */
  public String getTitle() {
    Object value = delegate.get(TITLE_KEY);
    return value != null ? value.toString() : null;
  }

  /**
   * Sets whether the tool does not modify its environment.
   * Default: false
   *
   * @param readOnlyHint true if read-only
   * @return this instance for method chaining
   */
  public ToolAnnotations readOnlyHint(Boolean readOnlyHint) {
    if (readOnlyHint != null) {
      delegate.put(READ_ONLY_HINT_KEY, readOnlyHint);
    } else {
      delegate.remove(READ_ONLY_HINT_KEY);
    }
    return this;
  }

  /**
   * If true, the tool does not modify its environment.
   * Default: false
   *
   * @return true if read-only
   */
  public Boolean getReadOnlyHint() {
    Object value = delegate.get(READ_ONLY_HINT_KEY);
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    return null;
  }

  /**
   * Sets whether the tool may perform destructive updates.
   * If false, the tool performs only additive updates.
   * (This property is meaningful only when readOnlyHint == false)
   * Default: true
   *
   * @param destructiveHint true if destructive
   * @return this instance for method chaining
   */
  public ToolAnnotations destructiveHint(Boolean destructiveHint) {
    if (destructiveHint != null) {
      delegate.put(DESTRUCTIVE_HINT_KEY, destructiveHint);
    } else {
      delegate.remove(DESTRUCTIVE_HINT_KEY);
    }
    return this;
  }

  /**
   * If true, the tool may perform destructive updates to its environment.
   * If false, the tool performs only additive updates.
   * (This property is meaningful only when readOnlyHint == false)
   * Default: true
   *
   * @return true if destructive
   */
  public Boolean getDestructiveHint() {
    Object value = delegate.get(DESTRUCTIVE_HINT_KEY);
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    return null;
  }

  /**
   * Sets whether the tool is idempotent.
   * If true, calling the tool repeatedly with the same arguments
   * will have no additional effect on its environment.
   * (This property is meaningful only when readOnlyHint == false)
   * Default: false
   *
   * @param idempotentHint true if idempotent
   * @return this instance for method chaining
   */
  public ToolAnnotations idempotentHint(Boolean idempotentHint) {
    if (idempotentHint != null) {
      delegate.put(IDEMPOTENT_HINT_KEY, idempotentHint);
    } else {
      delegate.remove(IDEMPOTENT_HINT_KEY);
    }
    return this;
  }

  /**
   * If true, calling the tool repeatedly with the same arguments
   * will have no additional effect on its environment.
   * (This property is meaningful only when readOnlyHint == false)
   * Default: false
   *
   * @return true if idempotent
   */
  public Boolean getIdempotentHint() {
    Object value = delegate.get(IDEMPOTENT_HINT_KEY);
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    return null;
  }

  /**
   * Sets whether this tool interacts with an open world.
   * If true, this tool may interact with an "open world" of external
   * entities. If false, the tool's domain of interaction is closed.
   * For example, the world of a web search tool is open, whereas that
   * of a memory tool is not.
   * Default: true
   *
   * @param openWorldHint true if open-world
   * @return this instance for method chaining
   */
  public ToolAnnotations openWorldHint(Boolean openWorldHint) {
    if (openWorldHint != null) {
      delegate.put(OPEN_WORLD_HINT_KEY, openWorldHint);
    } else {
      delegate.remove(OPEN_WORLD_HINT_KEY);
    }
    return this;
  }

  /**
   * If true, this tool may interact with an "open world" of external
   * entities. If false, the tool's domain of interaction is closed.
   * For example, the world of a web search tool is open, whereas that
   * of a memory tool is not.
   * Default: true
   *
   * @return true if open-world
   */
  public Boolean getOpenWorldHint() {
    Object value = delegate.get(OPEN_WORLD_HINT_KEY);
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    return null;
  }
}
