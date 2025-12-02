module io.vertx.mcp.client {
  requires io.vertx.jsonschema;
  requires io.vertx.codegen.api;

  requires io.vertx.codegen.json;
  requires io.vertx.core;

  requires io.vertx.mcp.common;
  requires java.logging;
  requires io.vertx.mcp.client;

  exports io.vertx.mcp.client;
}
