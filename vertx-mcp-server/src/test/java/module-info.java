open module io.vertx.tests.server {
  requires io.vertx.core;
  requires io.vertx.jsonschema;
  requires io.vertx.testing.unit;

  requires junit;
  requires testcontainers;

  requires io.vertx.mcp.server;
  requires io.vertx.mcp.common;

  exports io.vertx.tests.server;
}

