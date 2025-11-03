module io.vertx.mcp.common {
  requires io.vertx.jsonschema;
  requires io.vertx.codegen.api;

  requires io.vertx.codegen.json;
  requires io.vertx.core;

  exports io.vertx.mcp.common.content;
  exports io.vertx.mcp.common.resources;
  exports io.vertx.mcp.common.transport;
  exports io.vertx.mcp.common;
}
