package com.apix.agent.core.llm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.apix.common.constant.LlmProvider;
import com.apix.common.exception.ProviderNotFoundException;
import com.apix.common.model.AgentConfig;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * LLM 适配器 — 对标 Python: LlmNodeAdapter + llm_factory + llm_creator
 *
 * 统一调用入口，根据 provider 路由到具体的 LLM 实现。
 * 支持 OpenAI 兼容接口、Ollama、DeepSeek 思考模式。
 */
public class LlmAdapter {

    private static final Logger log = LoggerFactory.getLogger(LlmAdapter.class);

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final String provider;
    private final String model;
    private final String apiKey;
    private final String baseUrl;
    private final boolean enableThink;
    private final double temperature;

    public LlmAdapter(String provider, String model, String apiKey, String baseUrl,
            boolean enableThink, double temperature) {
        this.provider = provider;
        this.model = model;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.enableThink = enableThink;
        this.temperature = temperature;
    }

    // ==================== 工厂方法 ====================

    /**
     * 根据配置创建适配器。
     */
    public static LlmAdapter create(AgentConfig config) {
        String provider = config.getModelsProvider();
        String model = config.getModelName();
        String apiKey = config.getApiKey();
        double temp = clamp(config.getModelTemperature(), 0, 2);

        if (LlmProvider.isCustomProvider(provider)) {
            String[] parts = provider.split("-", 3);
            if (parts.length < 3) {
                throw new ProviderNotFoundException("Invalid custom provider: " + provider);
            }
            String baseUrl = LlmProvider.getBaseUrl(provider);
            if (baseUrl == null) {
                throw new ProviderNotFoundException("Custom provider URL not registered: " + provider);
            }
            return new LlmAdapter(provider, model, apiKey, baseUrl, config.isEnableThink(), temp);
        }

        String baseUrl = LlmProvider.getBaseUrl(provider);
        if (baseUrl == null) {
            throw new ProviderNotFoundException("Unsupported provider: " + provider, provider);
        }
        return new LlmAdapter(provider, model, apiKey, baseUrl, config.isEnableThink(), temp);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    // ==================== 非流式调用 ====================

    /**
     * 调用 LLM（非流式）。
     *
     * @param messages 消息列表
     * @param config   Agent 配置
     * @param tools    可选的工具列表（用于 function calling）
     * @return 响应 Map: {content, role, tool_calls?, reasoning_content?}
     */
    public Map<String, Object> call(List<Map<String, Object>> messages, AgentConfig config,
            List<Map<String, Object>> tools) {
        switch (provider) {
            case "openai":
            case "deepseek":
            case "moonshot":
            case "xiaomimimo":
            case "qwen":
            case "qianfan":
                return callOpenAiChat(messages, false, config, tools);
            case "ollama:local":
            case "ollama":
                return callOllamaChat(messages);
            default:
                if (provider.startsWith("custom-")) {
                    return callOpenAiChat(messages, false, config, tools);
                }
                throw new ProviderNotFoundException("Unsupported: " + provider, provider);
        }
    }

    // ==================== 流式调用 ====================

    /**
     * 流式调用 LLM（SSE 流式返回）。
     *
     * @param messages 消息列表
     * @param config   Agent 配置
     * @param tools    可选的工具列表（用于 function calling）
     * @param callback 流式回调
     */
    public void streamCall(List<Map<String, Object>> messages, AgentConfig config,
            List<Map<String, Object>> tools,
            StreamCallback callback) {
        try {
            switch (provider) {
                case "openai":
                case "deepseek":
                case "moonshot":
                case "xiaomimimo":
                case "qwen":
                    streamOpenAiChat(messages, config, callback, tools);
                    break;
                case "ollama:local":
                case "ollama":
                    streamOllamaChat(messages, callback);
                    break;
                default:
                    if (provider.startsWith("custom-")) {
                        streamOpenAiChat(messages, config, callback, tools);
                    } else {
                        callback.onError(new ProviderNotFoundException("Unsupported: " + provider));
                    }
            }
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    // ==================== OpenAI 兼容 ====================

    /**
     * OpenAI 兼容非流式 chat completions。
     */
    private Map<String, Object> callOpenAiChat(List<Map<String, Object>> messages,
            boolean isSubCall, AgentConfig config,
            List<Map<String, Object>> tools) {
        JSONObject body = buildOpenAiBody(messages, tools);

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"),
                        body.toJSONString()))
                .build();

        try (Response resp = HTTP_CLIENT.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                String errBody = resp.body() != null ? resp.body().string() : "";
                throw new RuntimeException("LLM API error " + resp.code() + ": " + errBody);
            }

            String respBody = resp.body() != null ? resp.body().string() : "{}";
            JSONObject json = JSON.parseObject(respBody);

            return parseOpenAiResponse(json);

        } catch (Exception e) {
            throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
        }
    }

    /**
     * OpenAI 兼容流式 chat completions（SSE）。
     */
    private void streamOpenAiChat(List<Map<String, Object>> messages,
            AgentConfig config, StreamCallback callback,
            List<Map<String, Object>> tools) throws Exception {
        JSONObject body = buildOpenAiBody(messages, tools);
        body.put("stream", true);

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(RequestBody.create(MediaType.parse("application/json"),
                        body.toJSONString()))
                .build();

        StringBuilder contentBuffer = new StringBuilder();
        StringBuilder reasoningBuffer = new StringBuilder();
        String finishReason = null;

        try (Response resp = HTTP_CLIENT.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                String errBody = resp.body() != null ? resp.body().string() : "";
                callback.onError(new RuntimeException("Stream error " + resp.code() + ": " + errBody));
                return;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            resp.body() != null ? resp.body().byteStream()
                                    : new java.io.ByteArrayInputStream(new byte[0]),
                            StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith(":"))
                    continue; // 心跳
                if (!line.startsWith("data: "))
                    continue;

                String data = line.substring(6).trim();
                if ("[DONE]".equals(data))
                    break;

                try {
                    JSONObject chunk = JSON.parseObject(data);
                    List<JSONObject> choices = parseChoices(chunk);
                    if (choices.isEmpty())
                        continue;

                    JSONObject delta = choices.get(0).getJSONObject("delta");
                    if (delta == null)
                        continue;

                    String content = delta.getString("content");
                    String reasoning = delta.getString("reasoning_content");
                    finishReason = choices.get(0).getString("finish_reason");

                    if (content != null)
                        contentBuffer.append(content);
                    if (reasoning != null)
                        reasoningBuffer.append(reasoning);

                    Map<String, Object> chunkMap = new LinkedHashMap<>();
                    if (content != null)
                        chunkMap.put("content", content);
                    if (reasoning != null)
                        chunkMap.put("reasoning_content", reasoning);
                    if (!chunkMap.isEmpty())
                        callback.onChunk(chunkMap);

                } catch (Exception ignored) {
                    // 跳过解析失败的 chunk
                }
            }
        }

        // 组装最终响应
        Map<String, Object> finalResp = new LinkedHashMap<>();
        finalResp.put("content", contentBuffer.toString());
        finalResp.put("role", "assistant");
        if (reasoningBuffer.length() > 0) {
            finalResp.put("reasoning_content", reasoningBuffer.toString());
        }
        if ("tool_calls".equals(finishReason)) {
            // 工具调用需要额外处理，简化版暂只返回 thinking
        }

        callback.onDone(finalResp);
    }

    /**
     * 构建 OpenAI 兼容的请求体。
     *
     * @param messages 消息列表
     * @param tools    可选的工具列表（用于 function calling），传 null 表示不使用工具
     */
    private JSONObject buildOpenAiBody(List<Map<String, Object>> messages,
            List<Map<String, Object>> tools) {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("stream", false);

        // 转换消息格式
        JSONArray msgArray = new JSONArray();
        for (Map<String, Object> msg : messages) {
            JSONObject m = new JSONObject();
            m.put("role", msg.getOrDefault("role", "user"));
            m.put("content", msg.getOrDefault("content", ""));
            // 保留 reasoning_content（DeepSeek 思考链回传）
            Object reasoning = msg.get("reasoning_content");
            if (reasoning != null) {
                m.put("reasoning_content", reasoning);
            }
            msgArray.add(m);
        }
        body.put("messages", msgArray);

        // 工具列表（function calling）
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
        }

        // DeepSeek 思考模式
        if (enableThink) {
            JSONObject thinking = new JSONObject();
            thinking.put("type", "enabled");
            body.put("thinking", thinking);
        }

        return body;
    }

    /**
     * 解析 OpenAI 非流式响应。
     */
    private Map<String, Object> parseOpenAiResponse(JSONObject json) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", "assistant");

        List<JSONObject> choices = parseChoices(json);
        if (choices.isEmpty()) {
            result.put("content", "");
            return result;
        }

        JSONObject choice = choices.get(0);
        JSONObject message = choice.getJSONObject("message");
        if (message == null) {
            JSONObject delta = choice.getJSONObject("delta");
            if (delta != null)
                message = delta;
        }

        if (message == null) {
            result.put("content", "");
            return result;
        }

        // content
        result.put("content", message.getString("content") != null ? message.getString("content") : "");

        // reasoning_content（DeepSeek 思考链）
        String reasoning = message.getString("reasoning_content");
        if (reasoning != null) {
            result.put("reasoning_content", reasoning);
        }

        // tool_calls
        JSONArray toolCalls = message.getJSONArray("tool_calls");
        if (toolCalls != null && !toolCalls.isEmpty()) {
            List<Map<String, Object>> tcList = new ArrayList<>();
            for (int i = 0; i < toolCalls.size(); i++) {
                JSONObject tc = toolCalls.getJSONObject(i);
                Map<String, Object> tcMap = new LinkedHashMap<>();
                tcMap.put("id", tc.getString("id"));
                tcMap.put("type", tc.getString("type"));

                JSONObject func = tc.getJSONObject("function");
                if (func != null) {
                    Map<String, Object> funcMap = new LinkedHashMap<>();
                    funcMap.put("name", func.getString("name"));
                    funcMap.put("arguments", func.getString("arguments"));
                    tcMap.put("function", funcMap);
                }
                tcList.add(tcMap);
            }
            result.put("tool_calls", tcList);
        }

        // 用量信息
        JSONObject usage = json.getJSONObject("usage");
        if (usage != null) {
            Map<String, Object> usageMap = new LinkedHashMap<>();
            usageMap.put("prompt_tokens", usage.getIntValue("prompt_tokens"));
            usageMap.put("completion_tokens", usage.getIntValue("completion_tokens"));
            usageMap.put("total_tokens", usage.getIntValue("total_tokens"));
            result.put("usage", usageMap);
        }

        // finish_reason
        result.put("finish_reason", choice.getString("finish_reason"));

        return result;
    }

    /**
     * 从 JSON 中提取 choices 列表。
     */
    private List<JSONObject> parseChoices(JSONObject json) {
        List<JSONObject> result = new ArrayList<>();
        JSONArray choices = json.getJSONArray("choices");
        if (choices == null)
            return result;
        for (int i = 0; i < choices.size(); i++) {
            result.add(choices.getJSONObject(i));
        }
        return result;
    }

    // ==================== Ollama ====================

    private Map<String, Object> callOllamaChat(List<Map<String, Object>> messages) {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("stream", false);

        JSONArray msgArray = new JSONArray();
        for (Map<String, Object> msg : messages) {
            JSONObject m = new JSONObject();
            m.put("role", msg.getOrDefault("role", "user"));
            m.put("content", msg.getOrDefault("content", ""));
            msgArray.add(m);
        }
        body.put("messages", msgArray);

        Request request = new Request.Builder()
                .url(baseUrl + "/api/chat")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"),
                        body.toJSONString()))
                .build();

        try (Response resp = HTTP_CLIENT.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                throw new RuntimeException("Ollama error " + resp.code());
            }
            String respBody = resp.body() != null ? resp.body().string() : "{}";
            JSONObject json = JSON.parseObject(respBody);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("role", "assistant");
            result.put("content",
                    json.getString("message") != null ? json.getJSONObject("message").getString("content") : "");
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Ollama call failed: " + e.getMessage(), e);
        }
    }

    private void streamOllamaChat(List<Map<String, Object>> messages,
            StreamCallback callback) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("stream", true);

        JSONArray msgArray = new JSONArray();
        for (Map<String, Object> msg : messages) {
            JSONObject m = new JSONObject();
            m.put("role", msg.getOrDefault("role", "user"));
            m.put("content", msg.getOrDefault("content", ""));
            msgArray.add(m);
        }
        body.put("messages", msgArray);

        Request request = new Request.Builder()
                .url(baseUrl + "/api/chat")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"),
                        body.toJSONString()))
                .build();

        StringBuilder contentBuffer = new StringBuilder();

        try (Response resp = HTTP_CLIENT.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                callback.onError(new RuntimeException("Ollama stream error " + resp.code()));
                return;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resp.body() != null ? resp.body().byteStream()
                            : new java.io.ByteArrayInputStream(new byte[0]),
                            StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;

                try {
                    JSONObject chunk = JSON.parseObject(line);
                    String content = chunk.getString("content");
                    if (content != null) {
                        contentBuffer.append(content);
                        Map<String, Object> chunkMap = new LinkedHashMap<>();
                        chunkMap.put("content", content);
                        callback.onChunk(chunkMap);
                    }
                    if (Boolean.TRUE.equals(chunk.getBoolean("done")))
                        break;
                } catch (Exception ignored) {
                }
            }
        }

        Map<String, Object> finalResp = new LinkedHashMap<>();
        finalResp.put("content", contentBuffer.toString());
        finalResp.put("role", "assistant");
        callback.onDone(finalResp);
    }

    // ==================== 回调接口 ====================

    public interface StreamCallback {
        void onChunk(Map<String, Object> chunk);

        void onDone(Map<String, Object> finalResponse);

        void onError(Exception e);
    }

    // ==================== Accessors ====================

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }
}
