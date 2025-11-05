package io.vertx.mcp.server;

import io.vertx.mcp.common.prompt.Prompt;
import io.vertx.mcp.common.resources.Resource;
import io.vertx.mcp.common.resources.ResourceTemplate;
import io.vertx.mcp.common.root.Root;
import io.vertx.mcp.common.tool.Tool;

/**
 * Main interface for the Model Context Protocol server.
 * Supports registering individual items (tools, resources, prompts, roots) and handlers.
 */
public interface ModelContextProtocolServer {

  // Individual item registration
  void addTool(Tool tool, ToolServerFeature feature);

  void addResource(Resource resource, DynamicResourceHandler feature);

  void addResourceTemplate(ResourceTemplate template);

  void addPrompt(Prompt prompt, PromptServerFeature feature);

  void addRoot(Root root);

  // Handler registration
  void setToolHandler(ToolHandler handler);

  void setResourceHandler(ResourceServerFeature handler);

  void setPromptHandler(PromptHandler handler);

  void setRootsHandler(RootsHandler handler);
}
