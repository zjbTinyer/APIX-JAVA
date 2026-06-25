package com.apix.agent.core.pipeline;

import com.apix.agent.core.AgentRuntime;
import com.apix.agent.core.GenerationManager;
import com.apix.common.model.*;
import com.apix.agent.core.graph.AgentGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 事件处理器 — 对标 Python: EventHandler。
 *
 * 接收前端 WebSocket 消息，解析 action 并派发处理。
 * 核心方法 chat_with_llm：在异步线程中执行 Agent 图，通过 AgentStreamWriter 推送事件。
 */
@Component
public class EventHandler {

    private static final Logger log = LoggerFactory.getLogger(EventHandler.class);

    @Autowired
    private AgentRuntime agentRuntime;

    @Autowired
    private GenerationManager generationManager;

    /**
     * 处理聊天请求 — 异步执行 Agent 图并流式推送事件。
     *
     * 前端 Payload 样例:
     * {
     *   "action": "chat_with_llm",
     *   "data": {
     *     "clientId": "xxx",
     *     "historyId": "conv_uid",
     *     "platform": "default",
     *     "messages": { "content": "你好" },
     *     "re_generate": false,
     *     "config": { ... }
     *   }
     * }
     */
    public void handleChatWithLlm(Map<String, Object> payload,
                                  AgentStreamWriter.WebSocketSender sender) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        if (data == null) {
            log.warn("[EventHandler] No data in payload");
            return;
        }

        String clientId = (String) data.get("clientId");
        String historyId = (String) data.get("historyId");
        String platform = (String) data.get("platform");
        boolean reGenerate = Boolean.TRUE.equals(data.get("re_generate"));

        @SuppressWarnings("unchecked")
        Map<String, Object> messages = (Map<String, Object>) data.get("messages");

        @SuppressWarnings("unchecked")
        Map<String, Object> configMap = (Map<String, Object>) data.get("config");

        log.info("[EventHandler] chat_with_llm: client={}, history={}, platform={}",
            clientId, historyId, platform);

        // 1. 创建生成 ID（自动中止旧生成）
        String generationId = generationManager.createGeneration(clientId, historyId);
        ApixEventEnvelopeTarget target = new ApixEventEnvelopeTarget(clientId, platform, historyId);

        // 2. 构建配置
        AgentConfig config = buildConfig(configMap, platform);

        // 3. 构建初始状态
        MainAgentState state = buildInitialState(clientId, historyId, platform,
            generationId, messages, reGenerate, config, target);

        // 4. 创建事件写入器
        AgentStreamWriter writer = new AgentStreamWriter(generationId, sender);

        // 5. 在异步线程中执行图
        agentRuntime.executeAgentAsync(state, writer, generationId);
    }

    /**
     * 中止生成。
     * action = "abort_generation"
     */
    public void handleAbortGeneration(Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        if (data == null) return;

        String historyId = (String) data.get("historyId");
        String clientId = (String) data.get("clientId");
        log.warn("[EventHandler] Abort generation: client={}, history={}", clientId, historyId);

        generationManager.abortByHistoryId(clientId, historyId);
    }

    /**
     * 解析阻塞事件。
     * action = "resolve_block"
     */
    public void handleResolveBlock(Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        if (data == null) return;

        String blockId = (String) data.get("blockId");
        String clientId = (String) data.get("clientId");
        String platform = (String) data.get("platform");
        String conversationId = (String) data.get("conversationId");

        ApixEventEnvelopeTarget target = new ApixEventEnvelopeTarget(clientId, platform, conversationId);
        AgentStreamWriter.resolveBlock(target, blockId, data.get("result"));
    }

    // ==================== 私有方法 ====================

    /**
     * 构建初始 Agent 状态。
     */
    private MainAgentState buildInitialState(String clientId, String historyId,
                                              String platform, String generationId,
                                              Map<String, Object> input, boolean reGenerate,
                                              AgentConfig config, ApixEventEnvelopeTarget target) {
        MainAgentState state = new MainAgentState();
        state.setAgentName("main_agent");
        state.setAgentRole("main_agent");
        state.setClientId(clientId);
        state.setHistoryId(historyId);
        state.setPlatform(platform);
        state.setGenerationId(generationId);
        state.setConfig(config);
        state.setTarget(target);
        state.setReGenerate(reGenerate);
        state.setInput(input);
        state.setLlmCalls(0);
        state.setLlmRetryCount(0);
        state.setContextCompressLevel(0);
        state.setTimestamp(System.currentTimeMillis() / 1000);

        // 初始化消息列表
        state.setMessages(new java.util.ArrayList<>());

        return state;
    }

    /**
     * 从前端 JSON 构建配置对象。
     */
    private AgentConfig buildConfig(Map<String, Object> configMap, String platform) {
        AgentConfig config = new AgentConfig();
        config.setPlatform(platform);

        if (configMap == null) return config;

        config.setModelsProvider(str(configMap.get("modelsProvider")));
        config.setModelName(str(configMap.get("modelName")));
        config.setApiKey(str(configMap.get("apiKey")));
        config.setWorkDir(str(configMap.get("workDir")));
        config.setEnableThink(bool(configMap.get("enableThink")));
        config.setPureChatOn(bool(configMap.get("pureChatOn")));
        config.setEnableFileOperation(bool(configMap.get("enableFileOperation")));
        config.setEnableWebSearch(bool(configMap.get("enableWebSearch")));
        config.setEnableKnowledgeRetrieval(bool(configMap.get("enableKnowledgeRetrieval")));
        config.setEnableCommandOperation(bool(configMap.get("enableCommandOperation")));
        config.setEnableAgentAssign(bool(configMap.get("enableAgentAssign")));
        config.setEnableShorttermMemory(bool(configMap.get("enableShorttermMemory")));
        config.setEnableLongtermMemory(bool(configMap.get("enableLongtermMemory")));

        Object temp = configMap.get("modelTemperature");
        if (temp instanceof Number) config.setModelTemperature(((Number) temp).doubleValue());

        return config;
    }

    private String str(Object o) { return o != null ? o.toString() : ""; }
    private boolean bool(Object o) { return o instanceof Boolean && (Boolean) o; }
}
