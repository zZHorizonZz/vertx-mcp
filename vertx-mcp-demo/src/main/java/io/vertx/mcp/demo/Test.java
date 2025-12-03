package io.vertx.mcp.demo;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;

public class Test {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    vertx.createHttpClient().request(new RequestOptions().setMethod(HttpMethod.POST).setAbsoluteURI("http://meowfacts.herokuapp.com/"))
      .compose(req -> req.send(new JsonObject().put("foo", "bar").toBuffer()))
      .compose(HttpClientResponse::body)
      .onSuccess(resp -> System.out.println(resp.toString()))
      .onFailure(err -> {
        err.printStackTrace();
        System.err.println("Error: " + err.getMessage());
      });
  }
}
