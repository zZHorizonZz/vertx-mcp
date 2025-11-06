package io.vertx.mcp.server;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.mcp.server.transport.http.HttpServerTransport;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public abstract class HttpTransportTestBase {

  protected Vertx vertx;
  protected HttpServer server;
  protected int port = 8080;
  protected ModelContextProtocolServer mcpServer;

  @Before
  public void setUp(TestContext context) {
    vertx = Vertx.vertx();
  }

  @After
  public void tearDown(TestContext context) {
    if (server != null) {
      server.close().onComplete(context.asyncAssertSuccess());
    }
    vertx.close().onComplete(context.asyncAssertSuccess());
  }

  protected void startServer(TestContext context, ModelContextProtocolServer mcpServer) {
    startServer(context, new HttpServerOptions().setPort(port).setHost("localhost"), mcpServer);
  }

  protected void startServer(TestContext context, HttpServerOptions options, ModelContextProtocolServer mcpServer) {
    this.mcpServer = mcpServer;
    HttpServerTransport transport = new HttpServerTransport(vertx, mcpServer);

    server = vertx.createHttpServer(options);
    server.requestHandler(transport);
    server.listen().onComplete(context.asyncAssertSuccess(s -> {
      port = s.actualPort();
    }));
  }
}
