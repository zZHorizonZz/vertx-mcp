package io.vertx.mcp.request;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;

import java.util.Map;

@DataObject
@JsonGen(publicConverter = false)
public class PingRequest extends Request {

  private static final String METHOD = "ping";

  public PingRequest() {
    super(METHOD, null);
  }

  public PingRequest(Map<String, Object> _meta) {
    super(METHOD, _meta);
  }
}
