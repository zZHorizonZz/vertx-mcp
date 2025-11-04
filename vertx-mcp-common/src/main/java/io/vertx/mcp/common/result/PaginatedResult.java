package io.vertx.mcp.common.result;

import java.util.Map;

/**
 * Base class for paginated results that support cursor-based pagination.
 */
public abstract class PaginatedResult extends Result {

  private String nextCursor;

  protected PaginatedResult(Map<String, Object> meta) {
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
