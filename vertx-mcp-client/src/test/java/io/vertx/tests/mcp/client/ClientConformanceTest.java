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
package io.vertx.tests.mcp.client;

import io.vertx.ext.unit.TestContext;
import io.vertx.tests.mcp.common.TestContainerTestBase;
import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.time.Duration;

/**
 * Client conformance tests using the MCP conformance framework.
 * <p>
 * Runs the @modelcontextprotocol/conformance framework in Docker to validate
 * the vertx-mcp-client implementation against the MCP specification.
 * <p>
 * Prerequisites: Docker must be running.
 * <p>
 * Usage: mvn test -Pmcp-conformance -Dtest=ClientConformanceTest
 */
public class ClientConformanceTest extends TestContainerTestBase {

  private ImageFromDockerfile conformanceImage;

  @Override
  public void setUp(TestContext context) {
    super.setUp(context);

    try {
      File moduleDir = new File(".").getCanonicalFile();
      File projectRoot = findProjectRoot(moduleDir);
      File dockerfile = new File(moduleDir, "src/test/resources/conformance.Dockerfile");

      conformanceImage = new ImageFromDockerfile()
        .withFileFromPath(".", projectRoot.toPath())
        .withFileFromFile("Dockerfile", dockerfile);
    } catch (Exception e) {
      context.fail("Failed to set up conformance test: " + e.getMessage());
    }
  }

  @Test
  @Ignore
  public void testInitialize(TestContext context) throws Exception {
    runScenario(context, "initialize");
  }

  @Test
  @Ignore
  public void testToolsCall(TestContext context) throws Exception {
    runScenario(context, "tools_call");
  }

  private void runScenario(TestContext context, String scenario) {
    ToStringConsumer logConsumer = new ToStringConsumer();

    try (GenericContainer<?> container = new GenericContainer<>(conformanceImage)) {
      container
        .withEnv("SCENARIO", scenario)
        .withLogConsumer(logConsumer)
        .withStartupCheckStrategy(new OneShotStartupCheckStrategy()
          .withTimeout(Duration.ofMinutes(5)));

      Integer exitCode = null;
      try {
        container.start();
        exitCode = 0;
      } catch (Exception e) {
        // Container may exit with non-zero for conformance failures
      }

      String output = logConsumer.toUtf8String();
      System.out.println(output);

      try {
        Integer actualExitCode = container.getCurrentContainerInfo().getState().getExitCode();
        if (actualExitCode != null) {
          exitCode = actualExitCode;
        }
      } catch (Exception ignored) {
      }

      validateResults(context, scenario, output, exitCode);
    }
  }

  private void validateResults(TestContext context, String scenario, String output, Integer exitCode) {
    int failedCount = countOccurrences(output, "\"status\": \"FAILURE\"") +
                      countOccurrences(output, "\"status\":\"FAILURE\"");

    if (exitCode == null || exitCode != 0) {
      context.fail("Conformance test '" + scenario + "' failed with exit code " + exitCode +
                   " (" + failedCount + " failed checks)");
    }
  }

  private File findProjectRoot(File moduleDir) {
    File projectRoot = moduleDir.getParentFile();
    File clientModule = new File(projectRoot, "vertx-mcp-client");
    if (!clientModule.exists()) {
      clientModule = new File(moduleDir, "vertx-mcp-client");
      if (clientModule.exists()) {
        return moduleDir;
      }
    }
    return projectRoot;
  }

  private int countOccurrences(String str, String sub) {
    int count = 0;
    int idx = 0;
    while ((idx = str.indexOf(sub, idx)) != -1) {
      count++;
      idx += sub.length();
    }
    return count;
  }
}
