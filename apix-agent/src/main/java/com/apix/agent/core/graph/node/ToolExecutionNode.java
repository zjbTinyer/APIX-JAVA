package com.apix.agent.core.graph.node;

import com.apix.agent.core.tools.ToolRegistry;
import com.apix.common.model.MainAgentState;
import com.apix.agent.core.graph.AgentGraph;
import com.apix.agent.core.graph.AgentNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具执行节点 — 对标 Python: ToolNode + ApixToolNode。
 *
 * 执行 LLM 发起的工具调用（tool_calls），
 * 将结果以 ToolMessage 形式放回消息列表。
 */
public class ToolExecutionNode implements AgentNode {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutionNode.class);

    private static ToolRegistry toolRegistry;

    public static void setToolRegistry(ToolRegistry registry) {
        toolRegistry = registry;
    }

    @Override
    public String getName() {
        return AgentGraph.NODE_TOOLS;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(MainAgentState state) {
        int callCount = state.getCurrentToolCalls() != null ? state.getCurrentToolCalls().size() : 0;
        log.debug("[ToolExecutionNode] Executing {} tool calls", callCount);

        if (toolRegistry == null) {
            log.warn("[ToolExecutionNode] ToolRegistry not available");
            state.setCurrentToolCalls(null);
            return AgentGraph.NODE_LLM_CALL;
        }

        List<Map<String, Object>> toolCalls = state.getCurrentToolCalls();
        if (toolCalls == null || toolCalls.isEmpty()) {
            return AgentGraph.NODE_LLM_CALL;
        }

        for (Map<String, Object> toolCall : toolCalls) {
            String toolName = (String) toolCall.get("name");
            Map<String, Object> args = (Map<String, Object>) toolCall.get("args");
            String callId = (String) toolCall.get("id");

            log.debug("[ToolExecutionNode] Calling tool: {} (id={})", toolName, callId);

            try {
                // 通过工具注册表执行
                Object result = toolRegistry.execute(toolName, args, state);

                // 构造 ToolMessage 放回消息列表
                Map<String, Object> toolMessage = new LinkedHashMap<>();
                toolMessage.put("role", "tool");
                toolMessage.put("content", String.valueOf(result));
                toolMessage.put("tool_call_id", callId);
                toolMessage.put("name", toolName);
                state.getMessages().add(toolMessage);

            } catch (Exception e) {
                log.error("[ToolExecutionNode] Tool {} failed: {}", toolName, e.getMessage());
                Map<String, Object> errorMessage = new LinkedHashMap<>();
                errorMessage.put("role", "tool");
                errorMessage.put("content", "Error: " + e.getMessage());
                errorMessage.put("tool_call_id", callId);
                errorMessage.put("name", toolName);
                errorMessage.put("status", "error");
                state.getMessages().add(errorMessage);
            }
        }

        // 清空当前工具调用，路由回 llm_call
        state.setCurrentToolCalls(null);
        return AgentGraph.NODE_LLM_CALL;
    }
}
