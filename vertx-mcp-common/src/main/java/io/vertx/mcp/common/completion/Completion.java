package io.vertx.mcp.common.completion;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@DataObject
@JsonGen(publicConverter = false)
public class Completion {

  private int total;
  private boolean hasMore;
  private List<String> values;

  public Completion() {

  }

  public Completion(JsonObject json) {
    CompletionConverter.fromJson(json, this);
  }

  public int getTotal() {
    return total;
  }

  public Completion setTotal(int total) {
    this.total = total;
    return this;
  }

  public boolean isHasMore() {
    return hasMore;
  }

  public Completion setHasMore(boolean hasMore) {
    this.hasMore = hasMore;
    return this;
  }

  public List<String> getValues() {
    return values;
  }

  public Completion setValues(List<String> values) {
    this.values = values;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    CompletionConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
