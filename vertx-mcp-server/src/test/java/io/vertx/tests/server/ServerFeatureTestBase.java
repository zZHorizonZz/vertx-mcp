package io.vertx.tests.server;

import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerFeature;
import org.junit.Before;

/**
 * Base class for testing server features. Extends HttpTransportTestBase and provides simplified server startup with features. The server is started once before all tests, and the
 * feature is cleared before each test.
 */
public abstract class ServerFeatureTestBase<T extends ServerFeature> extends HttpTransportTestBase {

  protected T feature;

  /**
   * Called before all tests to create the feature instance. Subclasses must implement this to provide their specific feature instance.
   *
   * @return the feature instance to use for all tests
   */
  protected abstract T createFeature();

  /**
   * Clears the feature state before each test. This ensures test isolation while reusing the same server instance.
   */
  @Before
  public void setUpFeature(TestContext context) {
    if (feature == null) {
      ModelContextProtocolServer server = ModelContextProtocolServer.create(super.vertx);
      super.startServer(context, server);
    }

    feature = createFeature();

    try {
      super.mcpServer.addServerFeature(feature);
    } catch (IllegalStateException e) {
      // ignore, feature already added
    }
  }
}
