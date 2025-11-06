package io.vertx.mcp.server;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import org.junit.Test;

public class SessionManagementTest extends HttpTransportTestBase {

  private static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";

  @Test
  public void testInitializeGeneratesSessionId(TestContext context) {
    ServerOptions options = new ServerOptions()
      .setSessionsEnabled(true);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    JsonRequest initRequest = JsonRequest.createRequest("initialize", new JsonObject(), 1);

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(initRequest.toJson().toBuffer())
          .compose(resp -> {
            // Verify session ID header is present
            String sessionId = resp.getHeader(MCP_SESSION_ID_HEADER);
            context.assertNotNull(sessionId, "Session ID should be generated");
            context.assertFalse(sessionId.isEmpty(), "Session ID should not be empty");

            return resp.body().map(body -> {
              JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
              context.assertNull(response.getError(), "Initialize should succeed");
              context.assertNotNull(response.getResult(), "Should have result");
              return sessionId;
            });
          });
      })
      .onComplete(context.asyncAssertSuccess(sessionId -> {
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testInitializeWithoutSessionsDoesNotGenerateSessionId(TestContext context) {
    ServerOptions options = new ServerOptions()
      .setSessionsEnabled(false);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    JsonRequest initRequest = JsonRequest.createRequest("initialize", new JsonObject(), 1);

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(initRequest.toJson().toBuffer())
          .compose(resp -> {
            // Verify no session ID header
            String sessionId = resp.getHeader(MCP_SESSION_ID_HEADER);
            context.assertNull(sessionId, "Session ID should not be generated when sessions disabled");

            return resp.body().map(body -> {
              JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
              context.assertNull(response.getError(), "Initialize should succeed");
              return null;
            });
          });
      })
      .onComplete(context.asyncAssertSuccess(v -> {
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testSubsequentRequestWithSessionId(TestContext context) {
    ServerOptions options = new ServerOptions()
      .setSessionsEnabled(true);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    // First, initialize to get a session ID
    JsonRequest initRequest = JsonRequest.createRequest("initialize", new JsonObject(), 1);

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(initRequest.toJson().toBuffer())
          .compose(resp -> resp.body().map(body -> resp.getHeader(MCP_SESSION_ID_HEADER)));
      })
      .compose(sessionId -> {
        // Now make another request with the session ID
        JsonRequest pingRequest = JsonRequest.createRequest("ping", new JsonObject(), 2);

        return client.request(HttpMethod.POST, port, "localhost", "/")
          .compose(req -> {
            req.putHeader("Content-Type", "application/json");
            req.putHeader(MCP_SESSION_ID_HEADER, sessionId);
            return req.send(pingRequest.toJson().toBuffer())
              .compose(resp -> {
                context.assertEquals(200, resp.statusCode());
                return resp.body();
              });
          });
      })
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
        context.assertNull(response.getError(), "Ping should succeed");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testInvalidSessionIdReturns400(TestContext context) {
    ServerOptions options = new ServerOptions()
      .setSessionsEnabled(true);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    JsonRequest pingRequest = JsonRequest.createRequest("ping", new JsonObject(), 1);

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        req.putHeader(MCP_SESSION_ID_HEADER, "invalid-session-id");
        return req.send(pingRequest.toJson().toBuffer());
      })
      .onComplete(context.asyncAssertSuccess(resp -> {
        context.assertEquals(400, resp.statusCode(), "Invalid session should return 400");
        async.complete();
      }));

    async.awaitSuccess(10_000);
  }

  @Test
  public void testRequestWithoutSessionIdWorks(TestContext context) {
    ServerOptions options = new ServerOptions()
      .setSessionsEnabled(true);

    ModelContextProtocolServer server = ModelContextProtocolServer.create(options);
    startServer(context, server);

    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    Async async = context.async();

    // Ping without session ID should still work
    JsonRequest pingRequest = JsonRequest.createRequest("ping", new JsonObject(), 1);

    client.request(HttpMethod.POST, port, "localhost", "/")
      .compose(req -> {
        req.putHeader("Content-Type", "application/json");
        return req.send(pingRequest.toJson().toBuffer())
          .compose(resp -> {
            context.assertEquals(200, resp.statusCode());
            return resp.body();
          });
      })
      .onComplete(context.asyncAssertSuccess(body -> {
        JsonResponse response = JsonResponse.fromJson(body.toJsonObject());
        context.assertNull(response.getError(), "Ping should succeed without session");
        async.complete();
      }));
  }
}
