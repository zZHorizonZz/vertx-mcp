package io.vertx.mcp.client.transport.http;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Timer;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.StreamResetException;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.PromiseInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import io.vertx.mcp.client.ClientRequest;
import io.vertx.mcp.client.ClientResponse;
import io.vertx.mcp.client.ClientSession;
import io.vertx.mcp.client.MessageWriteStream;
import io.vertx.mcp.common.rpc.JsonRequest;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class StreamableHttpClientRequest implements MessageWriteStream, ClientRequest {

  private final ContextInternal context;
  private final WriteStream<Buffer> writeStream;
  private final HttpClientRequest httpRequest;
  private final boolean scheduleDeadline;
  private final ClientSession session;

  private Future<ClientResponse> response;
  private long timeout;
  private TimeUnit timeoutUnit;
  private String timeoutHeader;
  private Timer deadline;

  private String mediaType;
  private String encoding;
  private boolean headersSent;
  private boolean trailersSent;
  private boolean cancelled;

  private MultiMap headers;
  private MultiMap trailers;

  private Handler<Throwable> exceptionHandler;

  public StreamableHttpClientRequest(HttpClientRequest httpRequest, long maxMessageSize, boolean scheduleDeadline, ClientSession session) {
    this.context = (ContextInternal) ((PromiseInternal<?>) httpRequest.response()).context();
    this.writeStream = httpRequest;
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
          contentType.contains("text/event-stream") ? new EventMessageDeframer() : new JsonMessageDeframer()
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

  public void init() {
    writeStream.exceptionHandler(err -> {
      if (err instanceof StreamResetException) {
      }
      handleException(err);
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

  private void handleException(Throwable err) {
    Handler<Throwable> handler = exceptionHandler;
    if (handler != null) {
      handler.handle(err);
    }
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void cancel() {
    if (!cancelled) {
      cancelled = sendCancel();
    }
  }

  public final ContextInternal context() {
    return context;
  }

  public boolean isHeadersSent() {
    return headersSent;
  }

  public boolean isTrailersSent() {
    return trailersSent;
  }

  @Override
  public final MultiMap headers() {
    if (headersSent) {
      throw new IllegalStateException("Headers already sent");
    }
    if (headers == null) {
      headers = MultiMap.caseInsensitiveMultiMap();
    }
    return headers;
  }

  public final MultiMap trailers() {
    if (trailersSent) {
      throw new IllegalStateException("Trailers already sent");
    }
    if (trailers == null) {
      trailers = MultiMap.caseInsensitiveMultiMap();
    }
    return trailers;
  }

  @Override
  public final boolean writeQueueFull() {
    return writeStream.writeQueueFull();
  }

  @Override
  public final StreamableHttpClientRequest drainHandler(Handler<Void> handler) {
    writeStream.drainHandler(handler);
    return this;
  }

  @Override
  public final StreamableHttpClientRequest exceptionHandler(Handler<Throwable> handler) {
    exceptionHandler = handler;
    return this;
  }

  @Override
  public StreamableHttpClientRequest setWriteQueueMaxSize(int maxSize) {
    writeStream.setWriteQueueMaxSize(maxSize);
    return this;
  }

  @Override
  public final Future<Void> write(JsonObject message) {
    return writeMessage(message);
  }

  @Override
  public final Future<Void> end(JsonObject message) {
    return endMessage(message);
  }

  @Override
  public final Future<Void> writeMessage(JsonObject data) {
    return writeMessage(data, false);
  }

  @Override
  public final Future<Void> endMessage(JsonObject message) {
    return writeMessage(message, true);
  }

  public final Future<Void> end() {
    return writeMessage(null, true);
  }

  private void setHeaders(MultiMap headers) {
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

  private void setTrailers(MultiMap trailers) {
  }

  private Future<Void> sendMessage(Buffer message) {
    httpRequest.putHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(message.length()));
    return httpRequest.write(message);
  }

  Future<Void> sendEnd() {
    return httpRequest.end();
  }

  private Future<Void> sendHead() {
    return httpRequest.sendHead();
  }

  private boolean sendCancel() {
    httpRequest
      .reset();
    return true;
  }

  public final Future<Void> writeHead() {
    return writeMessage(null, false);
  }

  private Future<Void> writeMessage(JsonObject message, boolean end) {
    if (trailersSent) {
      throw new IllegalStateException("The stream has been closed");
    }

    if (!headersSent) {
      headersSent = true;
      setHeaders(headers);
    }
    if (end) {
      trailersSent = true;
      if (message != null) {
        sendMessage(message.toBuffer());
      }
      setTrailers(trailers);
      return sendEnd();
    } else {
      if (message != null) {
        return sendMessage(message.toBuffer());
      } else {
        return sendHead();
      }
    }
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
}

