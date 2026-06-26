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
     * "clientId": "test",
     * "historyId": "conv1",
     * "platform": "default",
     * "message": "你好",
     * "config": { "modelsProvider": "openai", "modelName": "gpt-4o", ... }
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
                    if (r != null)
                        reasoning = r.toString();
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
        if (configMap == null)
            return config;

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

    private String str(Object o) {
        return o != null ? o.toString() : "";
    }

    private boolean bool(Object o) {
        return o instanceof Boolean && (Boolean) o;
    }

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
        return R.ok("Cleared");
    }

    /**
     * 根据供应商从真实 API 获取模型列表。
     */
    private List<String> fetchModelList(String provider, String apiKey) {
        List<String> models = new ArrayList<>();

        try {
            switch (provider) {
                case "ollama:local":
                case "ollama":
                    models.addAll(fetchOllamaModels());
                    break;

                case "google":
                    models.addAll(fetchOpenAiCompatibleModels(
                            "https://generativelanguage.googleapis.com/v1beta/models",
                            apiKey, ""));
                    break;

                case "openai":
                    models.addAll(fetchOpenAiCompatibleModels(
                            "https://api.openai.com/v1/models",
                            apiKey, "gpt"));
                    break;

                case "deepseek":
                    models.addAll(fetchOpenAiCompatibleModels(
                            "https://api.deepseek.com/v1/models",
                            apiKey, "deepseek"));
                    break;

                case "moonshot":
                    models.addAll(fetchOpenAiCompatibleModels(
                            "https://api.moonshot.cn/v1/models",
                            apiKey, "moonshot"));
                    break;

                case "xiaomimimo":
                    models.addAll(fetchOpenAiCompatibleModels(
                            "https://api.mija.ai/v1/models",
                            apiKey, "mij"));
                    break;

                case "qwen":
                    models.addAll(fetchOpenAiCompatibleModels(
                            "https://dashscope.aliyuncs.com/compatible-mode/v1/models",
                            apiKey, "qwen"));
                    break;

                case "custom":
                    // 自定义供应商无法自动获取模型列表
                    break;

                default:
                    log.warn("[API] Unknown provider: {}", provider);
            }
        } catch (Exception e) {
            log.warn("[API] fetchModelList failed for provider {}: {}", provider, e.getMessage());
        }

        // 过滤 embedding 和 tts 模型
        models.removeIf(m -> m.toLowerCase().contains("embed")
                || m.toLowerCase().contains("tts"));

        return models;
    }

    /**
     * 调用 Ollama API 获取本地模型列表。
     */
    private List<String> fetchOllamaModels() {
        List<String> models = new ArrayList<>();
        try {
            java.net.URL url = new java.net.URL("http://localhost:11434/api/tags");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int code = conn.getResponseCode();
            if (code == 200) {
                String resp = new String(conn.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                @SuppressWarnings("unchecked")
                Map<String, Object> map = com.alibaba.fastjson.JSON.parseObject(resp, Map.class);
                Object modelsObj = map.get("models");
                if (modelsObj instanceof java.util.List) {
                    for (Object m : (java.util.List<?>) modelsObj) {
                        if (m instanceof Map) {
                            String name = (String) ((Map<?, ?>) m).get("name");
                            if (name != null)
                                models.add(name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[API] Ollama not available: {}", e.getMessage());
        }
        if (models.isEmpty()) {
            // 回退到默认列表
            models.add("llama3");
            models.add("qwen2.5");
        }
        return models;
    }

    /**
     * 调用 OpenAI 兼容的 /models 接口获取模型列表。
     */
    private List<String> fetchOpenAiCompatibleModels(String apiUrl, String apiKey, String prefix) {
        List<String> models = new ArrayList<>();
        try {
            java.net.URL url = new java.net.URL(apiUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int code = conn.getResponseCode();
            if (code == 200) {
                String resp = new String(conn.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                @SuppressWarnings("unchecked")
                Map<String, Object> map = com.alibaba.fastjson.JSON.parseObject(resp, Map.class);
                Object dataObj = map.get("data");
                if (dataObj instanceof java.util.List) {
                    for (Object item : (java.util.List<?>) dataObj) {
                        if (item instanceof Map) {
                            String id = (String) ((Map<?, ?>) item).get("id");
                            if (id != null) {
                                // 按前缀过滤（如果有）
                                if (prefix.isEmpty() || id.toLowerCase().startsWith(prefix)) {
                                    models.add(id);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[API] Failed to fetch models from {}: {}", apiUrl, e.getMessage());
        }
        if (models.isEmpty()) {
            // 回退到通用默认模型
            models.add("gpt-4o");
            models.add("gpt-4o-mini");
        }
        return models;
    }
}
