package io.vertx.tests.mcp.server;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.mcp.common.request.Request;
import io.vertx.mcp.common.rpc.JsonRequest;
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
    StreamableHttpServerTransport transport = new StreamableHttpServerTransport(vertx, mcpServer);

    server = vertx.createHttpServer(options);
    // Add CORS handling before passing to transport
    server.requestHandler(req -> {
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
      server.listen().onComplete(context.asyncAssertSuccess(s -> port = s.actualPort())).await(20, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      AssertionFailedError afe = new AssertionFailedError();
      afe.initCause(e);
      throw afe;
    }
  }

  protected Future<HttpClientResponse> sendRequest(HttpMethod method, Request request) {
    return sendRequest(method, request.toRequest(1));
  }

  protected Future<HttpClientResponse> sendRequest(HttpMethod method, JsonRequest request) {
    return sendRequest(method, request.toJson().toBuffer());
  }

  protected Future<HttpClientResponse> sendRequest(HttpMethod method, Buffer body) {
    return sendRequest(method, body, null);
  }

  protected Future<HttpClientResponse> sendRequest(HttpMethod method, Request request, String session) {
    return sendRequest(method, request.toRequest(1), session);
  }

  protected Future<HttpClientResponse> sendRequest(HttpMethod method, JsonRequest request, String session) {
    return sendRequest(method, request.toJson().toBuffer(), session);
  }

  protected Future<HttpClientResponse> sendRequest(HttpMethod method, Buffer body, String session) {
    HttpClient client = vertx.createHttpClient(new HttpClientOptions());

    return client.request(method, port, "localhost", "/mcp").compose(req -> {
      req.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
      req.putHeader(HttpHeaders.ACCEPT, "application/json, text/event-stream");
      if (session != null) {
        req.putHeader(StreamableHttpServerTransport.MCP_SESSION_ID_HEADER, session);
      }
      return req.send(body);
    });
  }

  protected Future<Buffer> sendStreamingRequest(HttpMethod method, Buffer body, String session) {
    HttpClient client = vertx.createHttpClient(new HttpClientOptions());

    return client.request(method, port, "localhost", "/mcp").compose(req -> {
      req.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
      req.putHeader(HttpHeaders.ACCEPT, "application/json, text/event-stream");
      if (session != null) {
        req.putHeader(StreamableHttpServerTransport.MCP_SESSION_ID_HEADER, session);
      }
      return req.send(body).compose(resp -> {
        // For streaming responses, we need to read the SSE events
        Promise<Buffer> promise = Promise.promise();
        Buffer result = Buffer.buffer();

        resp.handler(chunk -> {
          result.appendBuffer(chunk);
          // Check if we got a complete SSE message (ends with \n\n)
          String data = result.toString();
          if (data.contains("data: ")) {
            // Extract JSON from SSE format: "data: {...}\n\n"
            int dataStart = data.indexOf("data: ") + 6;
            int dataEnd = data.indexOf("\n\n", dataStart);
            if (dataEnd > dataStart) {
              String json = data.substring(dataStart, dataEnd).trim();
              promise.complete(Buffer.buffer(json));
            }
          }
        });
        resp.endHandler(v -> {
          if (!promise.future().isComplete()) {
            promise.complete(result);
          }
        });
        resp.exceptionHandler(err -> {
          if (!promise.future().isComplete()) {
            promise.fail(err);
          }
        });

        return promise.future();
      });
    });
  }
}
