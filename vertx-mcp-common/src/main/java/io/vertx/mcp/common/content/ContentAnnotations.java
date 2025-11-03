package io.vertx.mcp.common.content;

import io.vertx.mcp.common.Annotations;

import java.util.*;

/**
 * Annotations for content blocks that provide hints to clients about how to use or display the content.
 * <p>
 * Supports standard MCP annotations:
 * <ul>
 *   <li>{@code audience} - Array indicating intended audience(s): "user" and/or "assistant"</li>
 *   <li>{@code priority} - Number from 0.0 to 1.0 indicating importance (1 = most important, 0 = least important)</li>
 * </ul>
 * <p>
 * Also supports custom annotations via the Map interface.
 */
public class ContentAnnotations extends Annotations {

  private static final String AUDIENCE_KEY = "audience";
  private static final String PRIORITY_KEY = "priority";

  /**
   * Creates a new empty ContentAnnotations instance.
   */
  public ContentAnnotations() {
    super();
  }

  /**
   * Creates a new ContentAnnotations instance with the given initial annotations.
   *
   * @param annotations initial annotations
   */
  public ContentAnnotations(Map<String, Object> annotations) {
    super(annotations);
  }

  /**
   * Sets the audience for this content.
   *
   * @param audience list of intended audiences ("user" and/or "assistant")
   * @return this instance for method chaining
   */
  public ContentAnnotations audience(List<String> audience) {
    if (audience != null && !audience.isEmpty()) {
      for (String aud : audience) {
        if (!"user".equals(aud) && !"assistant".equals(aud)) {
          throw new IllegalArgumentException("Invalid audience value: " + aud + ". Must be 'user' or 'assistant'");
        }
      }
      delegate.put(AUDIENCE_KEY, new ArrayList<>(audience));
    } else {
      delegate.remove(AUDIENCE_KEY);
    }
    return this;
  }

  /**
   * Sets the audience for this content.
   *
   * @param audience intended audiences ("user" and/or "assistant")
   * @return this instance for method chaining
   */
  public ContentAnnotations audience(String... audience) {
    return audience(audience != null ? Arrays.asList(audience) : null);
  }

  /**
   * Gets the audience for this content.
   *
   * @return list of intended audiences, or null if not set
   */
  @SuppressWarnings("unchecked")
  public List<String> getAudience() {
    Object value = delegate.get(AUDIENCE_KEY);
    if (value instanceof List) {
      return (List<String>) value;
    }
    return null;
  }

  /**
   * Sets the priority for this content.
   *
   * @param priority number from 0.0 to 1.0 indicating importance (1 = most important, 0 = least important)
   * @return this instance for method chaining
   * @throws IllegalArgumentException if priority is not between 0.0 and 1.0
   */
  public ContentAnnotations priority(Double priority) {
    if (priority != null) {
      if (priority < 0.0 || priority > 1.0) {
        throw new IllegalArgumentException("Priority must be between 0.0 and 1.0, got: " + priority);
      }
      delegate.put(PRIORITY_KEY, priority);
    } else {
      delegate.remove(PRIORITY_KEY);
    }
    return this;
  }

  /**
   * Gets the priority for this content.
   *
   * @return priority value between 0.0 and 1.0, or null if not set
   */
  public Double getPriority() {
    Object value = delegate.get(PRIORITY_KEY);
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    return null;
  }
}
