package io.vertx.mcp.common.capabilities;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

/**
 * Capabilities a client may support. Known capabilities are defined here, in this schema, but this is not a closed set: any client can define its own, additional capabilities.
 */
@DataObject
@JsonGen(publicConverter = false)
public class ClientCapabilities {

  private JsonObject experimental;
  private RootsCapability roots;
  private JsonObject sampling;
  private JsonObject elicitation;

  public ClientCapabilities() {
  }

  public ClientCapabilities(JsonObject json) {
    ClientCapabilitiesConverter.fromJson(json, this);
  }

  /**
   * Gets the experimental, non-standard capabilities that the client supports.
   *
   * @return experimental capabilities
   */
  public JsonObject getExperimental() {
    return experimental;
  }

  /**
   * Sets the experimental, non-standard capabilities that the client supports.
   *
   * @param experimental experimental capabilities
   * @return this instance for method chaining
   */
  public ClientCapabilities setExperimental(JsonObject experimental) {
    this.experimental = experimental;
    return this;
  }

  /**
   * Gets the roots capability if the client supports listing roots.
   *
   * @return roots capability
   */
  public RootsCapability getRoots() {
    return roots;
  }

  /**
   * Sets the roots capability if the client supports listing roots.
   *
   * @param roots roots capability
   * @return this instance for method chaining
   */
  public ClientCapabilities setRoots(RootsCapability roots) {
    this.roots = roots;
    return this;
  }

  /**
   * Gets the sampling capability if the client supports sampling from an LLM.
   *
   * @return sampling capability
   */
  public JsonObject getSampling() {
    return sampling;
  }

  /**
   * Sets the sampling capability if the client supports sampling from an LLM.
   *
   * @param sampling sampling capability
   * @return this instance for method chaining
   */
  public ClientCapabilities setSampling(JsonObject sampling) {
    this.sampling = sampling;
    return this;
  }

  /**
   * Gets the elicitation capability if the client supports elicitation from the server.
   *
   * @return elicitation capability
   */
  public JsonObject getElicitation() {
    return elicitation;
  }

  /**
   * Sets the elicitation capability if the client supports elicitation from the server.
   *
   * @param elicitation elicitation capability
   * @return this instance for method chaining
   */
  public ClientCapabilities setElicitation(JsonObject elicitation) {
    this.elicitation = elicitation;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ClientCapabilitiesConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
