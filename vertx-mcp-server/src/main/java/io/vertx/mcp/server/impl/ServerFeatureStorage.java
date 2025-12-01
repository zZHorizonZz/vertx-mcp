package io.vertx.mcp.server.impl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.rpc.JsonNotification;
import io.vertx.mcp.server.ServerFeatureHandler;
import io.vertx.mcp.server.ServerNotification;
import io.vertx.mcp.server.SessionManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A Map implementation for feature handlers that sends notifications when items are added, removed, or cleared.
 *
 * @param <T> the type of handler being stored
 */
public class ServerFeatureStorage<T extends ServerFeatureHandler<?, ?, ?>> implements Map<String, T> {

  private final Map<String, T> storage = new HashMap<>();
  private final Supplier<Vertx> vertxSupplier;
  private final String notificationMethod;

  public ServerFeatureStorage(Supplier<Vertx> vertxSupplier, String notificationMethod) {
    this.vertxSupplier = vertxSupplier;
    this.notificationMethod = notificationMethod;
  }

  @Override
  public T put(String key, T handler) {
    T previous = storage.put(key, handler);
    sendListChangedNotification();
    return previous;
  }

  @Override
  public T remove(Object key) {
    T removed = storage.remove(key);
    if (removed != null) {
      sendListChangedNotification();
    }
    return removed;
  }

  @Override
  public void putAll(Map<? extends String, ? extends T> m) {
    if (!m.isEmpty()) {
      storage.putAll(m);
      sendListChangedNotification();
    }
  }

  @Override
  public T get(Object key) {
    return storage.get(key);
  }

  @Override
  public boolean containsKey(Object key) {
    return storage.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return storage.containsValue(value);
  }

  @Override
  public Collection<T> values() {
    return storage.values();
  }

  @Override
  public Set<String> keySet() {
    return storage.keySet();
  }

  @Override
  public Set<Entry<String, T>> entrySet() {
    return storage.entrySet();
  }

  @Override
  public int size() {
    return storage.size();
  }

  @Override
  public boolean isEmpty() {
    return storage.isEmpty();
  }

  @Override
  public void clear() {
    if (!storage.isEmpty()) {
      storage.clear();
      sendListChangedNotification();
    }
  }

  private void sendListChangedNotification() {
    Vertx vertx = vertxSupplier.get();
    if (vertx == null || notificationMethod == null) {
      return;
    }

    JsonNotification notification = new JsonNotification(notificationMethod, new JsonObject());
    vertx.eventBus().send(SessionManager.NOTIFICATION_ADDRESS, new ServerNotification().setNotification(notification).setBroadcast(true).toJson());
  }
}
