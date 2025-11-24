package io.vertx.mcp.common.request;

import io.vertx.core.json.JsonObject;

/**
 * Base class for paginated requests that support cursor-based pagination.
 */
public abstract class PaginatedRequest extends Request {

  private String cursor;

  protected PaginatedRequest(String method, JsonObject meta) {
    super(method, meta);
  }

  public String getCursor() {
    return cursor;
  }

  public PaginatedRequest setCursor(String cursor) {
    this.cursor = cursor;
    return this;
  }
}
