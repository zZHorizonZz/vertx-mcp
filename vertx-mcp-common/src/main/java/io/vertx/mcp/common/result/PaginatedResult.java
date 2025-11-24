package io.vertx.mcp.common.result;

import io.vertx.core.json.JsonObject;

/**
 * Base class for paginated results that support cursor-based pagination.
 */
public abstract class PaginatedResult extends Result {

  private String nextCursor;

  protected PaginatedResult(JsonObject meta) {
    super(meta);
  }

  public String getNextCursor() {
    return nextCursor;
  }

  public PaginatedResult setNextCursor(String nextCursor) {
    this.nextCursor = nextCursor;
    return this;
  }
}
