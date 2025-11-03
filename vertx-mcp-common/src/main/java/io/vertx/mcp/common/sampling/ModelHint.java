package io.vertx.mcp.common.sampling;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

/**
 * Hints to use for model selection.
 * Keys not declared here are currently left unspecified by the spec and are up
 * to the client to interpret.
 */
@DataObject
@JsonGen(publicConverter = false)
public class ModelHint {

  private String name;

  public ModelHint() {
  }

  public ModelHint(JsonObject json) {
    ModelHintConverter.fromJson(json, this);
  }

  /**
   * Gets a hint for a model name.
   * The client SHOULD treat this as a substring of a model name; for example:
   * - "claude-3-5-sonnet" should match "claude-3-5-sonnet-20241022"
   * - "sonnet" should match "claude-3-5-sonnet-20241022", "claude-3-sonnet-20240229", etc.
   * - "claude" should match any Claude model
   * <p>
   * The client MAY also map the string to a different provider's model name or a different model family,
   * as long as it fills a similar niche; for example:
   * - "gemini-1.5-flash" could match "claude-3-haiku-20240307"
   *
   * @return model name hint
   */
  public String getName() {
    return name;
  }

  /**
   * Sets a hint for a model name.
   *
   * @param name model name hint
   * @return this instance for method chaining
   */
  public ModelHint setName(String name) {
    this.name = name;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ModelHintConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
