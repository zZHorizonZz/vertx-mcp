package io.vertx.mcp.client.transport.http;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.StreamResetException;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;
import io.vertx.mcp.client.MessageWriteStream;

public abstract class MessageWriteStreamBase<S extends MessageWriteStreamBase<S>> implements MessageWriteStream {

  protected final ContextInternal context;
  private final WriteStream<Buffer> writeStream;

  protected String mediaType;
  protected String encoding;
  private boolean headersSent;
  private boolean trailersSent;
  private boolean cancelled;

  private MultiMap headers;
  private MultiMap trailers;

  private Handler<Throwable> exceptionHandler;

  public MessageWriteStreamBase(ContextInternal context, WriteStream<Buffer> writeStream) {
    this.context = context;
    this.writeStream = writeStream;
  }

  public void init() {
    writeStream.exceptionHandler(err -> {
      if (err instanceof StreamResetException) {
        /*StreamResetException reset = (StreamResetException) err;
        GrpcError error = mapHttp2ErrorCode(reset.getCode());
        handleError(error);*/
      }
      handleException(err);
    });
  }

  /*public S errorHandler(Handler<GrpcError> handler) {
    this.errorHandler = handler;
    return (S) this;
  }

  public void handleError(GrpcError error) {
    if (this.error == null) {
      cancelled |= error == GrpcError.CANCELLED;
      this.error = error;
      Handler<GrpcError> handler = errorHandler;
      if (handler != null) {
        handler.handle(error);
      }
    }
  }*/

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
  public final S drainHandler(Handler<Void> handler) {
    writeStream.drainHandler(handler);
    return (S) this;
  }

  @Override
  public final S exceptionHandler(Handler<Throwable> handler) {
    exceptionHandler = handler;
    return (S) this;
  }

  @Override
  public S setWriteQueueMaxSize(int maxSize) {
    writeStream.setWriteQueueMaxSize(maxSize);
    return (S) this;
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

  protected abstract void setHeaders(MultiMap headers);

  protected abstract void setTrailers(MultiMap trailers);

  protected abstract Future<Void> sendMessage(Buffer message);

  protected abstract Future<Void> sendEnd();

  protected abstract Future<Void> sendHead();

  protected abstract boolean sendCancel();

  public final Future<Void> writeHead() {
    return writeMessage(null, false);
  }

  protected Future<Void> writeMessage(JsonObject message, boolean end) {
    /*if (error != null) {
      throw new IllegalStateException("The stream is failed: " + error);
    }*/
    if (trailersSent) {
      throw new IllegalStateException("The stream has been closed");
    }

    if (!headersSent) {
      headersSent = true;
      //String contentType = contentType(format);
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
}

