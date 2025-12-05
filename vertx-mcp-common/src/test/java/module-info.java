open module io.vertx.tests.mcp.common {
  requires io.vertx.core;
  requires io.vertx.jsonschema;
  requires io.vertx.testing.unit;

  requires junit;
  requires testcontainers;

  requires io.vertx.mcp.common;

  exports io.vertx.tests.mcp.common;
}

