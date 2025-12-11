package io.vertx.mcp.client.transport.http;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.StreamResetException;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.concurrent.InboundMessageQueue;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.mcp.client.*;

public class StreamableHttpClientResponse implements ClientResponse, Handler<Buffer> {

  private final ContextInternal context;
  private final ReadStream<Buffer> stream;
  private final InboundMessageQueue<JsonObject> queue;
  private final ClientRequest request;
  private final HttpClientResponse httpResponse;
  private final MessageDeframer deframer;
  private final Promise<Void> endPromise;

  private Handler<Throwable> exceptionHandler;
  private Handler<JsonObject> messageHandler;
  private Handler<Void> endHandler;
  private JsonObject last;
  private ClientSession session;

  public StreamableHttpClientResponse(
    ContextInternal context,
    HttpClientResponse httpResponse,
    ClientSession session,
    ClientRequest request,
    MessageDeframer deframer
  ) {
    this.context = context;
    this.stream = httpResponse;
    this.httpResponse = httpResponse;
    this.session = session;
    this.request = request;
    this.deframer = deframer;
    this.endPromise = context.promise();
    this.queue = new InboundMessageQueue<>(context.executor(), context.executor(), 8, 16) {
      @Override
      protected void handleResume() {
        stream.resume();
      }

      @Override
      protected void handlePause() {
        stream.pause();
      }

      @Override
      protected void handleMessage(JsonObject msg) {
        StreamableHttpClientResponse.this.handleMessage(msg);
      }
    };
  }

  public void init(ClientSession session, ClientRequest request) {
    long maxMessageSize = 1024 * 1024 * 10;
    deframer.maxMessageSize(maxMessageSize);
    stream.handler(this);
    stream.endHandler(v -> {
      deframer.end();
      deframe();
    });
    stream.exceptionHandler(err -> {
      if (err instanceof StreamResetException) {
        StreamResetException reset = (StreamResetException) err;
      }
      handleException(err);
    });
  }

  @Override
  public ClientSession session() {
    return session;
  }

  @Override
  public ClientRequest request() {
    return request;
  }

  @Override
  public MultiMap headers() {
    return httpResponse.headers();
  }

  @Override
  public StreamableHttpClientResponse pause() {
    queue.pause();
    return this;
  }

  @Override
  public StreamableHttpClientResponse resume() {
    return fetch(Long.MAX_VALUE);
  }

  @Override
  public StreamableHttpClientResponse fetch(long amount) {
    queue.fetch(amount);
    return this;
  }

  @Override
  public StreamableHttpClientResponse exceptionHandler(Handler<Throwable> handler) {
    exceptionHandler = handler;
    return this;
  }

  @Override
  public StreamableHttpClientResponse messageHandler(Handler<JsonObject> handler) {
    messageHandler = handler;
    return this;
  }

  @Override
  public StreamableHttpClientResponse handler(Handler<JsonObject> handler) {
    return messageHandler(handler);
  }

  @Override
  public StreamableHttpClientResponse endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }

  @Override
  public void handle(Buffer chunk) {
    deframer.update(chunk);
    deframe();
  }

  private void deframe() {
    while (true) {
      Object ret = deframer.next();
      if (ret == null) {
        break;
      } else {
        Buffer msg = (Buffer) ret;
        queue.write(msg.toJsonObject());
      }
    }
  }

  public final void tryFail(Throwable err) {
    if (endPromise.tryFail(err)) {
      Handler<Throwable> handler = exceptionHandler;
      if (handler != null) {
        context.dispatch(err, handler);
      }
    }
  }

  private void handleException(Throwable err) {
    tryFail(err);
  }

  private void handleEnd() {
    endPromise.tryComplete();
    Handler<Void> handler = endHandler;
    if (handler != null) {
      context.dispatch(handler);
    }
  }

  private void handleMessage(JsonObject msg) {
    last = msg;
    Handler<JsonObject> handler = messageHandler;
    if (handler != null) {
      context.dispatch(msg, messageHandler);
    }
  }

  @Override
  public Future<JsonObject> last() {
    return end().map(v -> last);
  }

  @Override
  public Future<Void> end() {
    return endPromise.future();
  }
}


