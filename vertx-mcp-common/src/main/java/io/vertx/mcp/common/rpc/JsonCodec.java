package io.vertx.mcp.common.rpc;

import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.notification.*;
import io.vertx.mcp.common.request.*;
import io.vertx.mcp.common.result.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Codec for encoding and decoding MCP JSON-RPC messages. Registers all request, result, and notification types.
 */
public final class JsonCodec {

  private static final Map<String, Function<JsonObject, ? extends Request>> REQUEST_DECODERS = new HashMap<>();
  private static final Map<String, Function<JsonObject, ? extends Result>> RESULT_DECODERS = new HashMap<>();
  private static final Map<String, Function<JsonObject, ? extends Notification>> NOTIFICATION_DECODERS = new HashMap<>();

  static {
    registerRequest("initialize", InitializeRequest::new);
    registerRequest("ping", PingRequest::new);
    registerRequest("tools/list", ListToolsRequest::new);
    registerRequest("tools/call", CallToolRequest::new);
    registerRequest("prompts/list", ListPromptsRequest::new);
    registerRequest("prompts/get", GetPromptRequest::new);
    registerRequest("resources/list", ListResourcesRequest::new);
    registerRequest("resources/templates/list", ListResourceTemplatesRequest::new);
    registerRequest("resources/read", ReadResourceRequest::new);
    registerRequest("resources/subscribe", SubscribeRequest::new);
    registerRequest("resources/unsubscribe", UnsubscribeRequest::new);
    registerRequest("logging/setLevel", SetLevelRequest::new);
    registerRequest("completion/complete", CompleteRequest::new);

    // Server-to-client requests
    registerRequest("sampling/createMessage", CreateMessageRequest::new);
    registerRequest("roots/list", ListRootsRequest::new);
    registerRequest("elicitation/elicit", ElicitRequest::new);

    // Register results
    registerResult("initialize", InitializeResult::new);
    registerResult("ping", EmptyResult::new);
    registerResult("tools/list", ListToolsResult::new);
    registerResult("tools/call", CallToolResult::new);
    registerResult("prompts/list", ListPromptsResult::new);
    registerResult("prompts/get", GetPromptResult::new);
    registerResult("resources/list", ListResourcesResult::new);
    registerResult("resources/templates/list", ListResourceTemplatesResult::new);
    registerResult("resources/read", ReadResourceResult::new);
    registerResult("resources/subscribe", EmptyResult::new);
    registerResult("resources/unsubscribe", EmptyResult::new);
    registerResult("logging/setLevel", EmptyResult::new);
    registerResult("completion/complete", CompleteResult::new);

    // Server-to-client results
    registerResult("sampling/createMessage", CreateMessageResult::new);
    registerResult("roots/list", ListRootsResult::new);
    registerResult("elicitation/elicit", ElicitResult::new);

    // Register notifications
    registerNotification("notifications/initialized", InitializedNotification::new);
    registerNotification("notifications/cancelled", CancelledNotification::new);
    registerNotification("notifications/progress", ProgressNotification::new);
    registerNotification("notifications/message", LoggingMessageNotification::new);
    registerNotification("notifications/resources/list_changed", ResourceListChangedNotification::new);
    registerNotification("notifications/resources/updated", ResourceUpdatedNotification::new);
    registerNotification("notifications/tools/list_changed", ToolListChangedNotification::new);
    registerNotification("notifications/prompts/list_changed", PromptListChangedNotification::new);
    registerNotification("notifications/roots/list_changed", RootsListChangedNotification::new);
  }

  private JsonCodec() {
  }

  private static void registerRequest(String method, Function<JsonObject, ? extends Request> decoder) {
    REQUEST_DECODERS.put(method, decoder);
  }

  private static void registerResult(String method, Function<JsonObject, ? extends Result> decoder) {
    RESULT_DECODERS.put(method, decoder);
  }

  private static void registerNotification(String method, Function<JsonObject, ? extends Notification> decoder) {
    NOTIFICATION_DECODERS.put(method, decoder);
  }

  /**
   * Decode a request from JSON.
   *
   * @param method the method name
   * @param json the JSON params
   * @return the decoded request
   * @throws IllegalArgumentException if the method is not registered
   */
  public static Request decodeRequest(String method, JsonObject json) {
    Function<JsonObject, ? extends Request> decoder = REQUEST_DECODERS.get(method);
    if (decoder == null) {
      throw new IllegalArgumentException("Unknown request method: " + method);
    }
    return decoder.apply(json != null ? json : new JsonObject());
  }

  /**
   * Decode a result from JSON based on the request method.
   *
   * @param method the method name of the original request
   * @param json the JSON result
   * @return the decoded result
   * @throws IllegalArgumentException if the method is not registered
   */
  public static Result decodeResult(String method, JsonObject json) {
    Function<JsonObject, ? extends Result> decoder = RESULT_DECODERS.get(method);
    if (decoder == null) {
      throw new IllegalArgumentException("Unknown result method: " + method);
    }
    return decoder.apply(json != null ? json : new JsonObject());
  }

  /**
   * Decode a notification from JSON.
   *
   * @param method the method name
   * @param json the JSON params
   * @return the decoded notification
   * @throws IllegalArgumentException if the method is not registered
   */
  public static Notification decodeNotification(String method, JsonObject json) {
    Function<JsonObject, ? extends Notification> decoder = NOTIFICATION_DECODERS.get(method);
    if (decoder == null) {
      throw new IllegalArgumentException("Unknown notification method: " + method);
    }
    return decoder.apply(json != null ? json : new JsonObject());
  }

  /**
   * Check if a request method is registered.
   *
   * @param method the method name
   * @return true if registered
   */
  public static boolean hasRequest(String method) {
    return REQUEST_DECODERS.containsKey(method);
  }

  /**
   * Check if a result method is registered.
   *
   * @param method the method name
   * @return true if registered
   */
  public static boolean hasResult(String method) {
    return RESULT_DECODERS.containsKey(method);
  }

  /**
   * Check if a notification method is registered.
   *
   * @param method the method name
   * @return true if registered
   */
  public static boolean hasNotification(String method) {
    return NOTIFICATION_DECODERS.containsKey(method);
  }
}
