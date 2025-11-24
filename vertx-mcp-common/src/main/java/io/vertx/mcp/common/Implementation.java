package io.vertx.mcp.common;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

/**
 * Describes the name and version of an MCP implementation, with an optional title for UI representation.
 */
@DataObject
@JsonGen(publicConverter = false)
public class Implementation {

  private String name;
  private String version;
  private String title;

  public Implementation() {
  }

  public Implementation(JsonObject json) {
    ImplementationConverter.fromJson(json, this);
  }

  /**
   * Gets the name of the implementation. Intended for programmatic or logical use, but used as a display name in past specs or fallback (if title isn't present).
   *
   * @return implementation name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the implementation.
   *
   * @param name implementation name
   * @return this instance for method chaining
   */
  public Implementation setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Gets the version of the implementation.
   *
   * @return implementation version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Sets the version of the implementation.
   *
   * @param version implementation version
   * @return this instance for method chaining
   */
  public Implementation setVersion(String version) {
    this.version = version;
    return this;
  }

  /**
   * Gets the title of the implementation. Intended for UI and end-user contexts — optimized to be human-readable and easily understood, even by those unfamiliar with
   * domain-specific terminology.
   *
   * @return implementation title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title of the implementation.
   *
   * @param title implementation title
   * @return this instance for method chaining
   */
  public Implementation setTitle(String title) {
    this.title = title;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ImplementationConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
