module io.vertx.mcp {
  requires io.vertx.jsonschema;
  requires io.vertx.codegen.api;

  requires io.vertx.codegen.json;
  requires io.vertx.core;

  exports io.vertx.mcp;
  exports io.vertx.mcp.content;
  exports io.vertx.mcp.resources;
  exports io.vertx.mcp.transport;
}
