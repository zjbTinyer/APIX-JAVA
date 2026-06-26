package com.apix.agent.config;

import com.apix.agent.client.MemoryClient;
import com.apix.agent.core.graph.AgentGraph;
import com.apix.agent.core.graph.node.ContextPrepareNode;
import com.apix.agent.core.graph.node.LlmCallNode;
import com.apix.agent.core.graph.node.MessagesPersistNode;
import com.apix.agent.core.graph.node.ToolExecutionNode;
import com.apix.agent.core.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Agent 核心配置 — 注入图节点所需的依赖。
 */
@Configuration
public class AgentConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AgentConfigurer.class);

    @Autowired(required = false)
    private MemoryClient memoryClient;

    @Autowired(required = false)
    private ToolRegistry toolRegistry;

    @PostConstruct
    public void init() {
        // 注入依赖到图节点
        if (memoryClient != null) {
            ContextPrepareNode.setMemoryClient(memoryClient);
            MessagesPersistNode.setMemoryClient(memoryClient);
            log.info("[AgentConfigurer] Injected MemoryClient into graph nodes");
        }

        if (toolRegistry != null) {
            LlmCallNode.setToolRegistry(toolRegistry);
            ToolExecutionNode.setToolRegistry(toolRegistry);
            log.info("[AgentConfigurer] Injected ToolRegistry into LlmCallNode and ToolExecutionNode");
        }
    }

    /**
     * Agent 图实例（单例，但每次 execute 需要新状态）。
     */
    @Bean
    public AgentGraph agentGraph() {
        AgentGraph graph = new AgentGraph();
        log.info("[AgentConfigurer] Created AgentGraph bean");
        return graph;
    }
}
