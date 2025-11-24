package io.vertx.mcp.common.root;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.Meta;

/**
 * Represents a root directory or file that the server can operate on.
 */
@DataObject
@JsonGen(publicConverter = false)
public class Root implements Meta {

  private String uri;
  private String name;
  private JsonObject _meta;

  public Root() {
  }

  public Root(JsonObject json) {
    RootConverter.fromJson(json, this);
  }

  /**
   * Gets the URI identifying the root. This must start with file:// for now. This restriction may be relaxed in future versions of the protocol to allow other URI schemes.
   *
   * @return root URI
   */
  public String getUri() {
    return uri;
  }

  /**
   * Sets the URI identifying the root.
   *
   * @param uri root URI
   * @return this instance for method chaining
   */
  public Root setUri(String uri) {
    this.uri = uri;
    return this;
  }

  /**
   * Gets an optional name for the root. This can be used to provide a human-readable identifier for the root, which may be useful for display purposes or for referencing the root
   * in other parts of the application.
   *
   * @return root name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets an optional name for the root.
   *
   * @param name root name
   * @return this instance for method chaining
   */
  public Root setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  @GenIgnore
  public JsonObject getMeta() {
    return _meta;
  }

  @Override
  @GenIgnore
  public void setMeta(JsonObject meta) {
    this._meta = meta;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    RootConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
