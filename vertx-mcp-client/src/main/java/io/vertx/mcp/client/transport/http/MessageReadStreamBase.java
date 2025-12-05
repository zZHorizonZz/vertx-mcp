package io.vertx.mcp.client.transport.http;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.StreamResetException;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.concurrent.InboundMessageQueue;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.mcp.client.MessageDeframer;
import io.vertx.mcp.client.MessageReadStream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class MessageReadStreamBase<S extends MessageReadStreamBase<S>> implements MessageReadStream, Handler<Buffer> {

  protected final ContextInternal context;
  private final ReadStream<Buffer> stream;
  private final InboundMessageQueue<JsonObject> queue;
  private Handler<Throwable> exceptionHandler;
  private Handler<JsonObject> messageHandler;
  private Handler<Void> endHandler;
  private JsonObject last;
  private final Promise<Void> end;
  private final MessageDeframer deframer;

  protected MessageReadStreamBase(Context context, ReadStream<Buffer> stream, MessageDeframer deframer) {
    ContextInternal ctx = (ContextInternal) context;
    this.context = ctx;
    this.stream = stream;
    this.queue = new InboundMessageQueue<>(ctx.executor(), ctx.executor(), 8, 16) {
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
        /*if (msg == END_SENTINEL) {
          handleEnd();
        } else {*/
        MessageReadStreamBase.this.handleMessage(msg);
        //}
      }
    };
    this.deframer = deframer;
    this.end = ctx.promise();
  }

  public void init(long maxMessageSize) {
    deframer.maxMessageSize(maxMessageSize);
    stream.handler(this);
    stream.endHandler(v -> {
      deframer.end();
      deframe();
      //queue.write(END_SENTINEL);
    });
    stream.exceptionHandler(err -> {
      if (err instanceof StreamResetException) {
        StreamResetException reset = (StreamResetException) err;
        //GrpcError error = mapHttp2ErrorCode(reset.getCode());
        //ws.handleError(error);
      } else {
        handleException(err);
      }
    });
  }

  public final S pause() {
    queue.pause();
    return (S) this;
  }

  public final S resume() {
    return fetch(Long.MAX_VALUE);
  }

  public final S fetch(long amount) {
    queue.fetch(amount);
    return (S) this;
  }

  @Override
  public final S exceptionHandler(Handler<Throwable> handler) {
    exceptionHandler = handler;
    return (S) this;
  }

  @Override
  public final S messageHandler(Handler<JsonObject> handler) {
    messageHandler = handler;
    return (S) this;
  }

  @Override
  public abstract S handler(@Nullable Handler<JsonObject> handler);

  @Override
  public final S endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return (S) this;
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
      }/* else if (ret instanceof MessageSizeOverflowException) {
        MessageSizeOverflowException msoe = (MessageSizeOverflowException) ret;
        Handler<InvalidMessageException> handler = invalidMessageHandler;
        if (handler != null) {
          context.dispatch(msoe, handler);
        }
      }*/ else {
        Buffer msg = (Buffer) ret;
        queue.write(msg.toJsonObject());
      }
    }
  }

  public final void tryFail(Throwable err) {
    if (end.tryFail(err)) {
      Handler<Throwable> handler = exceptionHandler;
      if (handler != null) {
        context.dispatch(err, handler);
      }
    }
  }

  protected final void handleException(Throwable err) {
    tryFail(err);
  }

  protected void handleEnd() {
    end.tryComplete();
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
  public final Future<JsonObject> last() {
    return end().map(v -> last);
  }

  @Override
  public Future<Void> end() {
    return end.future();
  }
}

