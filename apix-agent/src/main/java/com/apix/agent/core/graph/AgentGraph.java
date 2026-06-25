package com.apix.agent.core.graph;

import com.apix.agent.core.graph.node.*;
import com.apix.common.constant.AgentEvent;
import com.apix.common.model.ApixEventEnvelopeTarget;
import com.apix.common.model.MainAgentState;
import com.apix.agent.core.pipeline.AgentStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 有向状态图引擎 — 对标 LangGraph 的 CompiledStateGraph。
 *
 * 节点执行顺序:
 *   START → context_prepare → context_summary → llm_call
 *                                                  │
 *                                         ┌────────┴────────┐
 *                                         ▼                 ▼
 *                                     tools (有 tool_calls)  messages_persist
 *                                         │                 │
 *                                         └──→ llm_call ←───┘
 *                                                            │
 *                                                            ▼
 *                                                           END
 *
 *  路由逻辑: retry→llm_call, summary→context_summary, ok→messages_persist
 */
public class AgentGraph {

    private static final Logger log = LoggerFactory.getLogger(AgentGraph.class);

    public static final String NODE_CONTEXT_PREPARE = "context_prepare";
    public static final String NODE_CONTEXT_SUMMARY = "context_summary";
    public static final String NODE_LLM_CALL = "llm_call";
    public static final String NODE_TOOLS = "tools";
    public static final String NODE_MESSAGES_PERSIST = "messages_persist";
    public static final String END = "__end__";

    private final Map<String, AgentNode> nodes = new LinkedHashMap<>();

    public AgentGraph() {
        registerNode(new ContextPrepareNode());
        registerNode(new ContextSummaryNode());
        registerNode(new LlmCallNode());
        registerNode(new ToolExecutionNode());
        registerNode(new MessagesPersistNode());
    }

    /**
     * 注册节点。
     */
    public void registerNode(AgentNode node) {
        nodes.put(node.getName(), node);
        log.info("[AgentGraph] Registered node: {}", node.getName());
    }

    /**
     * 同步执行图。
     */
    public MainAgentState execute(MainAgentState initialState) {
        return execute(initialState, null);
    }

    /**
     * 执行图，通过 writer 流式推送事件。
     *
     * @param initialState 初始状态
     * @param writer       可选事件写入器（推送事件到前端）
     * @return 最终状态
     */
    public MainAgentState execute(MainAgentState initialState, AgentStreamWriter writer) {
        MainAgentState state = initialState;

        // 把 writer 注入状态，让节点（尤其是 LlmCallNode）能通过它推流式事件
        state.setStreamWriter(writer);

        String currentNode = NODE_CONTEXT_PREPARE;

        log.info("[AgentGraph] Start execution, generation={}", state.getGenerationId());
        sendEvent(writer, state, AgentEvent.LLM_STREAM_START, null);

        int step = 0;
        while (!END.equals(currentNode)) {
            AgentNode node = nodes.get(currentNode);
            if (node == null) {
                log.error("[AgentGraph] Node not found: {}", currentNode);
                break;
            }

            log.debug("[AgentGraph] Step {}: node={}", ++step, currentNode);
            sendEvent(writer, state, AgentEvent.ESSENTIAL_INFO_RETURN,
                Map.of("node", currentNode, "step", step));

            String nextNode = node.execute(state);
            log.debug("[AgentGraph] Node {} → next: {}", currentNode, nextNode);
            currentNode = nextNode;
        }

        // 发送最终 AI 消息事件
        sendAiMessageReturn(writer, state);
        sendEvent(writer, state, AgentEvent.LLM_STREAM_END, null);

        log.info("[AgentGraph] Execution completed, total steps={}", step);
        return state;
    }

    /**
     * 发送 LLM_STREAM_START / LLM_CHUNK_RETURN / LLM_STREAM_END 等事件。
     */
    private void sendEvent(AgentStreamWriter writer, MainAgentState state,
                           String eventName, Map<String, Object> extra) {
        if (writer == null) return;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("event_name", eventName);
        if (extra != null) data.putAll(extra);

        ApixEventEnvelopeTarget target = state.getTarget();
        if (target == null) {
            target = new ApixEventEnvelopeTarget(
                state.getClientId(), state.getPlatform(), state.getHistoryId());
        }

        writer.sendEvent(eventName, target, data);
    }

    /**
     * 发送 AI_MESSAGE_RETURN 事件（最终 AI 回复）。
     */
    private void sendAiMessageReturn(AgentStreamWriter writer, MainAgentState state) {
        if (writer == null || state.getMessages() == null) return;

        // 找到最后一条 AI 消息
        Map<String, Object> lastAiMsg = null;
        for (int i = state.getMessages().size() - 1; i >= 0; i--) {
            Map<String, Object> msg = state.getMessages().get(i);
            if ("assistant".equals(msg.get("role"))) {
                lastAiMsg = msg;
                break;
            }
        }
        if (lastAiMsg == null) return;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("event_name", AgentEvent.AI_MESSAGE_RETURN);
        data.put("content", lastAiMsg.getOrDefault("content", ""));
        data.put("reasoning_content", lastAiMsg.get("reasoning_content"));

        ApixEventEnvelopeTarget target = state.getTarget();
        if (target == null) {
            target = new ApixEventEnvelopeTarget(
                state.getClientId(), state.getPlatform(), state.getHistoryId());
        }
        writer.sendEvent(AgentEvent.AI_MESSAGE_RETURN, target, data);
    }

    /**
     * 获取节点。
     */
    public AgentNode getNode(String name) {
        return nodes.get(name);
    }
}
