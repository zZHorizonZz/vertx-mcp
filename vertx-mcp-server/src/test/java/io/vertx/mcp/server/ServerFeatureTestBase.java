package io.vertx.mcp.server;

import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.server.impl.PromptServerFeature;
import io.vertx.mcp.server.impl.ResourceServerFeature;
import io.vertx.mcp.server.impl.ToolServerFeature;
import org.junit.Before;

/**
 * Base class for testing server features.
 * Extends HttpTransportTestBase and provides simplified server startup with features.
 * The server is started once before all tests, and the feature is cleared before each test.
 */
public abstract class ServerFeatureTestBase<T extends ServerFeature> extends HttpTransportTestBase {

  protected T feature;

  /**
   * Called before all tests to create the feature instance.
   * Subclasses must implement this to provide their specific feature instance.
   *
   * @return the feature instance to use for all tests
   */
  protected abstract T createFeature();

  /**
   * Clears the feature state before each test.
   * This ensures test isolation while reusing the same server instance.
   */
  @Before
  public void setUpFeature(TestContext context) {
    // Create feature on first use
    if (feature == null) {
      feature = createFeature();
      ModelContextProtocolServer server = ModelContextProtocolServer.create();
      server.serverFeatures(feature);
      startServer(context, server);
    }

    // Clear feature state before each test for isolation
    clearFeature();
  }

  /**
   * Clears the feature's registered handlers.
   * Override this if using a custom feature type.
   */
  protected void clearFeature() {
    if (feature instanceof ResourceServerFeature) {
      ((ResourceServerFeature) feature).clear();
    } else if (feature instanceof ToolServerFeature) {
      ((ToolServerFeature) feature).clear();
    } else if (feature instanceof PromptServerFeature) {
      ((PromptServerFeature) feature).clear();
    }
  }
}
