package io.vertx.mcp.it;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.mcp.client.ClientOptions;
import io.vertx.mcp.client.ClientSession;
import io.vertx.mcp.client.ClientTransport;
import io.vertx.mcp.client.ModelContextProtocolClient;
import io.vertx.mcp.client.transport.http.StreamableHttpClientTransport;
import io.vertx.mcp.common.capabilities.ClientCapabilities;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.transport.http.StreamableHttpServerTransport;
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(VertxUnitRunner.class)
public abstract class HttpTransportTestBase {

  protected Vertx vertx;
  protected int port = 8080;

  protected HttpServer httpServer;
  protected HttpClient httpClient;

  protected ModelContextProtocolServer server;
  protected ModelContextProtocolClient client;

  @Before
  public void setUp(TestContext context) {
    vertx = Vertx.vertx();
  }

  @After
  public void tearDown(TestContext context) {
    if (httpServer != null) {
      httpServer.close().onComplete(context.asyncAssertSuccess());
    }
    vertx.close().onComplete(context.asyncAssertSuccess());
  }

  protected void startServer(TestContext context, ModelContextProtocolServer mcpServer) {
    startServer(context, new HttpServerOptions().setPort(port).setHost("localhost"), mcpServer);
  }

  protected void startServer(TestContext context, HttpServerOptions options, ModelContextProtocolServer mcpServer) {
    this.server = mcpServer;
    StreamableHttpServerTransport transport = new StreamableHttpServerTransport(vertx, mcpServer);

    httpServer = vertx.createHttpServer(options);
    // Add CORS handling before passing to transport
    httpServer.requestHandler(req -> {
      req.response()
        .putHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600")
        .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
        .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, DELETE, OPTIONS")
        .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, String.join(",", StreamableHttpServerTransport.ACCEPTED_HEADERS))
        .putHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, String.join(",", StreamableHttpServerTransport.ACCEPTED_HEADERS));

      if (req.method() == HttpMethod.OPTIONS) {
        req.response().setStatusCode(200).end();
        return;
      }
      transport.handle(req);
    });

    try {
      httpServer.listen().onComplete(context.asyncAssertSuccess(s -> port = s.actualPort())).await(20, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      AssertionFailedError afe = new AssertionFailedError();
      afe.initCause(e);
      throw afe;
    }
  }

  protected ModelContextProtocolClient getClient() {
    if (client == null) {
      // Configure client options
      ClientOptions clientOptions = new ClientOptions()
        .setClientName("mcp-client-demo")
        .setClientVersion("1.0.0")
        .setStreamingEnabled(true);

      ClientTransport transport = new StreamableHttpClientTransport(vertx, "http://localhost:8080/mcp", clientOptions);
      client = ModelContextProtocolClient.create(vertx, transport, clientOptions);
    }

    return client;
  }

  protected Future<ClientSession> createSession() {
    return getClient().connect(new ClientCapabilities());
  }
}
