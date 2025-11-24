package io.vertx.mcp.server.feature;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.common.completion.Completion;
import io.vertx.mcp.common.completion.CompletionArgument;
import io.vertx.mcp.common.completion.CompletionContext;
import io.vertx.mcp.common.completion.CompletionReference;
import io.vertx.mcp.common.request.CompleteRequest;
import io.vertx.mcp.common.result.CompleteResult;
import io.vertx.mcp.common.rpc.JsonError;
import io.vertx.mcp.common.rpc.JsonRequest;
import io.vertx.mcp.common.rpc.JsonResponse;
import io.vertx.mcp.server.CompletionProvider;
import io.vertx.mcp.server.ModelContextProtocolServer;
import io.vertx.mcp.server.ServerFeature;
import io.vertx.mcp.server.ServerRequest;
import io.vertx.mcp.server.impl.ServerFeatureBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * The CompletionServerFeature class implements the ServerFeatureBase and provides functionality to handle completion/complete requests. It delegates completion requests to
 * appropriate completion providers based on the reference type.
 *
 * @version 2025-06-18
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/utilities/completion">Server Utilities - Completion</a>
 */
public class CompletionServerFeature extends ServerFeatureBase {

  private final ModelContextProtocolServer server;

  public CompletionServerFeature(ModelContextProtocolServer server) {
    this.server = server;
  }

  @Override
  public Map<String, BiFunction<ServerRequest, JsonRequest, Future<JsonResponse>>> getHandlers() {
    return Map.of("completion/complete", this::handleComplete);
  }

  private Future<JsonResponse> handleComplete(ServerRequest serverRequest, JsonRequest request) {
    JsonObject params = request.getNamedParams();
    if (params == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing parameters"))
      );
    }

    // Parse the request
    CompleteRequest completeRequest = new CompleteRequest(params);

    // Validate ref (required)
    CompletionReference ref = completeRequest.getRef();
    if (ref == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing 'ref' parameter"))
      );
    }

    String refType = ref.getType();
    if (refType == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing 'ref.type' parameter"))
      );
    }

    // Validate the reference identifier based on type
    String refIdentifier;
    if (ref.isPromptRef()) {
      refIdentifier = ref.getName();
      if (refIdentifier == null) {
        return Future.succeededFuture(
          JsonResponse.error(request, JsonError.invalidParams("Missing 'ref.name' parameter for ref/prompt"))
        );
      }
    } else if (ref.isResourceRef()) {
      refIdentifier = ref.getUri();
      if (refIdentifier == null) {
        return Future.succeededFuture(
          JsonResponse.error(request, JsonError.invalidParams("Missing 'ref.uri' parameter for ref/resource"))
        );
      }
    } else {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Unknown reference type: " + refType))
      );
    }

    // Validate argument (required)
    CompletionArgument argument = completeRequest.getArgument();
    if (argument == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing 'argument' parameter"))
      );
    }

    if (argument.getName() == null) {
      return Future.succeededFuture(
        JsonResponse.error(request, JsonError.invalidParams("Missing 'argument.name' parameter"))
      );
    }

    // Get context (optional, default to empty)
    CompletionContext context = completeRequest.getContext();
    if (context == null) {
      context = new CompletionContext();
    }

    // Find a completion provider that supports this reference type
    CompletionProvider provider = findCompletionProvider(refType);
    if (provider == null) {
      // Return empty completion if no provider found
      Completion emptyCompletion = new Completion()
        .setValues(new ArrayList<>())
        .setTotal(0)
        .setHasMore(false);
      CompleteResult result = new CompleteResult().setCompletion(emptyCompletion);
      return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
    }

    // Delegate to the provider
    return provider.handleCompletion(refType, refIdentifier, argument, context)
      .compose(completion -> {
        // Ensure completion values don't exceed 100 items
        if (completion.getValues() != null && completion.getValues().size() > 100) {
          List<String> truncated = completion.getValues().subList(0, 100);
          completion.setValues(truncated);
          completion.setHasMore(true);
        }
        CompleteResult result = new CompleteResult().setCompletion(completion);
        return Future.succeededFuture(JsonResponse.success(request, result.toJson()));
      })
      .recover(err -> Future.succeededFuture(
        JsonResponse.error(request, JsonError.internalError(err.getMessage()))
      ));
  }

  private CompletionProvider findCompletionProvider(String refType) {
    for (ServerFeature feature : server.features()) {
      if (feature instanceof CompletionProvider) {
        CompletionProvider provider = (CompletionProvider) feature;
        if (provider.getCompletionCapabilities().contains(refType)) {
          return provider;
        }
      }
    }
    return null;
  }
}
