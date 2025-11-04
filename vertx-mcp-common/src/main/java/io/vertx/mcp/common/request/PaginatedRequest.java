package io.vertx.mcp.common.request;

import java.util.Map;

/**
 * Base class for paginated requests that support cursor-based pagination.
 */
public abstract class PaginatedRequest extends Request {

  private String cursor;

  protected PaginatedRequest(String method, Map<String, Object> meta) {
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
