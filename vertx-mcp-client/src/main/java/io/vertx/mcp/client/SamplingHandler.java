package io.vertx.mcp.client;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.mcp.common.request.CreateMessageRequest;
import io.vertx.mcp.common.result.CreateMessageResult;

import java.util.function.Function;

/**
 * Represents a handler responsible for processing sampling requests. A sampling handler processes LLM message creation requests from the server.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/sampling">Client Features - Sampling</a>
 */
@VertxGen
public interface SamplingHandler extends Function<CreateMessageRequest, Future<CreateMessageResult>> {

}
