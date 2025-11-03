package io.vertx.mcp.common;

import java.util.*;

/**
 * Abstract base class for annotations that provide hints to clients about how to use or display objects.
 * <p>
 * Implements Map to allow custom annotations beyond the standard ones.
 * Subclasses define their own specific annotation fields.
 */
public abstract class Annotations implements Map<String, Object> {

  protected final Map<String, Object> delegate;

  /**
   * Creates a new empty Annotations instance.
   */
  protected Annotations() {
    this.delegate = new HashMap<>();
  }

  /**
   * Creates a new Annotations instance with the given initial annotations.
   *
   * @param annotations initial annotations
   */
  protected Annotations(Map<String, Object> annotations) {
    this.delegate = new HashMap<>(annotations);
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

    Annotations that = (Annotations) o;
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
