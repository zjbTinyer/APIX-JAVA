package com.apix.agent.core.graph.node;

import com.apix.agent.client.MemoryClient;
import com.apix.common.model.AgentConfig;
import com.apix.common.model.MainAgentState;
import com.apix.agent.core.graph.AgentGraph;
import com.apix.agent.core.graph.AgentNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 消息持久化节点 — 对标 Python: AgentNodeBase.messages_persist。
 *
 * 将本轮对话消息持久化到 Memory 服务（MySQL）。
 */
public class MessagesPersistNode implements AgentNode {

    private static final Logger log = LoggerFactory.getLogger(MessagesPersistNode.class);

    private static MemoryClient memoryClient;

    public static void setMemoryClient(MemoryClient client) {
        memoryClient = client;
    }

    @Override
    public String getName() {
        return AgentGraph.NODE_MESSAGES_PERSIST;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(MainAgentState state) {
        log.debug("[MessagesPersistNode] Persisting messages for generation={}", state.getGenerationId());

        if (memoryClient == null) {
            log.warn("[MessagesPersistNode] MemoryClient not available, skipping persistence");
            return AgentGraph.END;
        }

        String clientId = state.getClientId();
        String historyId = state.getHistoryId();
        String generationId = state.getGenerationId();
        List<Map<String, Object>> messages = state.getMessages();
        AgentConfig config = state.getConfig();

        try {
            // 1. 识别本轮新生成的消息（从最后一条 AI 消息开始）
            boolean foundLastAi = false;
            for (int i = messages.size() - 1; i >= 0; i--) {
                Map<String, Object> msg = messages.get(i);
                String role = (String) msg.get("role");

                if ("assistant".equals(role)) {
                    // 跳过系统提示词
                    if (i == 0 && "system".equals(messages.get(0).get("role"))) continue;

                    // 构建持久化消息
                    Map<String, Object> persistMsg = new LinkedHashMap<>();
                    persistMsg.put("role", "ai");
                    persistMsg.put("content", msg.getOrDefault("content", ""));
                    persistMsg.put("generation_id", generationId);

                    // 思考链
                    Object reasoning = msg.get("reasoning_content");
                    if (reasoning != null) {
                        persistMsg.put("think", reasoning);
                    }

                    // 工具调用信息
                    Object toolCalls = msg.get("tool_calls");
                    if (toolCalls != null) {
                        persistMsg.put("extra", Map.of("tool_calls", toolCalls));
                    }

                    // 写入 Memory 服务
                    memoryClient.appendMessage(clientId, historyId, persistMsg);
                    foundLastAi = true;
                    break;
                }
            }

            if (!foundLastAi) {
                log.warn("[MessagesPersistNode] No AI message found to persist");
            }

            // 2. 用户输入也持久化
            Map<String, Object> input = state.getInput();
            if (input != null) {
                Map<String, Object> userMsg = new LinkedHashMap<>();
                userMsg.put("role", "human");
                userMsg.put("content", input.getOrDefault("content", ""));
                userMsg.put("generation_id", generationId);
                memoryClient.appendMessage(clientId, historyId, userMsg);
            }

            // 3. 更新短期记忆（如果开启）
            if (config != null && config.isEnableShorttermMemory()) {
                updateShorttermMemory(state);
            }

            log.info("[MessagesPersistNode] Persisted messages for generation={}", generationId);

        } catch (Exception e) {
            log.error("[MessagesPersistNode] Failed to persist messages", e);
        }

        return AgentGraph.END;
    }

    /**
     * 更新短期记忆。
     */
    private void updateShorttermMemory(MainAgentState state) {
        try {
            List<Map<String, Object>> messages = state.getMessages();

            // 提取最后几条消息作为短期记忆
            StringBuilder shortTerm = new StringBuilder();
            int count = 0;

            for (int i = messages.size() - 1; i >= 0 && count < 6; i--) {
                Map<String, Object> msg = messages.get(i);
                String role = (String) msg.get("role");
                if ("system".equals(role)) continue;

                String content = (String) msg.getOrDefault("content", "");
                if (!content.isEmpty()) {
                    shortTerm.insert(0, "[" + role + "]: " + content + "\n");
                    count++;
                }
            }

            if (shortTerm.length() > 0) {
                String memoryId = "mem_" + UUID.randomUUID().toString().substring(0, 12);
                memoryClient.insertShorttermMemory(
                    state.getClientId(),
                    state.getHistoryId(),
                    memoryId,
                    shortTerm.toString()
                );
            }

        } catch (Exception e) {
            log.warn("[MessagesPersistNode] Failed to update shortterm memory", e);
        }
    }
}
