package io.vertx.mcp.client;

import io.vertx.mcp.common.rpc.JsonError;

public class ClientRequestException extends RuntimeException {

  private final int code;
  private final Object data;

  public ClientRequestException(JsonError error) {
    super(error.getMessage());
    this.code = error.getCode();
    this.data = error.getData();
  }

  public ClientRequestException(int code, String message) {
    super(message);
    this.code = code;
    this.data = null;
  }

  public ClientRequestException(int code, String message, Object data) {
    super(message);
    this.code = code;
    this.data = data;
  }

  public int getCode() {
    return code;
  }

  public Object getData() {
    return data;
  }
}
