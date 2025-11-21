package io.vertx.mcp.server;

import io.vertx.codegen.annotations.CacheReturn;
import io.vertx.codegen.annotations.VertxGen;

import java.util.function.Function;

/**
 * Represents a handler for server features that extends the {@link Function} interface to process input and output data types.
 * <p>
 * A {@code ServerFeatureHandler} is designed to provide metadata such as its name, title, and description, which can be used to identify and describe the functionality of a server
 * feature in a system.
 *
 * @param <I> the type of input data processed by the handler
 * @param <O> the type of output data produced by the handler
 * @param <F> the feature type this handler produces (e.g., Tool, Prompt, Resource)
 */
@VertxGen
public interface ServerFeatureHandler<I, O, F> extends Function<I, O> {
  /**
   * Retrieves the name of the server feature handler.
   *
   * @return the name of the server feature handler
   */
  @CacheReturn
  String name();

  /**
   * Retrieves the title associated with this server feature handler.
   *
   * @return the title of the server feature handler
   */
  @CacheReturn
  String title();

  /**
   * Provides a description of the server feature or handler.
   *
   * @return the description as a string
   */
  @CacheReturn
  String description();

  /**
   * Converts this handler to its corresponding feature object.
   *
   * @return the feature object (e.g., Tool, Prompt, Resource)
   */
  F toFeature();
}
