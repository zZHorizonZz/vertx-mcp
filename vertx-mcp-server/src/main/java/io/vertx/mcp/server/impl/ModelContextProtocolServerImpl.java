package io.vertx.mcp.server.impl;

import io.vertx.mcp.common.prompt.Prompt;
import io.vertx.mcp.common.resources.Resource;
import io.vertx.mcp.common.resources.ResourceTemplate;
import io.vertx.mcp.common.root.Root;
import io.vertx.mcp.common.tool.Tool;
import io.vertx.mcp.server.*;

import java.util.HashMap;
import java.util.Map;

public class ModelContextProtocolServerImpl implements ModelContextProtocolServer {

  private final Map<Tool, ToolServerFeature> tools = new HashMap<>();
  private final Map<Resource, DynamicResourceHandler> resources = new HashMap<>();
  private final Map<ResourceTemplate, Void> resourceTemplates = new HashMap<>();
  private final Map<Prompt, PromptServerFeature> prompts = new HashMap<>();
  private final Map<Root, Void> roots = new HashMap<>();

  private ToolHandler toolHandler;
  private ResourceServerFeature resourceServerFeature;
  private PromptHandler promptHandler;
  private RootsHandler rootsHandler;

  @Override
  public void addTool(Tool tool, ToolServerFeature feature) {
    tools.put(tool, feature);
  }

  @Override
  public void addResource(Resource resource, DynamicResourceHandler feature) {
    resources.put(resource, feature);
  }

  @Override
  public void addResourceTemplate(ResourceTemplate template) {
    resourceTemplates.put(template, null);
  }

  @Override
  public void addPrompt(Prompt prompt, PromptServerFeature feature) {
    prompts.put(prompt, feature);
  }

  @Override
  public void addRoot(Root root) {
    roots.put(root, null);
  }

  @Override
  public void setToolHandler(ToolHandler handler) {
    this.toolHandler = handler;
  }

  @Override
  public void setResourceHandler(ResourceServerFeature handler) {
    this.resourceServerFeature = handler;
  }

  @Override
  public void setPromptHandler(PromptHandler handler) {
    this.promptHandler = handler;
  }

  @Override
  public void setRootsHandler(RootsHandler handler) {
    this.rootsHandler = handler;
  }
}
