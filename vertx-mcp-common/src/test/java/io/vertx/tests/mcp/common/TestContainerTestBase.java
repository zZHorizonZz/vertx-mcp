/*
 * Copyright (c) 2011-2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.tests.mcp.common;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.time.Duration;
import java.util.List;

/**
 * Base class for tests that use Testcontainers to run external tools.
 * Provides shared logic for container setup and execution.
 */
@RunWith(VertxUnitRunner.class)
public abstract class TestContainerTestBase {

  protected Vertx vertx;

  @Before
  public void setUp(TestContext context) {
    vertx = Vertx.vertx();
  }

  @After
  public void tearDown(TestContext context) {
    vertx.close().onComplete(context.asyncAssertSuccess());
  }

  /**
   * The hostname to use for connecting from containers to the host.
   */
  protected static final String HOST_INTERNAL = "host.testcontainers.internal";

  /**
   * Exposes the test server port to containers. Must be called after the server is started and listening.
   *
   * @param port the port to expose
   */
  protected void exposeHostPort(int port) {
    Testcontainers.exposeHostPorts(port);
  }

  /**
   * Executes a command in a container and returns the output.
   *
   * @param image the Docker image to use
   * @param command the command arguments
   * @return a Future containing the container output
   */
  protected Future<String> executeInContainer(ImageFromDockerfile image, List<String> command) {
    return vertx.executeBlocking(() -> {
      System.out.println("[container] Executing with args: " + command);

      ToStringConsumer logConsumer = new ToStringConsumer();
      try (GenericContainer<?> container = new GenericContainer<>(image)) {
        container
          .withLogConsumer(logConsumer)
          .withCommand(command.toArray(new String[0]))
          .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(30)));

        try {
          container.start();
        } catch (Exception e) {
          // Container may exit with non-zero for expected failures
        }

        String output = logConsumer.toUtf8String();
        System.out.println("[container] Output: " + output);
        return output;
      }
    });
  }

  /**
   * Executes a command in a container with custom timeout and returns the output.
   *
   * @param image the Docker image to use
   * @param command the command arguments
   * @param timeoutSeconds timeout in seconds
   * @return a Future containing the container output
   */
  protected Future<String> executeInContainer(ImageFromDockerfile image, List<String> command, int timeoutSeconds) {
    return vertx.executeBlocking(() -> {
      System.out.println("[container] Executing with args: " + command);

      ToStringConsumer logConsumer = new ToStringConsumer();
      try (GenericContainer<?> container = new GenericContainer<>(image)) {
        container
          .withLogConsumer(logConsumer)
          .withCommand(command.toArray(new String[0]))
          .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(timeoutSeconds)));

        try {
          container.start();
        } catch (Exception e) {
          // Container may exit with non-zero for expected failures
        }

        String output = logConsumer.toUtf8String();
        System.out.println("[container] Output: " + output);
        return output;
      }
    });
  }
}
