package com.apix.agent.web;

import com.apix.agent.core.AgentRuntime;
import com.apix.agent.core.GenerationManager;
import com.apix.agent.core.graph.AgentGraph;
import com.apix.agent.core.pipeline.AgentStreamWriter;
import com.apix.common.constant.LlmProvider;
import com.apix.common.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Agent HTTP API — 对标 Python: routers/infomation.py
 */
@RestController
@RequestMapping("/api/v1")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    @Autowired
    private AgentRuntime agentRuntime;

    @Autowired
    private GenerationManager generationManager;

    /**
     * 健康检查。
     */
    @GetMapping("/health")
    public R<String> health() {
        return R.ok("agent-service is running");
    }

    /**
     * HTTP 聊天接口（同步，用于测试）。
     *
     * POST /api/v1/chat
     * Body: {
     *   "clientId": "test",
     *   "historyId": "conv1",
     *   "platform": "default",
     *   "message": "你好",
     *   "config": { "modelsProvider": "openai", "modelName": "gpt-4o", ... }
     * }
     */
    @PostMapping("/chat")
    public R<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        String clientId = (String) body.getOrDefault("clientId", "test-client");
        String historyId = (String) body.getOrDefault("historyId", UUID.randomUUID().toString());
        String platform = (String) body.getOrDefault("platform", "http");
        String message = (String) body.getOrDefault("message", "");

        @SuppressWarnings("unchecked")
        Map<String, Object> configMap = (Map<String, Object>) body.get("config");

        log.info("[API] chat: client={}, msg={}", clientId, message);

        // 构建配置
        AgentConfig config = buildSimpleConfig(configMap, platform);

        // 生成 ID
        String generationId = generationManager.createGeneration(clientId, historyId);
        ApixEventEnvelopeTarget target = new ApixEventEnvelopeTarget(clientId, platform, historyId);

        // 构建状态
        MainAgentState state = new MainAgentState();
        state.setAgentName("main_agent");
        state.setAgentRole("main_agent");
        state.setClientId(clientId);
        state.setHistoryId(historyId);
        state.setPlatform(platform);
        state.setGenerationId(generationId);
        state.setConfig(config);
        state.setTarget(target);
        state.setInput(Map.of("content", message));
        state.setMessages(new ArrayList<>());
        state.setTimestamp(System.currentTimeMillis() / 1000);

        // 同步执行图
        AgentGraph graph = agentRuntime.submitAgentTask("main_agent", "default", config);
        MainAgentState finalState = graph.execute(state, null);

        // 提取 AI 回复
        String aiContent = "";
        String reasoning = "";
        if (finalState.getMessages() != null) {
            for (int i = finalState.getMessages().size() - 1; i >= 0; i--) {
                Map<String, Object> msg = finalState.getMessages().get(i);
                if ("assistant".equals(msg.get("role"))) {
                    aiContent = (String) msg.getOrDefault("content", "");
                    Object r = msg.get("reasoning_content");
                    if (r != null) reasoning = r.toString();
                    break;
                }
            }
        }

        generationManager.finishGeneration(clientId, generationId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generation_id", generationId);
        result.put("content", aiContent);
        result.put("reasoning_content", reasoning);
        result.put("llm_calls", finalState.getLlmCalls());

        return R.ok(result);
    }

    private AgentConfig buildSimpleConfig(Map<String, Object> configMap, String platform) {
        AgentConfig config = new AgentConfig();
        config.setPlatform(platform);
        if (configMap == null) return config;

        config.setModelsProvider(str(configMap.get("modelsProvider")));
        config.setModelName(str(configMap.get("modelName")));
        config.setApiKey(str(configMap.get("apiKey")));
        config.setWorkDir(str(configMap.getOrDefault("workDir", "")));
        config.setEnableThink(bool(configMap.get("enableThink")));
        config.setPureChatOn(bool(configMap.get("pureChatOn")));

        // 默认开启能力
        config.setEnableFileOperation(bool(configMap.get("enableFileOperation")));
        config.setEnableWebSearch(bool(configMap.get("enableWebSearch")));
        config.setEnableAgentAssign(bool(configMap.get("enableAgentAssign")));

        return config;
    }

    private String str(Object o) { return o != null ? o.toString() : ""; }
    private boolean bool(Object o) { return o instanceof Boolean && (Boolean) o; }

    /**
     * 获取可用模型列表 — 对标 Python: get_models_list
     */
    @PostMapping("/get_models_list")
    public R<List<String>> getModelsList(@RequestBody Map<String, Object> body) {
        String provider = (String) body.getOrDefault("model_provider", "");
        String apiKey = (String) body.getOrDefault("api_key", "");

        log.info("[API] get_models_list: provider={}", provider);
        List<String> models = fetchModelList(provider, apiKey);
        return R.ok(models);
    }

    /**
     * 获取子 Agent 任务列表 — 对标 Python: get_sub_agent_task_list
     */
    @GetMapping("/get_sub_agent_task_list")
    public R<Map<String, Object>> getSubAgentTaskList() {
        // TODO: 从 AgentRuntime 查询所有任务
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task_list", Collections.emptyList());
        result.put("total", 0);
        return R.ok(result);
    }

    /**
     * 清除已完成任务 — 对标 Python: clear_finished_tasks
     */
    @GetMapping("/clear_finished_tasks")
    public R<String> clearFinishedTasks() {
        // TODO: 清理已完成的任务
        return R.ok("Cleared");
    }

    /**
     * 根据供应商获取模型列表。
     */
    private List<String> fetchModelList(String provider, String apiKey) {
        List<String> models = new ArrayList<>();

        switch (provider) {
            case "ollama:local":
            case "ollama":
                // TODO: 调用 Ollama API /api/tags
                models.add("llama3");
                models.add("qwen2.5");
                break;

            case "google":
                models.add("gemini-1.5-pro");
                models.add("gemini-1.5-flash");
                break;

            case "openai":
            case "deepseek":
            case "moonshot":
            case "xiaomimimo":
            case "qwen":
                // TODO: 调用 OpenAI 兼容的 /models 接口
                models.add("gpt-4o");
                models.add("gpt-3.5-turbo");
                break;

            case "custom":
                // TODO: 从 Memory 服务查询自定义供应商
                break;

            default:
                log.warn("[API] Unknown provider: {}", provider);
        }

        // 过滤 embedding 和 tts 模型
        models.removeIf(m -> m.toLowerCase().contains("embed")
                         || m.toLowerCase().contains("tts"));

        return models;
    }
}
