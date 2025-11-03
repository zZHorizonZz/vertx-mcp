package io.vertx.mcp.common.result;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.resources.ResourceTemplate;

import java.util.List;

@DataObject
@JsonGen(publicConverter = false)
public class ListResourceTemplatesResult extends Result {

  private List<ResourceTemplate> resourceTemplates;
  private String nextCursor;

  public ListResourceTemplatesResult() {
    super(null);
  }

  public ListResourceTemplatesResult(JsonObject json) {
    this();
    ListResourceTemplatesResultConverter.fromJson(json, this);
  }

  public List<ResourceTemplate> getResourceTemplates() {
    return resourceTemplates;
  }

  public ListResourceTemplatesResult setResourceTemplates(List<ResourceTemplate> resourceTemplates) {
    this.resourceTemplates = resourceTemplates;
    return this;
  }

  public String getNextCursor() {
    return nextCursor;
  }

  public ListResourceTemplatesResult setNextCursor(String nextCursor) {
    this.nextCursor = nextCursor;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ListResourceTemplatesResultConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
