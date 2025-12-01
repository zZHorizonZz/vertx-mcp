package io.vertx.mcp.client;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.mcp.common.notification.Notification;

@VertxGen
public interface ClientNotificationHandler extends Handler<Notification> {
}
