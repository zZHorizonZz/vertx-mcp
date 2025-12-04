package io.vertx.mcp.client.transport.http;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Timer;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.StreamResetException;
import io.vertx.core.internal.PromiseInternal;
import io.vertx.mcp.client.ClientRequest;
import io.vertx.mcp.client.ClientResponse;
import io.vertx.mcp.client.ClientSession;
import io.vertx.mcp.common.rpc.JsonRequest;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class StreamableHttpClientRequest extends MessageWriteStreamBase<StreamableHttpClientRequest> implements ClientRequest {

  private final HttpClientRequest httpRequest;
  private final boolean scheduleDeadline;
  private Future<ClientResponse> response;
  private long timeout;
  private TimeUnit timeoutUnit;
  private String timeoutHeader;
  private Timer deadline;

  private final ClientSession session;

  public StreamableHttpClientRequest(HttpClientRequest httpRequest, long maxMessageSize, boolean scheduleDeadline, ClientSession session) {
    super(((PromiseInternal<?>) httpRequest.response()).context(), httpRequest);
    this.httpRequest = httpRequest;
    this.session = session;
    this.scheduleDeadline = scheduleDeadline;
    this.response = httpRequest
      .response()
      .compose(httpResponse -> {
        String contentType = httpResponse.getHeader(HttpHeaders.CONTENT_TYPE);

        if (httpResponse.statusCode() != 200 && httpResponse.statusCode() != 202) {
          return Future.failedFuture("Invalid HTTP response status code: " + httpResponse.statusCode() + " " + httpResponse.statusMessage());
        }

        if (contentType == null || (!contentType.contains("application/json") && !contentType.contains("text/event-stream"))) {
          return Future.failedFuture("Invalid HTTP response content type: " + contentType);
        }

        StreamableHttpClientResponse mcpResponse = new StreamableHttpClientResponse(
          context,
          httpResponse,
          session,
          this,
          contentType.contains("text/event-stream") ? new EventMessageDeframer() : new JsonMessageDeframer(),
          new JsonMessageDecoder()
        );

        mcpResponse.init(session, this);

        return Future.succeededFuture(mcpResponse);
      }, err -> {
        if (err instanceof StreamResetException) {
          return Future.failedFuture("Stream was reset: " + err.getMessage());
        }
        return Future.failedFuture(err);
      });
  }

  @Override
  public String path() {
    return httpRequest.path();
  }

  @Override
  public ClientSession session() {
    return session;
  }

  @Override
  public Future<Void> send(JsonRequest request) {
    return this.writeMessage(request.toJson());
  }

  @Override
  public Future<Void> end(JsonRequest request) {
    return this.endMessage(request.toJson());
  }

  @Override
  protected Future<Void> sendHead() {
    return httpRequest.sendHead();
  }

  @Override
  protected void setHeaders(MultiMap headers) {
    if (headers != null) {
      MultiMap requestHeaders = httpRequest.headers();
      for (Map.Entry<String, String> header : headers) {
        requestHeaders.add(header.getKey(), header.getValue());
      }
    }

    httpRequest.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    httpRequest.putHeader(HttpHeaders.ACCEPT, "application/json, text/event-stream");

    // Add session ID if available
    if (session != null) {
      httpRequest.putHeader(StreamableHttpClientTransport.MCP_SESSION_ID_HEADER, session.id());
    }

    httpRequest.putHeader(
      StreamableHttpClientTransport.MCP_PROTOCOL_VERSION_HEADER,
      StreamableHttpClientTransport.DEFAULT_PROTOCOL_VERSION
    );
    /*ServiceName serviceName = this.serviceName;
    String methodName = this.methodName;
    if (serviceName == null) {
      throw new IllegalStateException();
    }
    if (methodName == null) {
      throw new IllegalStateException();
    }
    if (headers != null) {
      MultiMap requestHeaders = httpRequest.headers();
      for (Map.Entry<String, String> header : headers) {
        requestHeaders.add(header.getKey(), header.getValue());
      }
    }
    if (timeout > 0L) {
      httpRequest.putHeader(GrpcHeaderNames.GRPC_TIMEOUT, timeoutHeader);
    }
    String uri = serviceName.pathOf(methodName);
    httpRequest.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
    if (encoding != null) {
      httpRequest.putHeader(GrpcHeaderNames.GRPC_ENCODING, encoding);
    }
    httpRequest.putHeader(GrpcHeaderNames.GRPC_ACCEPT_ENCODING, "gzip");
    httpRequest.putHeader(HttpHeaderNames.TE, "trailers");
    httpRequest.setChunked(true);
    httpRequest.setURI(uri);
    if (scheduleDeadline && timeout > 0L) {
      Timer timer = context.timer(timeout, timeoutUnit);
      deadline = timer;
      timer.onSuccess(v -> {
        cancel();
      });
    }*/
  }

  @Override
  protected void setTrailers(MultiMap trailers) {
  }

  @Override
  protected Future<Void> sendMessage(Buffer message) {
    httpRequest.putHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(message.length()));
    return httpRequest.write(message);
  }

  @Override
  protected Future<Void> sendEnd() {
    return httpRequest.end();
  }

  void cancelTimeout() {
    Timer timer = deadline;
    if (timer != null && timer.cancel()) {
      deadline = null;
    }
  }

  @Override
  public Future<ClientResponse> response() {
    return response;
  }

  @Override
  protected boolean sendCancel() {
    httpRequest
      .reset();
    //.onSuccess(v -> handleError(GrpcError.CANCELLED));
    return true;
  }
}

