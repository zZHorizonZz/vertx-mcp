package io.vertx.mcp.client.transport.http;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.client.*;

public class StreamableHttpClientResponse extends MessageReadStreamBase<StreamableHttpClientResponse> implements ClientResponse, Handler<Buffer> {

  private final ClientRequest request;
  private final HttpClientResponse httpResponse;

  private ClientSession session;

  public StreamableHttpClientResponse(
    ContextInternal context,
    HttpClientResponse httpResponse,
    ClientSession session,
    ClientRequest request,
    MessageDeframer deframer,
    MessageDecoder decoder
  ) {
    super(context, httpResponse, deframer, decoder);
    this.httpResponse = httpResponse;
    this.session = session;
    this.request = request;
  }

  @Override
  public void init(ClientSession session, ClientRequest request) {
    super.init(1024 * 1024 * 10);
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

  protected void handleEnd() {
    //sendRequest.cancelTimeout();
    /*if (status == null) {
      String responseStatus = httpResponse.getTrailer("grpc-status");
      if (responseStatus != null) {
        status = GrpcStatus.valueOf(Integer.parseInt(responseStatus));
      } else {
        status = GrpcStatus.UNKNOWN;
      }
    }*/
    super.handleEnd();
    /*sendRequest.handleStatus(status);
    if (!sendRequest.isTrailersSent()) {
      sendRequest.cancel();
    }*/
  }

  @Override
  public Future<Void> end() {
    return super.end();
  }

  @Override
  public StreamableHttpClientResponse handler(Handler<JsonObject> handler) {
    return messageHandler(handler);
  }
}


