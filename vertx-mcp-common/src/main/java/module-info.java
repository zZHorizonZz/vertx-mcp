module io.vertx.mcp.common {
  requires io.vertx.jsonschema;
  requires io.vertx.codegen.api;

  requires io.vertx.codegen.json;
  requires io.vertx.core;

  exports io.vertx.mcp.common;
  exports io.vertx.mcp.common.capabilities;
  exports io.vertx.mcp.common.content;
  exports io.vertx.mcp.common.notification;
  exports io.vertx.mcp.common.prompt;
  exports io.vertx.mcp.common.reference;
  exports io.vertx.mcp.common.request;
  exports io.vertx.mcp.common.resources;
  exports io.vertx.mcp.common.result;
  exports io.vertx.mcp.common.root;
  exports io.vertx.mcp.common.rpc;
  exports io.vertx.mcp.common.sampling;
  exports io.vertx.mcp.common.tool;
  exports io.vertx.mcp.common.transport;
}
