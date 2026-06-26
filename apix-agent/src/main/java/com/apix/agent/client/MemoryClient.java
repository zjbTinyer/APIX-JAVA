package com.apix.agent.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Memory 服务 HTTP 客户端 — 对标 Python 中的 httpx 调用 MEMORY_SERVICE_BASE_URL。
 *
 * 封装与 Memory 服务的所有 HTTP 通信：
 * - 消息追加/查询
 * - 短期/长期记忆
 * - 用户与会话管理
 */
@Component
public class MemoryClient {

    private static final Logger log = LoggerFactory.getLogger(MemoryClient.class);

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Value("${apix.services.memory:http://localhost:5093}")
    private String baseUrl;

    /**
     * 追加一条消息到对话历史。
     * 对标 Python: AIContextManager.append_to_messages
     */
    public boolean appendMessage(String clientId, String historyId, Map<String, Object> message) {
        try {
            JSONObject body = new JSONObject();
            body.put("client_id", clientId);
            body.put("session_id", "");
            body.put("history_id", historyId);
            body.put("messages", message);

            String respJson = post("/memory/memory/append_message", body);
            JSONObject resp = JSON.parseObject(respJson);

            boolean success = resp.getBooleanValue("success");
            if (!success) {
                log.warn("[MemoryClient] appendMessage failed: {}", resp.getString("message"));
            }
            return success;

        } catch (Exception e) {
            log.error("[MemoryClient] appendMessage error", e);
            return false;
        }
    }

    /**
     * 批量追加消息。
     */
    public boolean batchAppendMessages(String clientId, String historyId, List<Map<String, Object>> messages) {
        for (Map<String, Object> msg : messages) {
            if (!appendMessage(clientId, historyId, msg)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取对话历史消息。
     * 对标 Python: AIContextManager.get_messages
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getMessages(String historyId) {
        try {
            JSONObject body = new JSONObject();
            body.put("history_id", historyId);

            String respJson = post("/memory/memory/get_messages", body);
            JSONObject resp = JSON.parseObject(respJson);

            if (!resp.getBooleanValue("success")) {
                return new ArrayList<>();
            }

            // messages 字段可能是 JSONArray
            Object msgObj = resp.get("messages");
            if (msgObj instanceof JSONArray) {
                return (List<Map<String, Object>>) (List<?>) ((JSONArray) msgObj).toJavaList(Map.class);
            }
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("[MemoryClient] getMessages error", e);
            return new ArrayList<>();
        }
    }

    /**
     * 插入短期记忆。
     */
    public boolean insertShorttermMemory(String clientId, String historyId, String memoryId, String content) {
        try {
            JSONObject body = new JSONObject();
            body.put("client_id", clientId);
            body.put("history_id", historyId);
            body.put("memory_id", memoryId);
            body.put("content", content);

            String respJson = post("/memory/memory/insert_shortterm_memory", body);
            return JSON.parseObject(respJson).getBooleanValue("success");

        } catch (Exception e) {
            log.error("[MemoryClient] insertShorttermMemory error", e);
            return false;
        }
    }

    /**
     * 获取短期记忆。
     */
    public String getShorttermMemory(String clientId, String historyId) {
        try {
            JSONObject body = new JSONObject();
            body.put("client_id", clientId);
            body.put("history_id", historyId);

            String respJson = post("/memory/memory/get_shortterm_memory", body);
            JSONObject resp = JSON.parseObject(respJson);

            if (resp.getBooleanValue("success")) {
                return resp.getString("messages");
            }
            return "";

        } catch (Exception e) {
            log.error("[MemoryClient] getShorttermMemory error", e);
            return "";
        }
    }

    /**
     * 获取长期记忆。
     */
    public String getLongtermMemory(String clientId) {
        try {
            JSONObject body = new JSONObject();
            body.put("client_id", clientId);

            String respJson = post("/memory/memory/get_longterm_memory", body);
            JSONObject resp = JSON.parseObject(respJson);

            if (resp.getBooleanValue("success")) {
                return resp.getString("messages");
            }
            return "";

        } catch (Exception e) {
            log.error("[MemoryClient] getLongtermMemory error", e);
            return "";
        }
    }

    /**
     * 获取最近会话列表。
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getConversations(String userUid) {
        try {
            JSONObject body = new JSONObject();
            body.put("user_uid", userUid);

            String respJson = post("/memory/conversation/get_list", body);
            JSONObject resp = JSON.parseObject(respJson);

            if (resp.getBooleanValue("success")) {
                Object msgObj = resp.get("messages");
                if (msgObj instanceof JSONArray) {
                    return (List<Map<String, Object>>) (List<?>) ((JSONArray) msgObj).toJavaList(Map.class);
                }
            }
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("[MemoryClient] getConversations error", e);
            return new ArrayList<>();
        }
    }

    /**
     * 创建新会话。
     */
    public Map<String, Object> createConversation(String userUid, String title) {
        try {
            JSONObject body = new JSONObject();
            body.put("user_uid", userUid);
            body.put("title", title != null ? title : "新的聊天...");

            String respJson = post("/memory/conversation/create", body);
            JSONObject resp = JSON.parseObject(respJson);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", resp.getBooleanValue("success"));
            result.put("data", resp.get("messages"));
            return result;

        } catch (Exception e) {
            log.error("[MemoryClient] createConversation error", e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    // ==================== HTTP 工具方法 ====================

    private String post(String path, JSONObject body) throws Exception {
        String url = baseUrl + path;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"),
                        body.toJSONString()))
                .build();

        try (Response resp = httpClient.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                throw new RuntimeException("Memory service error " + resp.code() + " at " + path);
            }
            return resp.body() != null ? resp.body().string() : "{}";
        }
    }
}
