package com.apix.agent.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * File 服务 HTTP 客户端 — 对标 Python 中的 httpx 调用 FILE_SERVICE_URL。
 *
 * 封装文件上传、下载、RAG 查询等操作。
 */
@Component
public class FileClient {

    private static final Logger log = LoggerFactory.getLogger(FileClient.class);

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Value("${apix.services.file:http://localhost:5094}")
    private String baseUrl;

    /**
     * 获取最近文件列表。
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRecentFiles(String clientId, int limit) {
        try {
            JSONObject body = new JSONObject();
            body.put("client_id", clientId);
            body.put("limit", limit);

            String respJson = post("/file/file/get_recent_files", body);
            JSONObject resp = JSON.parseObject(respJson);

            if (resp.getBooleanValue("success")) {
                Object msgObj = resp.get("messages");
                if (msgObj instanceof JSONArray) {
                    return (List<Map<String, Object>>) (List<?>) ((JSONArray) msgObj).toJavaList(Map.class);
                }
            }
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("[FileClient] getRecentFiles error", e);
            return new ArrayList<>();
        }
    }

    /**
     * 搜索知识库（RAG）。
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchKnowledgeBase(String clientId, String query, int topK) {
        try {
            JSONObject body = new JSONObject();
            body.put("client_id", clientId);
            body.put("query", query);
            body.put("top_k", topK);

            String respJson = post("/rag/retrieval/search", body);
            JSONObject resp = JSON.parseObject(respJson);

            if (resp.getBooleanValue("success")) {
                Object msgObj = resp.get("messages");
                if (msgObj instanceof JSONArray) {
                    return (List<Map<String, Object>>) (List<?>) ((JSONArray) msgObj).toJavaList(Map.class);
                }
            }
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("[FileClient] searchKnowledgeBase error", e);
            return new ArrayList<>();
        }
    }

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
                throw new RuntimeException("File service error " + resp.code() + " at " + path);
            }
            return resp.body() != null ? resp.body().string() : "{}";
        }
    }
}
