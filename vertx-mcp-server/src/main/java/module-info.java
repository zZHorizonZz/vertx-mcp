module io.vertx.mcp.server {
  requires io.vertx.jsonschema;
  requires io.vertx.codegen.api;

  requires io.vertx.codegen.json;
  requires io.vertx.core;

  requires io.vertx.mcp.common;
  requires java.logging;
  requires io.vertx.mcp.server;

  exports io.vertx.mcp.server;
  exports io.vertx.mcp.server.feature;
  exports io.vertx.mcp.server.transport.http;
}
