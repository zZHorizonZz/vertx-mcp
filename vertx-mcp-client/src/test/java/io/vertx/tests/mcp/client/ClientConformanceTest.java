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
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Client conformance tests using the MCP conformance framework.
 * <p>
 * The conformance framework runs inside a Docker container that has:
 * - Node.js and the @modelcontextprotocol/conformance package
 * - Java runtime and the compiled MCP client JAR
 * - The conformance framework manages scenario servers and validates client behavior
 * <p>
 * Prerequisites:
 * - Docker must be running
 * - Maven build must have been run to compile the client JAR and dependencies
 * <p>
 * Usage: mvn test -Pmcp-client-conformance -Dtest=ClientConformanceTest
 */
public class ClientConformanceTest extends TestContainerTestBase {

  private ImageFromDockerfile conformanceImage;

  @Override
  public void setUp(TestContext context) {
    super.setUp(context);

    System.out.println("[setup] Building conformance Docker image...");

    // Get project paths
    File resourcesDir = new File("src/test/resources");
    File dockerfile = new File(resourcesDir, "conformance.Dockerfile");
    File wrapperScript = new File(resourcesDir, "conformance-client-wrapper.sh");
    File targetDir = new File("target");

    // Build Docker image with all necessary files
    conformanceImage = new ImageFromDockerfile()
      .withFileFromFile("Dockerfile", dockerfile)
      .withFileFromFile("conformance-client-wrapper.sh", wrapperScript)
      .withFileFromPath("target", targetDir.toPath());

    System.out.println("[setup] Docker image build configuration complete");
  }

  @Test
  public void testInitialize(TestContext context) throws Exception {
    runConformanceScenario(context, "initialize");
  }

  @Test
  public void testToolsCall(TestContext context) throws Exception {
    runConformanceScenario(context, "tools-call");
  }

  /**
   * Runs a conformance test scenario in a Docker container.
   * The container runs the conformance framework which validates the client behavior.
   */
  private void runConformanceScenario(TestContext context, String scenario) throws Exception {
    System.out.println("\n" + "=".repeat(80));
    System.out.println("Running conformance test for scenario: " + scenario);
    System.out.println("=".repeat(80) + "\n");

    ToStringConsumer logConsumer = new ToStringConsumer();

    try (GenericContainer<?> container = new GenericContainer<>(conformanceImage)) {
      container
        .withEnv("SCENARIO", scenario)
        .withLogConsumer(logConsumer)
        .withStartupCheckStrategy(new org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy()
          .withTimeout(Duration.ofMinutes(5)));

      System.out.println("[test] Starting conformance container...");

      Integer exitCode = null;
      try {
        container.start();
        exitCode = 0;
      } catch (Exception e) {
        // Container may exit with non-zero for conformance failures
        System.out.println("[test] Container exited (possibly with non-zero): " + e.getMessage());
      }

      // Get output
      String output = logConsumer.toUtf8String();
      System.out.println("\n[test] Container output:");
      System.out.println(output);

      // Try to get actual exit code if available
      try {
        Integer actualExitCode = container.getCurrentContainerInfo().getState().getExitCode();
        if (actualExitCode != null) {
          exitCode = actualExitCode;
        }
      } catch (Exception e) {
        System.out.println("[test] Could not retrieve exit code: " + e.getMessage());
      }

      System.out.println("[test] Container finished with exit code: " + exitCode);

      // Validate results
      validateConformanceOutput(context, scenario, output, exitCode);

      System.out.println("\n" + "=".repeat(80));
      System.out.println("Scenario '" + scenario + "' completed");
      System.out.println("=".repeat(80) + "\n");
    }
  }

  /**
   * Validates the conformance test output.
   * The conformance framework returns exit code 0 for success, non-zero for failure.
   */
  private void validateConformanceOutput(TestContext context, String scenario, String output, Integer exitCode) {
    System.out.println("\n[validation] Analyzing conformance results:");

    // Count checks in output
    int passedCount = countOccurrences(output, "✓");
    int failedCount = countOccurrences(output, "✗");

    System.out.println("[validation] Passed checks: " + passedCount);
    System.out.println("[validation] Failed checks: " + failedCount);
    System.out.println("[validation] Exit code: " + exitCode);

    // Exit code 0 means all conformance checks passed
    if (exitCode == null || exitCode != 0) {
      context.fail("Conformance test failed for scenario '" + scenario +
                   "' with exit code " + exitCode +
                   " (" + failedCount + " failed checks). See output above.");
    }

    System.out.println("[validation] ✓ All conformance checks passed");
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
