package io.vertx.mcp.common.sampling;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * The server's preferences for model selection, requested of the client during sampling.
 * <p>
 * Because LLMs can vary along multiple dimensions, choosing the "best" model is
 * rarely straightforward. Different models excel in different areas—some are
 * faster but less capable, others are more capable but more expensive, and so
 * on. This interface allows servers to express their priorities across multiple
 * dimensions to help clients make an appropriate selection for their use case.
 * <p>
 * These preferences are always advisory. The client MAY ignore them. It is also
 * up to the client to decide how to interpret these preferences and how to
 * balance them against other considerations.
 */
@DataObject
@JsonGen(publicConverter = false)
public class ModelPreferences {

  private List<ModelHint> hints;
  private Double costPriority;
  private Double speedPriority;
  private Double intelligencePriority;

  public ModelPreferences() {
  }

  public ModelPreferences(JsonObject json) {
    ModelPreferencesConverter.fromJson(json, this);
  }

  /**
   * Gets optional hints to use for model selection.
   * If multiple hints are specified, the client MUST evaluate them in order
   * (such that the first match is taken).
   * The client SHOULD prioritize these hints over the numeric priorities, but
   * MAY still use the priorities to select from ambiguous matches.
   *
   * @return list of model hints
   */
  public List<ModelHint> getHints() {
    return hints;
  }

  /**
   * Sets optional hints to use for model selection.
   *
   * @param hints list of model hints
   * @return this instance for method chaining
   */
  public ModelPreferences setHints(List<ModelHint> hints) {
    this.hints = hints;
    return this;
  }

  /**
   * Gets how much to prioritize cost when selecting a model. A value of 0 means cost
   * is not important, while a value of 1 means cost is the most important factor.
   *
   * @return cost priority between 0.0 and 1.0
   */
  public Double getCostPriority() {
    return costPriority;
  }

  /**
   * Sets how much to prioritize cost when selecting a model.
   * Must be a value between 0.0 and 1.0.
   *
   * @param costPriority cost priority between 0.0 and 1.0
   * @return this instance for method chaining
   */
  public ModelPreferences setCostPriority(Double costPriority) {
    this.costPriority = costPriority;
    return this;
  }

  /**
   * Gets how much to prioritize sampling speed (latency) when selecting a model. A
   * value of 0 means speed is not important, while a value of 1 means speed is
   * the most important factor.
   *
   * @return speed priority between 0.0 and 1.0
   */
  public Double getSpeedPriority() {
    return speedPriority;
  }

  /**
   * Sets how much to prioritize sampling speed (latency) when selecting a model.
   * Must be a value between 0.0 and 1.0.
   *
   * @param speedPriority speed priority between 0.0 and 1.0
   * @return this instance for method chaining
   */
  public ModelPreferences setSpeedPriority(Double speedPriority) {
    this.speedPriority = speedPriority;
    return this;
  }

  /**
   * Gets how much to prioritize intelligence and capabilities when selecting a
   * model. A value of 0 means intelligence is not important, while a value of 1
   * means intelligence is the most important factor.
   *
   * @return intelligence priority between 0.0 and 1.0
   */
  public Double getIntelligencePriority() {
    return intelligencePriority;
  }

  /**
   * Sets how much to prioritize intelligence and capabilities when selecting a model.
   * Must be a value between 0.0 and 1.0.
   *
   * @param intelligencePriority intelligence priority between 0.0 and 1.0
   * @return this instance for method chaining
   */
  public ModelPreferences setIntelligencePriority(Double intelligencePriority) {
    this.intelligencePriority = intelligencePriority;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ModelPreferencesConverter.toJson(this, json);
    return json;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
