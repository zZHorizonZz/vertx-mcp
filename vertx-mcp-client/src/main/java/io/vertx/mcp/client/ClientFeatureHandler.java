package io.vertx.mcp.client;

import io.vertx.codegen.annotations.CacheReturn;
import io.vertx.codegen.annotations.VertxGen;

import java.util.function.Function;

/**
 * Represents a handler for client features that extends the {@link Function} interface to process input and output data types.
 * <p>
 * A {@code ClientFeatureHandler} is designed to provide metadata such as its name, which can be used to identify
 * the functionality of a client feature in a system.
 *
 * @param <I> the type of input data processed by the handler
 * @param <O> the type of output data produced by the handler
 */
@VertxGen
public interface ClientFeatureHandler<I, O> extends Function<I, O> {
  /**
   * Retrieves the name of the client feature handler.
   *
   * @return the name of the client feature handler
   */
  @CacheReturn
  String name();
}
