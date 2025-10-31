package io.vertx.mcp.request;

import java.util.Map;

public abstract class Request {

  private final String method;
  private final Map<String, Object> _meta;

  public Request(String method, Map<String, Object> _meta) {
    this.method = method;
    this._meta = _meta;
  }

  public String method() {
    return method;
  }

  public Map<String, Object> meta() {
    return _meta;
  }
}
