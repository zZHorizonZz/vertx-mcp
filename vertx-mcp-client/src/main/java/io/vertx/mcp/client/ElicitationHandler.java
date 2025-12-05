package io.vertx.mcp.client;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.mcp.common.request.ElicitRequest;
import io.vertx.mcp.common.result.ElicitResult;

import java.util.function.Function;

/**
 * Represents a handler responsible for processing elicitation requests. An elicitation handler processes requests for structured user input from the server.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/elicitation">Client Features - Elicitation</a>
 */
@VertxGen
public interface ElicitationHandler extends Function<ElicitRequest, Future<ElicitResult>> {

}
