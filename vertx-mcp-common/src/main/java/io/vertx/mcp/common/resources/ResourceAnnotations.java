package io.vertx.mcp.common.resources;

import java.util.*;

/**
 * Annotations for MCP resources that provide hints to clients about how to use or display the resource.
 * <p>
 * Supports standard MCP annotations:
 * <ul>
 *   <li>{@code audience} - Array indicating intended audience(s): "user" and/or "assistant"</li>
 *   <li>{@code priority} - Number from 0.0 to 1.0 indicating importance (1 = most important, 0 = least important)</li>
 *   <li>{@code lastModified} - ISO 8601 formatted timestamp indicating when resource was last modified</li>
 * </ul>
 * <p>
 * Also supports custom annotations via the Map interface.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/resources#annotations">Model Context Protocol Specification</a>
 */
public class ResourceAnnotations implements Map<String, Object> {

  private static final String AUDIENCE_KEY = "audience";
  private static final String PRIORITY_KEY = "priority";
  private static final String LAST_MODIFIED_KEY = "lastModified";

  private final Map<String, Object> delegate;

  /**
   * Creates a new empty ResourceAnnotations instance.
   */
  public ResourceAnnotations() {
    this.delegate = new HashMap<>();
  }

  /**
   * Creates a new ResourceAnnotations instance with the given initial annotations.
   *
   * @param annotations initial annotations
   */
  public ResourceAnnotations(Map<String, Object> annotations) {
    this.delegate = new HashMap<>(annotations);
  }

  /**
   * Sets the audience for this resource.
   *
   * @param audience list of intended audiences ("user" and/or "assistant")
   * @return this instance for method chaining
   */
  public ResourceAnnotations audience(List<String> audience) {
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
   * Sets the audience for this resource.
   *
   * @param audience intended audiences ("user" and/or "assistant")
   * @return this instance for method chaining
   */
  public ResourceAnnotations audience(String... audience) {
    return audience(audience != null ? Arrays.asList(audience) : null);
  }

  /**
   * Gets the audience for this resource.
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
   * Sets the priority for this resource.
   *
   * @param priority number from 0.0 to 1.0 indicating importance (1 = most important, 0 = least important)
   * @return this instance for method chaining
   * @throws IllegalArgumentException if priority is not between 0.0 and 1.0
   */
  public ResourceAnnotations priority(Double priority) {
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
   * Gets the priority for this resource.
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

  /**
   * Sets the last modified timestamp for this resource.
   *
   * @param lastModified ISO 8601 formatted timestamp (e.g., "2025-01-12T15:00:58Z")
   * @return this instance for method chaining
   */
  public ResourceAnnotations lastModified(String lastModified) {
    if (lastModified != null && !lastModified.isEmpty()) {
      delegate.put(LAST_MODIFIED_KEY, lastModified);
    } else {
      delegate.remove(LAST_MODIFIED_KEY);
    }
    return this;
  }

  /**
   * Gets the last modified timestamp for this resource.
   *
   * @return ISO 8601 formatted timestamp, or null if not set
   */
  public String getLastModified() {
    Object value = delegate.get(LAST_MODIFIED_KEY);
    return value != null ? value.toString() : null;
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return delegate.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return delegate.containsValue(value);
  }

  @Override
  public Object get(Object key) {
    return delegate.get(key);
  }

  @Override
  public Object put(String key, Object value) {
    return delegate.put(key, value);
  }

  @Override
  public Object remove(Object key) {
    return delegate.remove(key);
  }

  @Override
  public void putAll(Map<? extends String, ?> m) {
    delegate.putAll(m);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public Set<String> keySet() {
    return delegate.keySet();
  }

  @Override
  public Collection<Object> values() {
    return delegate.values();
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    return delegate.entrySet();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ResourceAnnotations that = (ResourceAnnotations) o;
    return Objects.equals(delegate, that.delegate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(delegate);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
